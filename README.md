# 花田友盘

## 📖 项目简介

资源共享型兴趣社交平台采用 Spring Boot + Vue 3 前后端分离架构。平台提供多类型空间管理（私有/团队/社区空间）、文件上传处理、社区维护、社交互动、实时通讯及协同过滤推荐等功能。

---

## 🛠️ 技术栈

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **JDK** | 17 (Amazon Corretto) | Java 运行环境 |
| **Spring Boot** | 2.6.13 | 核心框架 |
| **MyBatis-Plus** | 3.5.9 | ORM 框架 |
| **Sa-Token** | 1.39.0 | 权限认证框架 |
| **MySQL** | 8.0+ | 关系型数据库 |
| **Redis** | 6.0+ | 缓存与会话存储 |
| **Redisson** | 3.19.1 | Redis 客户端 |
| **Knife4j** | 4.4.0 | API 文档工具 |
| **腾讯云 COS** | 5.6.255 | 对象存储服务 |
| **FFmpeg** | 8.1-full build-shared | 视频缩略图生成 |
| **Hutool** | 5.8.26 | Java 工具类库 |
| **Maven** | 3.9 | 构建工具 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Node.js** | 18+ | JavaScript 运行时 |
| **Vue** | 3.5.13 | 前端框架 |
| **Vite** | 6.0.1 | 构建工具 |
| **TypeScript** | 5.6.3 | 类型系统 |
| **Ant Design Vue** | 4.2.6 | UI 组件库 |
| **Vue Router** | 4.4.5 | 路由管理 |
| **Pinia** | 2.2.6 | 状态管理 |
| **Axios** | 1.11.0 | HTTP 客户端 |
| **ECharts** | 5.6.0 | 数据可视化 |
| **Day.js** | 1.11.13 | 日期处理 |

---

## 📋 环境要求

### 必需软件

- **JDK**: Amazon Corretto 17 或 OpenJDK 17
- **Maven**: 3.9+
- **Node.js**: 18+ (推荐 LTS 版本)
- **MySQL**:5.7
- **Redis**: 6.0+

### 可选工具

- Docker（容器化部署）
- Git（版本控制）
- IDE: IntelliJ IDEA / VS Code
- FFmpeg: 8.1-full build-shared（用于视频缩略图生成）

---

## 🚀 本地部署运行流程

### 第一步：下载项目

将源代码下载到本地。

### 第二步：数据库准备

1. **创建数据库**

```sql
CREATE DATABASE `happy-companion-flower-field` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **导入数据表结构**

执行项目根目录下的 SQL 文件：

```bash
mysql -u root -p happy-companion-flower-field < happy-companion-flower-field.sql
```

### 第三步：配置后端

1. **修改配置文件**

编辑 `src/main/resources/application-local.yml`，根据实际情况修改以下配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/happy-companion-flower-field
    username: root          # 修改为你的 MySQL 用户名
    password: 123456        # 修改为你的 MySQL 密码
  redis:
    host: localhost         # 修改为你的 Redis 地址
    port: 6379
    password: your_password # 修改为你的 Redis 密码（如无密码可留空）

cos:
  client:
    host: https://your-bucket.cos.region.myqcloud.com
    secretId: YOUR_SECRET_ID      # 腾讯云 COS SecretId
    secretKey: YOUR_SECRET_KEY    # 腾讯云 COS SecretKey
    region: ap-beijing            # COS 地域
    bucket: your-bucket-name      # COS Bucket 名称

aliYunAi:
  apiKey: YOUR_ALIYUN_API_KEY     # 阿里云 AI API Key（如使用）
```

2. **安装 FFmpeg**

**Windows:**

