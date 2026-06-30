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

## Setup paso a paso

Sigue este orden exacto la primera vez que configures el proyecto.

---

### Paso 1 — Clonar el repositorio

```bash
git clone <url-del-repo>
cd plan-a
```

> El repositorio principal contiene el backend y el microservicio de IA.
> El frontend (`front-neo/`) es un submódulo o repo anidado — ver Paso 2.

---

### Paso 2 — Instalar dependencias del frontend

El frontend vive en `front-neo/` y tiene su propio `package.json`:

```bash
cd front-neo
npm install
cd ..
```

---

### Paso 3 — Configurar variables de entorno

**Backend:**

```bash
cp backend/.env.example backend/.env
```

Edita `backend/.env` y completa:

```env
DB_URL=jdbc:postgresql://localhost:5432/neogaming
DB_USERNAME=<tu_usuario_postgres>       # ej: yuseth, postgres
DB_PASSWORD=<tu_password_postgres>
JWT_SECRET=neogaming-super-secret-dev-key-change-in-production-64chars!!
MP_ACCESS_TOKEN=<token_sandbox_mercadopago>
AI_INTERNAL_SECRET=neo-dev-secret-2026
REDIS_HOST=localhost
REDIS_PORT=6379
```

**Microservicio IA:**

```bash
cp neogaming-ai-service/.env.example neogaming-ai-service/.env
```

Edita `neogaming-ai-service/.env` y completa:

```env
GEMINI_API_KEY=<tu_api_key_gemini>
AI_INTERNAL_SECRET=neo-dev-secret-2026
SPRING_BOOT_BASE_URL=http://localhost:8080
```

> Pide las claves al líder del equipo. Nunca las compartas por chat ni las subas al repo.

---

### Paso 4 — Configurar PostgreSQL local

Asegúrate de tener PostgreSQL instalado y corriendo. Luego establece una contraseña para tu usuario (necesaria para que Spring Boot conecte por TCP):

```sql
-- Ejecutar en pgAdmin o psql como superusuario:
ALTER USER <tu_usuario> WITH PASSWORD '<tu_password>';
```

---

### Paso 5 — Importar la base de datos

Solicita el archivo `neogaming_seed.sql` a un compañero (no está en el repo por contener datos reales) y colócalo en la raíz del proyecto. Luego ejecuta:

```bash
./setup-db.sh
```

El script crea la BD `neogaming`, importa el schema completo y todos los datos, y muestra el conteo final para confirmar.

```
✓ Base de datos lista
  Usuarios:   16
  Productos:  42
  Categorías: 10
```

> Si tu usuario de postgres es diferente al del sistema operativo:
> ```bash
> PG_USER=postgres ./setup-db.sh
> ```

---

### Paso 6 — Levantar todo

```bash
./dev.sh
```

El script levanta los servicios en este orden y espera que cada uno responda antes de continuar:

| Orden | Servicio | Puerto |
|---|---|---|
| 1 | Docker: Redis | 6379 |
| 2 | Docker: MailHog | 8025 |
| 3 | AI Microservice (FastAPI) | 8001 |
| 4 | Backend (Spring Boot) | 8080 |
| 5 | Frontend (Angular) | 4200 |

Para detener todo: `Ctrl+C` o `./dev.sh stop`

```bash
./dev.sh logs   # ver rutas de cada log
```

---

### Paso 7 — Verificar que todo funciona

| Servicio | URL | Qué verificar |
|---|---|---|
| Frontend | http://localhost:4200 | Catálogo con productos |
| Backend API | http://localhost:8080/swagger-ui.html | Swagger carga |
| AI Service | http://localhost:8001/api/v1/ai/health | `{"status":"ok"}` |
| MailHog | http://localhost:8025 | Panel de emails vacío |

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
- **Backend**: `backend-neogamin-production-c183.up.railway.app`
- **Frontend**: SSR con Node
- **AI Service**: FastAPI containerizado
- **PostgreSQL** y **Redis**: gestionados por Railway
