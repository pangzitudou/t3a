# T3A 完整启动指南

## 📋 前置要求

### 必需软件

1. **Java 17+**
   ```bash
   java -version  # 确认版本 >= 17
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   ```

3. **Node.js 18+** (用于前端)
   ```bash
   node --version
   npm --version
   ```

4. **Docker** (推荐，用于基础设施)
   ```bash
   docker --version
   docker-compose --version
   ```

### 可选软件

- **Bun** (替代 npm，更快)
  ```bash
  curl -fsSL https://bun.sh/install | bash
  ```

---

## 🚀 一键启动（推荐）

### 方式1：使用自动化脚本

```bash
# 1. 检查并安装环境
./check-env.sh

# 2. 启动后端所有服务
./t3a-manager.sh start

# 3. 启动前端（新终端）
cd quiz-frontend
./start-frontend.sh
```

### 方式2：手动启动

#### 步骤1：启动基础设施

```bash
# 使用 Docker Compose 启动MySQL、Redis、Nacos、RocketMQ
docker-compose up -d

# 等待 Nacos 启动（大约30秒）
curl http://localhost:8848/nacos/
```

#### 步骤2：配置环境变量（可选）

```bash
# 如果使用 AI 功能，需要配置 API Key
export AI_DASHSCOPE_API_KEY="your-api-key-here"
```

#### 步骤3：启动后端服务

```bash
# 编译项目
mvn clean package -DskipTests

# 方式A：使用管理脚本
./t3a-manager.sh start

# 方式B：手动启动各服务（4个终端）
cd quiz-gateway && mvn spring-boot:run      # 终端1 (端口8080)
cd quiz-core && mvn spring-boot:run         # 终端2 (端口8081)
cd quiz-ai && mvn spring-boot:run           # 终端3 (端口8082)
cd quiz-communication && mvn spring-boot:run # 终端4 (端口8083)
```

#### 步骤4：启动前端

```bash
# 进入前端目录
cd quiz-frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev

# 或使用启动脚本
./start-frontend.sh
```

---

## 🌐 访问地址

### 前端
- **应用首页**: http://localhost:5173

### 后端 API
- **API Gateway**: http://localhost:8080
- **Core API 文档**: http://localhost:8081/doc.html
- **AI API 文档**: http://localhost:8082/doc.html

### 基础设施
- **Nacos 控制台**: http://localhost:8848/nacos
  - 用户名/密码: `nacos/nacos`
- **RocketMQ 控制台**: http://localhost:8180
- **Sentinel 控制台**: http://localhost:8858

---

## ✅ 验证服务状态

### 后端服务

```bash
# 使用管理脚本查看状态
./t3a-manager.sh status

# 或手动检查
curl http://localhost:8080  # Gateway
curl http://localhost:8081/doc.html  # Core
curl http://localhost:8082/doc.html  # AI
curl http://localhost:8083  # Communication
```

### 前端服务

```bash
curl http://localhost:5173
```

### 基础设施

```bash
# 查看 Docker 容器
docker ps

# 应该看到以下容器运行中：
# - t3a-mysql
# - t3a-redis
# - t3a-nacos
# - t3a-rocketmq-namesrv
# - t3a-rocketmq-broker
```

---

## 🧪 快速测试

### 1. 测试后端 API

```bash
# 注册用户
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456",
    "email": "test@example.com",
    "nickname": "测试用户"
  }'

# 登录获取Token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "123456"
  }'
```

### 2. 测试前端

1. 打开浏览器访问: http://localhost:5173
2. 点击"创建账号"注册新用户
3. 登录后进入仪表板

---

## 🔧 常见问题

### Q1: 端口被占用

**症状**:
```
Error: listen EADDRINUSE: address already in use :::8080
```

**解决方案**:
```bash
# 查看占用端口的进程
lsof -i :8080

# 停止服务
./t3a-manager.sh stop

# 或杀死进程
kill -9 <PID>
```

### Q2: Nacos 启动失败

**症状**:
```
docker logs t3a-nacos
Error: Unable to access jarfile
```

**解决方案**:
```bash
# 重启 Nacos
docker restart t3a-nacos

# 等待30秒后检查
curl http://localhost:8848/nacos/
```

### Q3: 前端无法连接后端

**症状**: 登录时提示"Network Error"

