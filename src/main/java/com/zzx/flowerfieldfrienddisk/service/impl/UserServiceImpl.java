package com.zzx.flowerfieldfrienddisk.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.common.ResultUtils;
import com.zzx.flowerfieldfrienddisk.exception.BusinessException;
import com.zzx.flowerfieldfrienddisk.exception.ErrorCode;
import com.zzx.flowerfieldfrienddisk.exception.ThrowUtils;
import com.zzx.flowerfieldfrienddisk.manager.auth.StpKit;
import com.zzx.flowerfieldfrienddisk.manager.upload.FilePictureUpload;
import com.zzx.flowerfieldfrienddisk.manager.upload.PictureUploadTemplate;
import com.zzx.flowerfieldfrienddisk.model.dto.file.UploadPictureResult;
import com.zzx.flowerfieldfrienddisk.model.dto.user.UserAvatarUploadRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.user.UserQueryRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.mapper.UserMapper;
import com.zzx.flowerfieldfrienddisk.model.enums.UserRoleEnum;
import com.zzx.flowerfieldfrienddisk.model.vo.LoginUserVO;
import com.zzx.flowerfieldfrienddisk.model.vo.UserVO;
import com.zzx.flowerfieldfrienddisk.service.UserFieldScoreService;
import com.zzx.flowerfieldfrienddisk.service.UserService;
import com.zzx.flowerfieldfrienddisk.utils.AlgorithmUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzx.flowerfieldfrienddisk.constant.RedisConstants.*;
import static com.zzx.flowerfieldfrienddisk.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author zzx
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-08-17 15:33:00
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private UserFieldScoreService userFieldScoreService;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "yupi";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
    @Override
    public BaseResponse<Boolean> sign(HttpServletRequest request, Long spaceId) {
        //1.获取当前登录用户
        Long userId = getLoginUser( request).getId();
        //2.获取日期
        LocalDateTime now = LocalDateTime.now();
        //3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY+spaceId+userId+keySuffix;
        //4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //5.写入Redis
        redisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);

        return ResultUtils.success(true);
    }

    @Override
    public BaseResponse<Integer> signCount(HttpServletRequest request,Long spaceId) {
        //1.获取本月截至今天为止所以的签到记录
        //1.获取当前登录用户
        Long userId = getLoginUser( request).getId();
        //2.获取日期
        LocalDateTime now = LocalDateTime.now();
        //3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY+spaceId+userId+keySuffix;
        //4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //5.获取本月截至今天为止所有的签到记录
        List<Long> result = redisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if(result==null||result.isEmpty()){
            return ResultUtils.success(0);
        }
        Long num=result.get(0);
        if(num==null||num==0){
            return ResultUtils.success(0);
        }
        int count=0;
        while (true){
            if((num & 1)==0){
                break;
            }else {
                count++;
            }
            num>>>=1;
        }
        return ResultUtils.success(count);
    }
    
    @Override
    public void autoSignOnVisit(Long userId, Long spaceId) {
        LocalDateTime now = LocalDateTime.now();
        String monthKey = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        int dayOfMonth = now.getDayOfMonth();
        
        // 1. 记录网站访问 bitmap
        String websiteKey = VISIT_WEBSITE_KEY + userId + monthKey;
        redisTemplate.opsForValue().setBit(websiteKey, dayOfMonth - 1, true);
        
        // 2. 记录社区访问 bitmap（同时作为签到记录）
        String fieldVisitKey = VISIT_FIELD_KEY + userId + ":" + spaceId + monthKey;
        redisTemplate.opsForValue().setBit(fieldVisitKey, dayOfMonth - 1, true);
        
        // 3. 记录传统签到 bitmap（保持兼容）
        String signKey = USER_SIGN_KEY + spaceId + userId + monthKey;
        redisTemplate.opsForValue().setBit(signKey, dayOfMonth - 1, true);
        
        // 4. 检查连续签到天数
        List<Long> result = redisTemplate.opsForValue().bitField(signKey, BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (result == null || result.isEmpty()) {
            return;
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return;
        }
        int consecutiveDays = 0;
        long temp = num;
        while (true) {
            if ((temp & 1) == 0) {
                break;
            } else {
                consecutiveDays++;
            }
            temp >>>= 1;
        }
        
        // 5. 连签 3 天奖励 +0.6（6 分封顶）
        if (consecutiveDays == 3) {
            String bonusKey = SIGN_STREAK_BONUS_KEY + userId + ":" + spaceId;
            Object bonusObj = redisTemplate.opsForValue().get(bonusKey);
            double totalBonus = bonusObj != null ? Double.parseDouble(bonusObj.toString()) : 0.0;
            if (totalBonus < 6.0) {
                double bonus = Math.min(0.6, 6.0 - totalBonus);
                userFieldScoreService.addScore(userId, spaceId, bonus);
                redisTemplate.opsForValue().set(bonusKey, totalBonus + bonus, 30, TimeUnit.DAYS);
            }
        }
        
        // 6. 重置连续未签周数（用户今天签到了）
        String inactiveWeeksKey = INACTIVE_WEEKS_KEY + userId + ":" + spaceId;
        redisTemplate.delete(inactiveWeeksKey);
    }
    
    @Override
    public boolean hasSignedToday(Long userId, Long spaceId) {
        LocalDateTime now = LocalDateTime.now();
        String monthKey = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String signKey = USER_SIGN_KEY + spaceId + userId + monthKey;
        int dayOfMonth = now.getDayOfMonth();
        Boolean signed = redisTemplate.opsForValue().getBit(signKey, dayOfMonth - 1);
        return signed != null && signed;
    }
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }


    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 根据标签搜索用户。(sql查询版)
     *
     * @param tagNameList 用户要搜索的标签
     * @return
     * @Deprecated 过时
     */
    @Override
    public List<UserVO> searchUsersByTags(List<String> tagNameList,String username) {
//        if (CollectionUtils.isEmpty(tagNameList)) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //拼接tag
        // like '%Java%' and like '%Python%'
        if(tagNameList!=null&& !tagNameList.isEmpty()){
            for (String tagList : tagNameList) {
                queryWrapper = queryWrapper.like("tags", tagList);
            }
        }

        queryWrapper.like(StrUtil.isNotBlank(username),"username", username);
        List<User> userList = this.list(queryWrapper);
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public List<UserVO> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNotNull("tags");
        queryWrapper.select("id","tags");
        List<User> userList = this.list(queryWrapper);

        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 用户列表的下表 => 相似度'
        List<Pair<User,Long>> list = new ArrayList<>();
        // 依次计算当前用户和所有用户的相似度
        for (int i = 0; i <userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTags();
            //无标签的 或当前用户为自己
            if (StringUtils.isBlank(userTags) || Objects.equals(user.getId(), loginUser.getId())){
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //计算分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            list.add(new ImmutablePair<>(user,distance));
        }
        //按编辑距离有小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //有顺序的userID列表
        List<Long> userListVo = topUserPairList.stream()
                .map(pair -> pair.getLeft().getId())  // 不是 getKey()
                .collect(Collectors.toList());

        //根据id查询user完整信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userListVo);
        Map<Long, List<UserVO>> userIdUserListMap = this.list(userQueryWrapper).stream()
                .map(this::getUserVO)
                .collect(Collectors.groupingBy(UserVO::getId));

        // 因为上面查询打乱了顺序，这里根据上面有序的userID列表赋值
        List<UserVO> finalUserList = new ArrayList<>();
        for (Long userId : userListVo){
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }

    @Override
    public String uploadUserAvatar(MultipartFile multipartFile, UserAvatarUploadRequest userAvatarUploadRequest, HttpServletRequest request) {
        if (multipartFile == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        User loginUser = this.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);



        // 用于判断是新增还是更新图片
        String userAvatar= loginUser.getUserAvatar();

        // 如果是更新图片，需要校验图片是否存在
        if (userAvatar != null) {
            String oldUserAvatar =userAvatar ;
            ThrowUtils.throwIf(oldUserAvatar == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");}
            // 仅本人或管理员可编辑
//            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }


        // 上传图片，得到信息




        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;

        uploadPathPrefix = String.format("userAvatar/%s", loginUser.getId());


        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate =  filePictureUpload;;

        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(multipartFile, uploadPathPrefix);
        // 构造要入库的图片信息

        loginUser.setUserAvatar(uploadPictureResult.getUrl());


        boolean result = this.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");


        return uploadPictureResult.getUrl();
    }

}




