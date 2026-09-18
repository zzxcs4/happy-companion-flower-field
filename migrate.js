// 花田融入空间 —— 数据迁移脚本
// 输入：happy-companion-flower-field.sql（主文件，2026最新结构）
// 参考：happy-companion-flower-field-2025.sql（时间目标）
// 输出：happy-companion-flower-field-migrated.sql
//
// 规则：
// 1. 时间迁移到2025年：对每个表（评分表 user_field_score、新增表 category/file_comment/file_mention 除外），
//    按主键 id 匹配参考文件，将时间列替换为参考文件对应记录的时间；
//    主文件独有的记录（新增数据）保持原时间。
// 2. 花田数据迁移到对应空间（field.spaceId = space.id）：
//    fieldPicUrl→picUrl、description→description、maxNum→maxNum（团队/社区都适用）
//    status→status、password→password（仅社区空间 spaceType=2）
// 3. 非社区空间（spaceType != 2）状态默认 1。
// 4. 帖子迁移到对应空间：post.spaceId = field.spaceId（通过 post.fieldId 关联 field）。

const fs = require('fs');

const MAIN = 'happy-companion-flower-field.sql';
const REF = 'happy-companion-flower-field-2025.sql';
const OUT = 'happy-companion-flower-field-migrated.sql';

// ---------------- 解析工具 ----------------
function parseCreateTables(text) {
  const map = {};
  const re = /CREATE TABLE `([^`]+)`\s*\(([\s\S]*?)\)\s*ENGINE/g;
  let m;
  while ((m = re.exec(text)) !== null) {
    const cols = [];
    for (const line of m[2].split('\n')) {
      const lm = line.match(/^\s*`([^`]+)`\s+[a-z0-9_]+/i);
      if (lm) cols.push(lm[1]);
    }
    map[m[1]] = cols;
  }
  return map;
}

// 将 SQL 文本切成「语句」列表（' ; ' 在字符串外才切分，兼容 -- 行注释与 /* */ 块注释）
function splitStatements(text) {
  const statements = [];
  let cur = '';
  let inStr = false;
  let inLineComment = false;
  let inBlockComment = false;
  let i = 0;
  const n = text.length;
  while (i < n) {
    const ch = text[i];
    const next = text[i + 1];
    if (inBlockComment) {
      cur += ch;
      if (ch === '*' && next === '/') { cur += '/'; i += 2; inBlockComment = false; continue; }
      i++;
      continue;
    }
    if (inLineComment) {
      cur += ch;
      if (ch === '\n') inLineComment = false;
      i++;
      continue;
    }
    if (inStr) {
      cur += ch;
      if (ch === '\\' && i + 1 < n) { cur += next; i += 2; continue; }
      if (ch === "'") {
        if (next === "'") { cur += "'"; i += 2; continue; }
        inStr = false;
      }
      i++;
      continue;
    }
    // normal
    if (ch === "'") { inStr = true; cur += ch; i++; continue; }
    if (ch === '-' && next === '-') { inLineComment = true; cur += '--'; i += 2; continue; }
    if (ch === '/' && next === '*') { inBlockComment = true; cur += '/*'; i += 2; continue; }
    if (ch === ';') {
      statements.push(cur.trim());
      cur = '';
      i++;
      continue;
    }
    cur += ch;
    i++;
  }
  if (cur.trim()) statements.push(cur.trim());
  return statements;
}

// 解析 VALUES 元组
function parseTuples(body) {
  const tuples = [];
  let i = 0;
  const n = body.length;
  while (i < n) {
    while (i < n && body[i] !== '(') i++;
    if (i >= n) break;
    const vals = [];
    let j = i + 1;
    let cur = '';
    let depth = 0;
    let inStr = false;
    while (j < n) {
      const ch = body[j];
      if (inStr) {
        if (ch === '\\' && j + 1 < n) { cur += ch + body[j + 1]; j += 2; continue; }
        if (ch === "'") {
          if (body[j + 1] === "'") { cur += "''"; j += 2; continue; }
          inStr = false; cur += ch;
        } else cur += ch;
        j++;
        continue;
      }
      if (ch === "'") { inStr = true; cur += ch; j++; continue; }
      if (ch === '(') { depth++; cur += ch; j++; continue; }
      if (ch === ')') {
        if (depth === 0) { vals.push(cur.trim()); break; }
        depth--; cur += ch; j++; continue;
      }
      if (ch === ',' && depth === 0) { vals.push(cur.trim()); cur = ''; j++; continue; }
      cur += ch;
      j++;
    }
    tuples.push(vals);
    i = j + 1;
  }
  return tuples;
}