**解决方案**:
```bash
# 1. 确认后端运行
./t3a-manager.sh status

# 2. 检查前端配置
cat quiz-frontend/.env
# 确保 VITE_API_URL=http://localhost:8080

# 3. 重启前端
cd quiz-frontend
npm run dev
```

### Q4: MySQL 连接失败

**症状**:
```
Cannot connect to database server
```

**解决方案**:
```bash
# 1. 检查 MySQL 容器
docker ps | grep mysql

# 2. 查看 MySQL 日志
docker logs t3a-mysql

# 3. 手动连接测试
mysql -h 127.0.0.1 -u root -proot
```

### Q5: npm install 失败

**症状**:
```
npm WARN EBADENGINE Unsupported engine
```

**解决方案**:
```bash
# 更新 Node.js 到 v20+
nvm install 20
nvm use 20

# 或使用 --force
npm install --force
```

---

## 📊 服务依赖关系

```
前端 (Port 5173)
    ↓ HTTP
Gateway (Port 8080)
    ↓
    ├─→ Core (Port 8081) ────→ MySQL
    │                    └───→ Redis
    ├─→ AI (Port 8082) ──────→ Redis
    │                    └───→ Spring AI (Qwen/DeepSeek)
    └─→ Communication (Port 8083) → Redis

所有服务 → Nacos (服务注册/配置)
AI/Core → RocketMQ (异步消息)
```

---

## 🛑 停止服务

### 停止所有服务

```bash
# 使用管理脚本
./t3a-manager.sh stop

# 手动停止
# 1. Ctrl+C 停止前端和各个后端服务
# 2. 停止 Docker 容器
docker-compose down
```

### 只停止后端

```bash
./t3a-manager.sh stop
```

### 只停止前端

```bash
# 在前端终端按 Ctrl+C
```

---

## 📁 目录结构

```
test-again-and-again/
├── check-env.sh           # 环境检查脚本
├── t3a-manager.sh         # 后端管理脚本 ⭐
├── docker-compose.yml     # 基础设施编排
├── quiz-frontend/         # 前端项目 ⭐
│   ├── start-frontend.sh  # 前端启动脚本
│   ├── package.json
│   └── src/
├── quiz-gateway/          # API 网关
├── quiz-core/             # 核心服务
├── quiz-ai/               # AI 服务
└── quiz-communication/    # 通信服务
```

---

## 🎯 开发模式

### 热重载开发

**前端**:
- Vite 自动热更新，修改代码即时刷新
- 无需重启服务

**后端**:
```bash
# 使用 Spring Boot DevTools（已配置）
# 修改代码后自动重新编译
```

### 查看日志

```bash
# 后端日志
./t3a-manager.sh logs core    # 实时查看 Core 日志
./t3a-manager.sh logs ai      # 实时查看 AI 日志

# 或直接查看文件
tail -f logs/core.log

# 前端日志
# 直接在终端查看
```

---

## 🔐 安全提示

1. **不要在生产环境使用默认密码**
   - MySQL: `root/root`
   - Nacos: `nacos/nacos`

2. **保护 API Key**
   ```bash
   # 不要提交到 Git
   echo "AI_DASHSCOPE_API_KEY=xxx" >> .env.local
   ```

3. **CORS 配置**
   - 开发环境允许所有来源
   - 生产环境需限制域名

---

## 📚 相关文档

- [QUICK-START.md](QUICK-START.md:1) - 快速开始
- [BACKEND-IMPLEMENTATION.md](BACKEND-IMPLEMENTATION.md:1) - 后端实现
- [TEST-GUIDE.md](TEST-GUIDE.md:1) - 测试指南
- [ARCHITECTURE.md](ARCHITECTURE.md:1) - 架构设计

---

## 💡 提示

1. **首次启动较慢**
   - Maven 下载依赖
   - Docker 拉取镜像
   - npm 安装包
   - 预计 5-10 分钟

2. **后续启动很快**
   - 依赖已缓存
   - 镜像已下载
   - 约 30 秒完成启动

3. **推荐开发工具**
   - IDE: IntelliJ IDEA / VS Code
   - API 测试: Postman / Insomnia
   - 数据库: DBeaver / MySQL Workbench

---

**最后更新**: 2026-02-01
**版本**: 1.0.0

🎉 **现在您可以开始开发了！**
