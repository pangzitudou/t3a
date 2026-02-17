# T3A Developer Context (Synchronized)

This file is a compact context brief for coding agents. It is aligned with the current repository state.

## Stack
- Backend: Spring Boot 3.2.2, Spring Cloud Alibaba, Java 17
- Frontend: React 18.3, Vite 5.4, TypeScript
- Infra: MySQL, Redis, Nacos, RocketMQ

## Services
- `quiz-gateway` -> `:8080`
- `quiz-core` -> `:8081` with context path `/quiz`
- `quiz-ai` -> `:8082`
- `quiz-communication` -> `:8083`
- Frontend dev -> `:5173`

## Routing Reality
- Frontend dev traffic usually goes through Vite proxy:
  - `/api/quiz/*` -> `http://localhost:8081/quiz/*`
  - `/api/ai/*` -> `http://localhost:8082/*`
- Gateway routes exist for `/api/quiz/**`, `/api/ai/**`, `/api/ws/**` but are not required for local frontend dev.

## Auth Model
- `quiz-core` uses JWT with Spring Security.
- `/auth/**` is public; others require `Authorization: Bearer <token>`.

## Important Implementation Status
- Working: auth, bank/question CRUD, session start/submit, AI generation task + status polling.
- Pending/TODO:
  - save generated AI questions into `quiz-core`
  - real dashboard statistics
  - real AI analysis content
  - full answer scoring persistence in session answer endpoint

## Startup (recommended)
```bash
docker compose -f docs/docker-compose.yml up -d
mvn clean install -DskipTests
# run each backend module with mvn spring-boot:run
# run frontend with npm run dev in quiz-frontend
```

## Documentation Sync Policy
When code changes affect behavior, update docs in the same change set:
- `docs/README.md`
- `docs/STARTUP-GUIDE.md`
- `docs/ARCHITECTURE.md`
- `docs/PROJECT-SUMMARY.md`
- `docs/AGENT-HANDBOOK.md`
