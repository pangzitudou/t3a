# API Architecture After Fixes

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser (Client)                            │
│  React 19 + Vite 6 running on http://localhost:7777                │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         │ API Calls
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Vite Dev Server (Proxy)                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ /api/*         → http://localhost:8081 (quiz-core)           │  │
│  │ /api/ai/*      → http://localhost:8082 (quiz-ai)             │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────┬──────────────────────────────┬─────────────────────┘
                  │                              │
                  │                              │
    ┌─────────────▼─────────────┐  ┌────────────▼──────────────┐
    │      quiz-core (8081)     │  │     quiz-ai (8082)        │
    │   ┌───────────────────┐   │  │  ┌────────────────────┐   │
    │   │ /auth/login       │   │  │  │ /generation/generate│   │
    │   │ /auth/register    │   │  │  │ /generation/status  │   │
    │   │ /bank/list        │   │  │  │ /ai/analysis        │   │
    │   │ /bank/create      │   │  │  │ /chat               │   │
    │   │ /bank/{id}        │   │  │  └────────────────────┘   │
    │   │ /start            │   │  │                           │
    │   │ /submit           │   │  │  DeepSeek/Qwen LLM        │
    │   │ /session/{key}    │   │  │  (AI Processing)          │
    │   │ /result/{key}     │   │  │                           │
    │   │ /history/{uid}    │   │  │                           │
    │   │ /dashboard        │   │  │                           │
    │   │ /question/*       │   │  │                           │
    │   └───────────────────┘   │  └───────────────────────────┘
    │                           │
    │  MySQL 8.0+               │
    │  Redis 7.0+               │
    └───────────────────────────┘
```

## Request Flow Examples

### Example 1: Fetch Dashboard Data
```
1. Browser: quizApi.getDashboard()
2. Frontend makes: GET /api/quiz/dashboard
3. Vite Proxy rewrites: /quiz/dashboard
4. Forwards to: http://localhost:8081/quiz/dashboard
5. quiz-core DashboardController.getDashboard()
6. Returns: { code: 200, data: { quizzesCompleted: 0, ... } }
```

### Example 2: Create Question Bank
```
1. Browser: quizApi.createBank({ name: 'React Quiz' })
2. Frontend makes: POST /api/quiz/bank/create
3. Vite Proxy rewrites: /quiz/bank/create
4. Forwards to: http://localhost:8081/quiz/bank/create
5. quiz-core QuestionBankController.createBank()
6. Returns: { code: 200, data: { id: 1, name: 'React Quiz', ... } }
```

### Example 3: Generate Questions with AI
```
1. Browser: aiApi.generateQuestions({ file, ... })
2. Frontend makes: POST /api/ai/generation/generate
3. Vite Proxy rewrites: /generation/generate
4. Forwards to: http://localhost:8082/generation/generate
5. quiz-ai AIGenerationController.generateQuestions()
6. Processes file with LLM → Returns taskId
7. Browser: aiApi.getGenerationProgress(taskId)
8. Frontend makes: GET /api/ai/generation/status/{taskId}
9. Returns: "completed" or "processing"
```

## API Endpoint Matrix

### Authentication (quiz-core:8081)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/quiz/auth/login` | User login | No |
| POST | `/quiz/auth/register` | User registration | No |

### Question Banks (quiz-core:8081)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/quiz/bank/list` | List all banks | Yes |
| POST | `/quiz/bank/create` | Create new bank | Yes |
| GET | `/quiz/bank/{id}` | Get bank details | Yes |
| DELETE | `/quiz/bank/{id}` | Delete bank | Yes |
| PUT | `/quiz/bank/update` | Update bank | Yes |

### Quiz Sessions (quiz-core:8081)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/quiz/start` | Start quiz session | Yes |
| GET | `/quiz/session/{key}` | Get session details | Yes |
| POST | `/quiz/submit` | Submit quiz answers | Yes |
| GET | `/quiz/result/{key}` | Get quiz result | Yes |
| GET | `/quiz/history/{uid}` | Get user history | Yes |

### Questions (quiz-core:8081)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/quiz/question/list` | List questions by bank | Yes |
| GET | `/quiz/question/random` | Get random questions | Yes |
| POST | `/quiz/question/create` | Create question | Yes |
| POST | `/quiz/question/batch` | Batch create questions | Yes |

### Dashboard (quiz-core:8081)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/quiz/dashboard` | Get dashboard stats | Yes |

### AI Generation (quiz-ai:8082)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/generation/generate` | Generate questions | Yes |
| GET | `/generation/status/{id}` | Check generation status | Yes |
| GET | `/generation/chat` | Test LLM chat | Yes |

### AI Analysis (quiz-ai:8082)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/ai/analysis/{key}` | Get knowledge analysis | Yes |
| POST | `/ai/analysis/{key}/regenerate` | Regenerate analysis | Yes |

## Data Flow Diagrams

### Login Flow
```
Browser                    Frontend                 Vite Proxy              Backend
   │                          │                         │                      │
   ├─── enter credentials ───▶│                         │                      │
   │                          │                         │                      │
   │                          ├─── POST /api/auth/login─▶│                      │
   │                          │                         │                      │
   │                          │                         ├─── POST /quiz/auth/login
   │                          │                         │                      │
   │                          │                         │                      ├─── Validate
   │                          │                         │                      │      credentials
   │                          │                         │                      │
   │                          │                         │◀─── Return JWT ──────┤
   │                          │                         │                      │
   │                          │◀─── Return token ───────┤                      │
   │                          │                         │                      │
   │◀─── Store token ─────────┤                         │                      │
   │                          │                         │                      │
   │◀─── Redirect to home ────│                         │                      │
```

### Quiz Taking Flow
```
Browser                    Frontend                 Vite Proxy              Backend
   │                          │                         │                      │
   ├─── Select bank ─────────▶│                         │                      │
   │                          │                         │                      │
   │                          ├─── GET /api/bank/list ─▶│                      │
   │                          │                         │                      │
   │                          │◀─── Return banks ──────┤                      │
   │                          │                         │                      │
   │◀─── Display banks ───────┤                         │                      │
   │                          │                         │                      │
   │                          │                         │                      │
   ├─── Click "Start Quiz" ──▶│                         │                      │
   │                          │                         │                      │
   │                          ├─── POST /api/start ────▶│                      │
   │                          │    {bankId, userId}     │                      │
   │                          │                         │                      │
   │                          │                         ├─── POST /quiz/start  │
   │                          │                         │                      │
   │                          │                         │                      ├─── Create session
   │                          │                         │                      │      in Redis
   │                          │                         │                      │
   │                          │                         │◀─── Return sessionKey │
   │                          │                         │                      │
   │                          │◀─── Return session ─────┤                      │
   │                          │                         │                      │
   │◀─── Show quiz UI ─────────┤                         │                      │
   │                          │                         │                      │
   │                          │                         │                      │
   ├─── Submit answers ──────▶│                         │                      │
   │                          │                         │                      │
   │                          ├─── POST /api/submit ───▶│                      │
   │                          │    {sessionKey, score}  │                      │
   │                          │                         │                      │
   │                          │                         ├─── POST /quiz/submit │
   │                          │                         │                      │
   │                          │                         │                      ├─── Calculate score
   │                          │                         │                      │      Save to MySQL
   │                          │                         │                      │
   │                          │                         │◀─── Return result ───┤
   │                          │                         │                      │
   │                          │◀─── Return result ──────┤                      │
   │                          │                         │                      │
   │◀─── Show results ─────────┤                         │                      │
```

### AI Question Generation Flow
```
Browser                    Frontend                 Vite Proxy              Backend
   │                          │                         │                      │
   ├─── Upload PDF ──────────▶│                         │                      │
   │                          │                         │                      │
   │                          ├─── POST /api/ai/generation/generate ────────▶│
   │                          │    FormData{file, request}                   │
   │                          │                         │                      │
   │                          │                         │    ┌─────────────────┤
   │                          │                         │    │                 │
   │                          │                         │    │  quiz-ai        │
   │                          │                         │    │  Process file   │
   │                          │                         │    │  Extract text   │
   │                          │                         │    │  Call LLM       │
   │                          │                         │    │  Generate Qs    │
   │                          │                         │    │                 │
   │                          │                         │    └─────────────────┤
   │                          │                         │                      │
   │                          │◀─── Return {taskId} ─────┤                      │
   │                          │                         │                      │
   │◀─── Show progress ────────┤                         │                      │
   │                          │                         │                      │
   │                          │                         │                      │
   ├─── Poll progress ────────▶│                         │                      │
   │                          │                         │                      │
   │                          ├─── GET /api/ai/generation/status/{id} ────────▶│
   │                          │                         │                      │
   │                          │                         │                      ├─── Check status
   │                          │                         │                      │      in Redis/DB
   │                          │                         │                      │
   │                          │◀─── Return status ──────┤                      │
   │                          │                         │                      │
   │◀─── Update progress bar ─┤                         │                      │
   │                          │                         │                      │
   │                          │                         │                      │
   │◀─── (Repeat until completed) │                      │                      │
```

## Error Handling

### Frontend Error Handling (api.ts interceptors)
```
Request Interceptor:
  - Add Authorization header for non-public endpoints
  - Log all requests for debugging

Response Interceptor:
  - Log all responses
  - Handle 401 → Redirect to /login
  - Pass errors to component for display
```

### Backend Error Handling
```
@ControllerAdvice
  - Handle validation errors
  - Handle business logic errors
  - Return consistent Result<T> format

Result<T> Format:
  {
    code: 200,           // HTTP status code
    message: "success",  // User-friendly message
    data: {...}          // Response payload or null
  }
```

## Security Considerations

### Authentication
- JWT tokens stored in localStorage
- Tokens sent in Authorization header
- Public endpoints: login, register
- Protected endpoints: all others

### CORS
- Vite proxy handles CORS during development
- Backend needs @CrossOrigin for production
- Allowed origins: http://localhost:7777

### Rate Limiting
- Gateway service (quiz-gateway) should implement:
  - Rate limiting per user
  - AI generation throttling
  - DDoS protection

## Monitoring & Logging

### Frontend Logging
```typescript
console.log('[API] Request:', { url, method, headers })
console.log('[API] Response:', { status, data })
console.log('[API] Error:', { status, url, message })
```

### Backend Logging
```java
log.info("API request: {}", endpoint);
log.error("API error: {}", exception);
```

### Metrics to Track
- Request latency per endpoint
- Error rates (4xx, 5xx)
- AI generation processing time
- Quiz completion rates
- User engagement metrics

## Future Enhancements

1. **GraphQL API** - Alternative to REST for complex queries
2. **WebSocket API** - Real-time quiz updates
3. **API Versioning** - /v1/, /v2/ paths
4. **Request Caching** - Reduce redundant calls
5. **Batch Operations** - Bulk question CRUD
6. **Search API** - Full-text search for banks/questions
7. **Analytics API** - Detailed usage statistics
8. **Webhook API** - Notifications for quiz completion