// ---------------- 读取并解析 ----------------
const mainText = fs.readFileSync(MAIN, 'utf8');
const refText = fs.readFileSync(REF, 'utf8');
const mainCols = parseCreateTables(mainText);
const refCols = parseCreateTables(refText);

// 参考文件：表 -> id -> {列名: token}
const refById = {};
for (const [tname, cols] of Object.entries(refCols)) {
  const map = {};
  for (const stmt of splitStatements(refText)) {
    const m = stmt.match(/INSERT INTO `([^`]+)`\s*VALUES\s*([\s\S]*)$/);
    if (m && m[1] === tname) {
      for (const tup of parseTuples(m[2])) {
        if (tup.length === 0) continue;
        const rec = {};
        cols.forEach((c, idx) => { rec[c] = tup[idx]; });
        map[tup[0]] = rec;
      }
    }
  }
  refById[tname] = map;
}

// 主文件：表 -> 记录列表（token 数组）
const mainRecords = {};
for (const [tname, cols] of Object.entries(mainCols)) {
  const list = [];
  for (const stmt of splitStatements(mainText)) {
    const m = stmt.match(/INSERT INTO `([^`]+)`\s*VALUES\s*([\s\S]*)$/);
    if (m && m[1] === tname) {
      for (const tup of parseTuples(m[2])) {
        if (tup.length > 0) list.push(tup);
      }
    }
  }
  mainRecords[tname] = list;
}

// ---------------- 迁移规则 ----------------
const SKIP_TIME_TABLES = new Set(['user_field_score', 'category', 'file_comment', 'file_mention']);

const TIME_COLS = {
  category: ['createTime', 'updateTime'],
  chat_list: ['createtime'],
  field: ['createTime', 'updateTime'],
  file: ['createTime', 'editTime', 'updateTime', 'reviewTime'],
  file_comment: ['createTime', 'updateTime'],
  file_mention: ['createTime', 'updateTime'],
  first_category: ['createTime', 'updateTime'],
  post: ['createTime', 'updateTime'],
  post_comment: ['createTime', 'updateTime'],
  second_category: ['createTime', 'updateTime'],
  space: ['createTime', 'editTime', 'updateTime'],
  space_user: ['createTime', 'updateTime'],
  tb_follow: ['create_time'],
  user: ['editTime', 'createTime', 'updateTime'],
  user_field: ['joinTime', 'createTime', 'updateTime'],
  user_field_score: ['create_time', 'update_time'],
  user_link: [],
  videos: ['createTime', 'editTime', 'updateTime', 'reviewTime'],
};

// 步骤1：时间迁移（按 id 匹配参考文件）
for (const [tname, cols] of Object.entries(mainCols)) {
  if (SKIP_TIME_TABLES.has(tname)) continue;
  const refMap = refById[tname];
  if (!refMap) continue; // 新增表 -> 时间不动
  const timeCols = TIME_COLS[tname] || [];
  if (timeCols.length === 0) continue;
  const colIdx = {};
  cols.forEach((c, i) => { colIdx[c] = i; });
  for (const rec of mainRecords[tname]) {
    const refRec = refMap[rec[0]];
    if (!refRec) continue; // 新增数据 -> 时间不动
    for (const tc of timeCols) {
      const mainIdx = colIdx[tc];
      if (mainIdx === undefined) continue;
      const refVal = refRec[tc];
      if (refVal !== undefined) rec[mainIdx] = refVal;
    }
  }
}

// 步骤2：field -> space 迁移
const fieldRecs = mainRecords['field'] || [];
const spaceRecs = mainRecords['space'] || [];
const spaceColIdx = {};
(mainCols['space'] || []).forEach((c, i) => { spaceColIdx[c] = i; });
const fieldColIdx = {};
(mainCols['field'] || []).forEach((c, i) => { fieldColIdx[c] = i; });

