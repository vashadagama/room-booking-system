# Room Booking System

Мини-система бронирования переговорных комнат.
REST API на Spring Boot + SPA на Vue 3.

## Стек технологий

**Backend:**
- Java 21+ / Spring Boot 4
- Spring Web (REST), Spring Data JPA
- PostgreSQL 18 (Docker)
- Lombok, Bean Validation, Swagger UI

**Frontend:**
- Vue 3 + Vite
- Vue Router 4, Pinia, Axios

## Требования

- Docker Desktop
- JDK 21+
- Node.js 20+

## Запуск

Нужно **3 терминала**.

### 1. База данных

Из **корня** проекта:
```bash
docker compose up -d postgres
```

### 2. Бэкэнд

Из папки **backend/**:
```bash
./mvnw spring-boot:run
```

### 3. Фронтенд

Из папки **frontend/**:
```bash
npm install   # только в первый раз
npm run dev
```

Открой: http://localhost:5173