# Room Booking System

Мини-система бронирования переговорных комнат. REST API на Spring Boot + SPA на Vue 3.

## Стек

**Backend:** Java 26+, Spring Boot 4, Spring Data JPA, PostgreSQL, Lombok, Swagger UI  
**Frontend:** Vue 3 + Vite, Vue Router 4, Pinia, Axios  
**DevOps:** Docker, Docker Compose

## Требования

- Docker Desktop
- JDK 26+
- Node.js 20+

## Запуск

```bash
# База данных
docker compose up -d postgres

# Бэкенд (из папки backend/)
./mvnw spring-boot:run

# Фронтенд (из папки frontend/)
npm install
npm run dev
```

Открыть: http://localhost:5173

## Админ-панель

http://localhost:5173/admin — управление комнатами и сводка по системе.  
Права ADMIN выдаются через консольную оболочку (профиль `cli`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=cli"
promote --username <логин>
```