const fieldBySpaceId = {};
for (const f of fieldRecs) {
  const sid = f[fieldColIdx['spaceId']];
  if (sid && sid !== 'NULL' && !fieldBySpaceId[sid]) fieldBySpaceId[sid] = f;
}

for (const s of spaceRecs) {
  const sid = s[spaceColIdx['id']];
  const f = fieldBySpaceId[sid];
  const spaceType = s[spaceColIdx['spaceType']];
  if (f) {
    // 该空间是对应花田的空间（即社群空间）：迁移封面/描述/人数/状态/密码
    const picUrl = f[fieldColIdx['fieldPicUrl']];
    const desc = f[fieldColIdx['description']];
    const fmaxNum = f[fieldColIdx['maxNum']];
    const fstatus = f[fieldColIdx['status']];
    const fpassword = f[fieldColIdx['password']];
    if (picUrl && picUrl !== 'NULL') s[spaceColIdx['picUrl']] = picUrl;
    if (desc && desc !== 'NULL') s[spaceColIdx['description']] = desc;
    if (fmaxNum !== undefined && fmaxNum !== 'NULL') s[spaceColIdx['maxNum']] = fmaxNum;
    if (fstatus !== undefined) s[spaceColIdx['status']] = fstatus;
    if (fpassword !== undefined && fpassword !== 'NULL' && fpassword !== "''") s[spaceColIdx['password']] = fpassword;
    // 花田的时间相当于社群空间的时间：空间时间取自花田时间
    const fCreate = f[fieldColIdx['createTime']];
    const fUpdate = f[fieldColIdx['updateTime']];
    if (fCreate !== undefined) {
      s[spaceColIdx['createTime']] = fCreate;
      s[spaceColIdx['editTime']] = fCreate;
    }
    if (fUpdate !== undefined) s[spaceColIdx['updateTime']] = fUpdate;
  } else {
    // 非社区空间（无对应花田）：状态默认 1
    if (spaceType !== '2' && spaceType !== undefined) {
      s[spaceColIdx['status']] = '1';
    }
  }
}

// 步骤3：post 迁移（fieldId -> spaceId）
const postRecs = mainRecords['post'] || [];
const postColIdx = {};
(mainCols['post'] || []).forEach((c, i) => { postColIdx[c] = i; });
const fieldIdToSpaceId = {};
for (const f of fieldRecs) {
  fieldIdToSpaceId[f[fieldColIdx['id']]] = f[fieldColIdx['spaceId']];
}
for (const p of postRecs) {
  const fid = p[postColIdx['fieldId']];
  const sid = fieldIdToSpaceId[fid];
  if (sid !== undefined && sid !== 'NULL') {
    p[postColIdx['spaceId']] = sid;
  }
}

// 步骤4：user_field_score 评分表 —— 空间评分（时间不改，但列存的是花田id，映射为对应空间id）
// user_field_score 列：id, user_id, space_id, score, create_time, update_time
const scoreRecs = mainRecords['user_field_score'] || [];
const scoreColIdx = {};
(mainCols['user_field_score'] || []).forEach((c, i) => { scoreColIdx[c] = i; });
for (const r of scoreRecs) {
  const sidIdx = scoreColIdx['space_id'];
  if (sidIdx === undefined) continue;
  const cur = r[sidIdx];
  if (cur === undefined || cur === 'NULL') continue;
  // 若当前值是花田 id，则映射为对应空间 id
  const mapped = fieldIdToSpaceId[cur];
  if (mapped !== undefined && mapped !== 'NULL') {
    r[sidIdx] = mapped;
  }
}

