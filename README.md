# Reply AI — Backend

AI-powered messenger assistant backend built with Django REST Framework.

## Tech Stack
- Django 6 + DRF
- PostgreSQL
- Redis + Celery
- JWT Auth + Google OAuth
- Groq AI (Llama 3.3 70B)
- Swagger UI
- Docker

## Quick Start

### 1. Clone
```bash
git clone https://github.com/yourusername/reply_ai.git
cd reply_ai
```

### 2. Setup env
```bash
cp .env.example .env
# Fill in your values
```

### 3. Run with Docker
```bash
docker-compose up --build
```

### 4. Migrate & load fixtures
```bash
docker-compose exec web python manage.py migrate
docker-compose exec web python manage.py loaddata users/fixtures/plans.json
docker-compose exec web python manage.py loaddata ai_settings/fixtures/languages.json
docker-compose exec web python manage.py createsuperuser
```

### 5. API Docs
- Swagger: http://localhost:8005/api/docs/
- ReDoc: http://localhost:8005/api/redoc/
- Admin: http://localhost:8005/admin/

## API Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| POST | /api/users/register/ | Registration |
| POST | /api/users/login/ | Login (JWT) |
| GET | /api/users/profile/ | My profile |
| GET | /api/users/my-subscription/ | My subscription |
| POST | /api/chats/sessions/ | Create chat session |
| POST | /api/chats/sessions/{id}/ask/ | Ask AI |
| PATCH | /api/chats/responses/{id}/feedback/ | Like/dislike |
| GET | /api/settings/my-settings/ | My settings |