- 从 [FFmpeg 官网](https://ffmpeg.org/download.html) 下载
- 解压后将 `bin` 目录添加到系统环境变量 PATH

**macOS:**

```bash
brew install ffmpeg
```

**Linux:**

```bash
sudo apt-get install ffmpeg  # Ubuntu/Debian
sudo yum install ffmpeg      # CentOS/RHEL
```

验证安装：
```bash
ffmpeg -version
ffprobe -version
```

### 第四步：启动后端服务

```bash
# 进入项目根目录
cd happy-companion-flower-field

# 使用 Maven 编译并启动
mvn spring-boot:run

# 或者先打包再运行
mvn clean package -DskipTests
java -jar target/yun-file-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

后端服务将在 `http://localhost:8123/api` 启动

访问 API 文档：`http://localhost:8123/api/doc.html`

### 第五步：配置并启动前端

1. **进入前端目录**

```bash
cd src/yun-picture-frontend
```

2. **安装依赖**

```bash
npm install
```

3. **配置开发服务器代理**

检查 `vite.config.ts` 或相关配置文件，确保 API 请求能正确代理到后端：

```typescript
// 如需配置代理，可在 vite.config.ts 中添加
export default defineConfig({
  // ... 其他配置
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8123',
        changeOrigin: true,
      }
    }
  }
})
```

4. **启动前端开发服务器**

```bash
npm run dev
```

前端服务将在 `http://localhost:5173` 启动（端口可能因配置而异）

### 第六步：访问应用

打开浏览器访问：`http://localhost:5173`

---

## 🐳 Docker 部署（可选）

### 构建镜像

```bash
docker build -t happy-companion-flower-field .
```

### 运行容器

```bash
docker run -d \
  --name flower-field \
  -p 8123:8123 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=your-db-host \
  -e DB_PASSWORD=your-db-password \
  -e REDIS_HOST=your-redis-host \
  -e REDIS_PASSWORD=your-redis-password \
  happy-companion-flower-field
```

---

## 📁 项目结构

```
happy-companion-flower-field/
├── src/
│   ├── main/
│   │   ├── java/com/zzx/yunpicturebackend/
│   │   │   ├── annotation/        # 自定义注解
│   │   │   ├── aop/               # AOP 切面
│   │   │   ├── api/               # 第三方 API 集成
│   │   │   ├── common/            # 通用类
│   │   │   ├── config/            # 配置类
│   │   │   ├── constant/          # 常量定义
│   │   │   ├── controller/        # 控制器层
│   │   │   ├── exception/         # 异常处理
│   │   │   ├── manager/           # 业务管理器
│   │   │   ├── mapper/            # MyBatis Mapper
│   │   │   ├── model/             # 数据模型
│   │   │   ├── service/           # 服务层
│   │   │   └── utils/             # 工具类
│   │   └── resources/
│   │       ├── application.yml           # 主配置文件
│   │       ├── application-local.yml     # 本地环境配置
│   │       └── application-prod.yml      # 生产环境配置
│   └── yun-picture-frontend/      # 前端项目
│       ├── src/
│       │   ├── api/               # API 接口
│       │   ├── components/        # 组件
│       │   ├── pages/             # 页面
│       │   ├── stores/            # Pinia 状态管理
│       │   └── utils/             # 工具函数
│       └── package.json
├── pom.xml                        # Maven 配置
└── happy-companion-flower-field.sql  # 数据库脚本
```

---

## 🔧 常见问题

### 1. 端口被占用

如果 8123 或 5173 端口被占用，可以修改配置文件中的端口号：

**后端**: 修改 `application.yml` 中的 `server.port`
**前端**: 修改 `package.json` 中的启动脚本或 `vite.config.ts`

### 2. Redis 连接失败

确保 Redis 服务已启动，并检查 `application-local.yml` 中的 Redis 配置是否正确。

### 3. 文件上传失败

- 检查腾讯云 COS 配置是否正确
- 确认 Bucket 权限设置
- 验证文件大小是否超过限制（默认 100MB）

### 4. 视频缩略图生成失败

确保 FFmpeg 已正确安装并添加到系统 PATH 中。

---

## 📝 开发说明

### 后端开发

- **API 文档**: 访问 `http://localhost:8123/api/doc.html`
- **日志配置**: 开发环境已开启 MyBatis SQL 日志输出
- **Session 配置**: 使用 Redis 存储 Session，有效期 30 天

### 前端开发

- **代码规范**: 使用 ESLint + Prettier
- **类型检查**: `npm run type-check`
- **代码格式化**: `npm run format`

---

## 📞 联系方式

如有问题，请通过 2926068989@qq.com反馈。