// ---------------- 输出 ----------------
// 逐语句重建：INSERT 用迁移后的记录重建，其余语句原样保留（并补回被剥离的末尾分号）
const rebuilt = new Set();
const outStatements = [];
const pushStmt = (stmt) => {
  const trimmed = stmt.trimEnd();
  if (!trimmed) return;
  // 语句若不以分号结尾则补上（保证可执行）；纯注释块（头注释）除外
  if (!trimmed.endsWith(';') && !(trimmed.startsWith('/*') && trimmed.endsWith('*/'))) {
    outStatements.push(trimmed + ';');
  } else {
    outStatements.push(trimmed);
  }
};
for (const stmt of splitStatements(mainText)) {
  const m = stmt.match(/INSERT INTO `([^`]+)`\s*VALUES\s*([\s\S]*)$/);
  if (m) {
    const tname = m[1];
    if (!rebuilt.has(tname)) {
      rebuilt.add(tname);
      const recs = mainRecords[tname] || [];
      if (recs.length > 0) {
        outStatements.push(`-- ----------------------------`);
        outStatements.push(`-- Records of ${tname}`);
        outStatements.push(`-- ----------------------------`);
        for (const rec of recs) {
          outStatements.push(`INSERT INTO \`${tname}\` VALUES (${rec.join(', ')});`);
        }
      }
    }
    continue;
  }
  pushStmt(stmt);
}

let out = outStatements.join('\n') + '\n';
// 恢复文件头注释（splitStatements 已保留语句内注释，但行尾空行整理）
out = out.replace(/\n{3,}/g, '\n\n');
fs.writeFileSync(OUT, out, 'utf8');
console.log('迁移完成，输出文件:', OUT);

// ---------------- 校验输出 ----------------
const outText = fs.readFileSync(OUT, 'utf8');
const outCols = parseCreateTables(outText);
const osc = {};
outCols['space'].forEach((c, k) => { osc[c] = k; });
const outSpace = [];
for (const stmt of splitStatements(outText)) {
  const m = stmt.match(/INSERT INTO `space`\s*VALUES\s*([\s\S]*)$/);
  if (m) for (const tup of parseTuples(m[1])) outSpace.push(tup);
}
console.log('space 记录数:', outSpace.length);

// post 校验
const oPost = [];
for (const stmt of splitStatements(outText)) {
  const m = stmt.match(/INSERT INTO `post`\s*VALUES\s*([\s\S]*)$/);
  if (m) for (const tup of parseTuples(m[1])) oPost.push(tup);
}
const opc = {};
outCols['post'].forEach((c, k) => { opc[c] = k; });
console.log('post 记录数:', oPost.length, '| 空 spaceId 数:', oPost.filter(p => p[opc['spaceId']] === 'NULL' || p[opc['spaceId']] === undefined).length);

// 时间迁移抽查：chat_list id=1、field id=1981969199210491906、post id=1982029592830189569
function findRow(table, id) {
  for (const stmt of splitStatements(outText)) {
    const m = stmt.match(new RegExp(`INSERT INTO \\\`${table}\\\`\\s*VALUES\\s*([\\s\\S]*)$`));
    if (m) for (const tup of parseTuples(m[1])) if (tup[0] === id) return tup;
  }
  return null;
}
const checkTime = (table, id) => {
  const cols = outCols[table];
  const row = findRow(table, id);
  if (!row) return console.log(`  ${table} ${id} (not found)`);
  const tcols = TIME_COLS[table] || [];
  console.log(`  ${table} ${id}:`, tcols.map(tc => {
    const idx = cols.indexOf(tc);
    return `${tc}=${row[idx]}`;
  }).join(' '));
};
console.log('\n=== 时间迁移抽查 ===');
checkTime('chat_list', '1');
checkTime('field', '1981969199210491906');
checkTime('post', '1982029592830189569');
checkTime('space', '1981969199176937474');
checkTime('user_field_score', '12'); // 评分表应保持原时间（2026）
console.log('\n=== space 迁移结果（type/status/pwd/maxNum/pic/desc）===');
for (const s of outSpace) {
  const sid = s[osc['id']];
  const st = s[osc['spaceType']];
  const status = s[osc['status']];
  const pic = s[osc['picUrl']];
  const desc = s[osc['description']];
  const pwd = s[osc['password']];
  const mn = s[osc['maxNum']];
  console.log(`  space ${sid} type=${st} status=${status} pwd=${pwd} maxNum=${mn} pic=${pic ? String(pic).slice(0, 40) : pic} desc=${desc ? String(desc).slice(0, 16) : desc}`);
}
