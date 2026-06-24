# NeoGaming — Marketplace Gaming Colombia

Plataforma de comercio electrónico de videojuegos y periféricos para el mercado colombiano.

**Stack:** Spring Boot 4.0.6 · Java 21 · Angular 21 · PostgreSQL 17 · Redis · FastAPI (IA) · Mercado Pago

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java | 21 |
| Node.js | 20+ |
| npm | 10+ |
| Docker + Docker Compose | 24+ |
| PostgreSQL | 16+ (local) |

---

## Levantar el proyecto en local

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd plan-a
```

### 2. Configurar variables de entorno

Copia los archivos de ejemplo y completa los valores:

```bash
cp backend/.env.example backend/.env
cp neogaming-ai-service/.env.example neogaming-ai-service/.env
```

Variables clave en `backend/.env`:

```env
DB_URL=jdbc:postgresql://localhost:5432/neogaming
DB_USERNAME=<tu_usuario_postgres>
DB_PASSWORD=<tu_password_postgres>
JWT_SECRET=neogaming-super-secret-dev-key-change-in-production-64chars!!
MP_ACCESS_TOKEN=<token_sandbox_mercadopago>
AI_INTERNAL_SECRET=neo-dev-secret-2026
```

### 3. Importar la base de datos

Solicita el archivo `neogaming_seed.sql` a un compañero del equipo (no está en el repo por contener datos reales) y ejecuta:

```bash
./setup-db.sh
```

El script crea la BD `neogaming`, importa el schema y los datos, y muestra el conteo final.

> Si tu usuario de postgres es diferente al del sistema, pásalo como variable:
> ```bash
> PG_USER=postgres ./setup-db.sh
> ```

### 4. Levantar todo con un comando

```bash
./dev.sh
```

El script levanta en orden:

1. **Docker** — Redis + MailHog
2. **AI Microservice** — FastAPI/uvicorn en `:8001`
3. **Backend** — Spring Boot en `:8080`
4. **Frontend** — Angular en `:4200`

Para en cualquier momento con `Ctrl+C`.

```bash
./dev.sh stop   # detener todo
./dev.sh logs   # ver rutas de logs
```

### 5. URLs de desarrollo

| Servicio | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| AI Service | http://localhost:8001 |
| MailHog (emails) | http://localhost:8025 |

---

## Estructura del proyecto

```
plan-a/
├── backend/                 # Spring Boot — API REST
│   ├── src/
│   └── .env                 # Variables locales (no commitear)
├── front-neo/               # Angular 21 — Frontend SSR
├── neogaming-ai-service/    # FastAPI — Microservicio IA (Gemini)
├── docker-compose.yml       # Infraestructura local (Redis, MailHog)
├── dev.sh                   # Script para levantar todo en local
└── setup-db.sh              # Script para importar la BD
```

---

## Compartir la base de datos con el equipo

El archivo `neogaming_seed.sql` **no está en el repositorio** porque contiene datos reales. Para compartirlo:

1. Genera un dump actualizado desde Railway:
   ```bash
   pg_dump "postgresql://postgres:<pass>@<host>/railway" \
     --no-owner --no-acl --inserts -f neogaming_seed.sql
   ```
2. Comparte el archivo por Drive o Discord
3. El compañero ejecuta `./setup-db.sh`

---

## Despliegue (Railway)

El proyecto se despliega automáticamente en Railway al hacer push a `main`.

Servicios en producción:
- **Backend**: `backend-neogamin-production-58ed.up.railway.app`
- **Frontend**: SSR con Node
- **AI Service**: FastAPI containerizado
- **PostgreSQL** y **Redis**: gestionados por Railway
