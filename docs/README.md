# TestAgainAndAgain (T3A)

AI-native distributed online quiz platform with intelligent question generation, automated scoring, and knowledge gap analysis.

## 🏗️ Architecture

### Microservices

```
test-again-and-again/
├── quiz-gateway/           # API Gateway (Port: 8080)
│   ├── Routing & Load Balancing
│   ├── Authentication
│   └── Sentinel Rate Limiting
│
├── quiz-core/              # Core Service (Port: 8081)
│   ├── Question Bank Management
│   ├── Quiz Session Tracking
│   └── Randomization Logic
│
├── quiz-ai/                # AI Service (Port: 8082)
│   ├── Question Generation (Spring AI + RAG)
│   ├── Subjective Answer Scoring
│   └── Knowledge Graph Analysis
│
├── quiz-communication/     # Communication Service (Port: 8083)
│   ├── WebSocket (STOMP) Management
│   └── Real-time Progress Updates
│
└── quiz-common/            # Common Utilities
    ├── Domain Models
    ├── Constants
    └── Response Wrappers
```

## 🚀 Tech Stack

### Backend
- **Framework**: Spring Boot 3.2+ & Spring Cloud Alibaba 2023.0.x
- **AI**: Spring AI Alibaba (DeepSeek-V3 / Qwen-Max)
- **Runtime**: JDK 17 with Virtual Threads
- **Database**: MySQL 8.0+ (via MyBatis Plus & Druid)
- **Cache**: Redis 7.0+
- **Messaging**: RocketMQ 5.x
- **Service Discovery**: Nacos 2.3+
- **API Gateway**: Spring Cloud Gateway
- **Rate Limiting**: Sentinel
- **WebSocket**: STOMP over SockJS

### Frontend ✅
- **Runtime**: Node.js 18+ / Bun 1.1+
- **Framework**: React 18 + Vite 6
- **Styling**: Tailwind CSS 3.4
- **State Management**: TanStack React Query
- **Routing**: React Router 7
- **Animations**: Framer Motion
- **Icons**: Lucide React
- **HTTP Client**: Axios

## 📦 Quick Start

### Prerequisites
```bash
# Backend
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose (推荐)

# Frontend
- Node.js 18+ / Bun 1.1+
- npm / bun
```

### 🚀 一键启动（推荐）

```bash
# 1. 检查环境
./check-env.sh

# 2. 启动后端
./t3a-manager.sh start

# 3. 启动前端（新终端）
cd quiz-frontend
./start-frontend.sh
```

### 🌐 访问地址

**前端**:
- 应用首页: http://localhost:5173

**后端 API**:
- API Gateway: http://localhost:8080
- Core API 文档: http://localhost:8081/doc.html
- AI API 文档: http://localhost:8082/doc.html

**基础设施**:
- Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)
- RocketMQ 控制台: http://localhost:8180

📖 **详细启动指南**: [STARTUP-GUIDE.md](STARTUP-GUIDE.md:1)

## 🔧 Key Configuration

### Virtual Threads (JDK 17+)
All services are configured to use virtual threads for high concurrency:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### Nacos Namespace
All services use the `test-again-and-again` namespace for isolation.

### API Gateway Routes

| Service Path | Target Service | Description |
|-------------|---------------|-------------|
| `/api/quiz/**` | quiz-core | Question bank & quiz management |
| `/api/ai/**` | quiz-ai | AI generation & analysis |
| `/api/ws/**` | quiz-communication | WebSocket connections |

## 📚 API Documentation

Each service provides Swagger/Knife4j documentation:

- Quiz Core: http://localhost:8081/doc.html
- Quiz AI: http://localhost:8082/doc.html

## 🔄 Data Flow

1. **Question Generation**:
   - User uploads learning materials → quiz-ai
   - AI parses & generates questions (Spring AI + RAG)
   - RocketMQ async processing
   - Results saved to quiz-core

2. **Quiz Execution**:
   - User starts quiz → quiz-core
   - WebSocket connection → quiz-communication
   - Real-time progress updates via STOMP

3. **AI Scoring & Analysis**:
   - Submit answers → quiz-ai
   - Objective: Redis fast calculation
   - Subjective: AI semantic analysis
   - Generate knowledge graph → quiz-communication (WebSocket push)

## 🎯 Project Goals

- ✅ **Microservices Setup**: Complete basic architecture
- 🚧 **AI Integration**: Implement Spring AI for generation & scoring
- 🚧 **Real-time Communication**: WebSocket session management
- 🚧 **Async Processing**: RocketMQ message handling
- 🚧 **Frontend Development**: React 19 + Vite UI
- 🚧 **Performance Optimization**: Virtual threads + Redis caching
- 🚧 **Deployment**: Docker + Kubernetes

## 📝 Development Notes

### Code Structure
Each microservice follows a standard layered architecture:
```
src/main/java/com/t3a/{service}/
├── controller/     # REST Controllers
├── service/        # Business Logic
├── mapper/         # MyBatis Mappers (if needed)
├── domain/
│   ├── entity/     # Database Entities
│   ├── dto/        # Data Transfer Objects
│   └── vo/         # View Objects
├── config/         # Configuration Classes
└── {Service}Application.java
```

### Testing
```bash
# Unit tests
mvn test

# Integration tests (requires running services)
mvn verify
```

## 🤝 Contributing

Please refer to [CLAUDE.md](./CLAUDE.md) for detailed architecture documentation.

## 📄 License

MIT License

---

**Generated with Claude Code** 🤖
