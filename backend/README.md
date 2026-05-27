# NeoGaming Backend

API REST para el marketplace de gaming más importante de Colombia. Conecta compradores con vendedores de consolas, periféricos, componentes PC y juegos.

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.0.6 (Spring Framework 7) |
| Seguridad | Spring Security 7 + JWT (JJWT 0.12.6) |
| Persistencia | Spring Data JPA + Hibernate ORM 7.2 |
| Base de datos | PostgreSQL |
| Migraciones | Flyway 11 |
| Documentación | SpringDoc OpenAPI 3.0 (Swagger UI) |
| Email | Spring Mail + Thymeleaf (plantillas HTML) |
| Pagos | Mercado Pago (preferencias + webhooks) |
| Build | Maven 3 |
| Utilidades | Lombok 1.18 |

---

## Requisitos previos

- Java 21+
- Maven 3.9+
- PostgreSQL 14+ corriendo en `localhost:5432`
- (Opcional) Docker + Docker Compose para MailHog

---

## Configuración inicial

### 1. Crear base de datos y usuario

```sql
CREATE USER neogaming WITH PASSWORD 'neogaming';
CREATE DATABASE neogaming_dev OWNER neogaming TEMPLATE template0;
GRANT ALL PRIVILEGES ON DATABASE neogaming_dev TO neogaming;
```

### 2. Habilitar pgcrypto (para el seed)

```bash
sudo -u postgres psql -d neogaming_dev -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
```

### 3. Variables de entorno

El proyecto usa valores por defecto para desarrollo local. Solo necesitas exportar variables en producción:

| Variable | Defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/neogaming_dev` | URL de conexión |
| `DB_USERNAME` | `neogaming` | Usuario PostgreSQL |
| `DB_PASSWORD` | `neogaming` | Contraseña PostgreSQL |
| `JWT_SECRET` | *(incluido en yml)* | Clave HMAC-SHA256 para firmar tokens |
| `JWT_ACCESS_TOKEN_MINUTES` | `15` | Duración del access token |
| `JWT_REFRESH_TOKEN_DAYS` | `30` | Duración del refresh token |
| `MAIL_HOST` | `localhost` | Servidor SMTP |
| `MAIL_PORT` | `1025` | Puerto SMTP |
| `MP_ACCESS_TOKEN` | *(token de prueba)* | Access token de Mercado Pago |
| `MP_WEBHOOK_SECRET` | *(vacío)* | Secreto para validar webhooks de MP |
| `MP_SANDBOX` | `true` | Modo sandbox de Mercado Pago |
| `PLATFORM_FEE_PERCENTAGE` | `5.0` | Comisión de la plataforma (%) |

### 4. Ejecutar el servidor

**Linux / macOS**
```bash
cd "/ruta/al/proyecto"
mvn spring-boot:run
```

**Windows**
```cmd
cd "C:\ruta\al\proyecto"
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### 5. Seed de datos de prueba

**Linux / macOS**
```bash
cp seed_data.sql /tmp/seed_data.sql
sudo -u postgres psql -d neogaming_dev -f /tmp/seed_data.sql
```

**Windows** (cmd o PowerShell — PostgreSQL debe estar en el PATH)
```cmd
psql -U postgres -d neogaming_dev -f "seed_data.sql"
```
> En Windows te pedirá la contraseña del usuario `postgres`. Si usas pgAdmin, también puedes abrir el archivo con la herramienta Query Tool.

Carga 16 categorías, 1 admin, 5 vendedores y 250 productos con inventario.

---

## Documentación interactiva

Swagger UI disponible en desarrollo:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON en:

```
http://localhost:8080/v3/api-docs
```

---

## Arquitectura de módulos

El proyecto sigue una arquitectura por dominio (package by feature):

```
com.neogaming
├── auth          → Login, registro, JWT, refresh token, sesiones
├── user          → Perfil de usuario, cambio de contraseña
├── address       → Direcciones de entrega del comprador
├── catalog
│   ├── category  → Categorías y subcategorías de productos
│   ├── product   → Catálogo de productos e imágenes
│   └── offer     → Ofertas y descuentos por tiempo limitado
├── seller        → Perfil de vendedor, tienda, cuenta de cobro
├── inventory     → Stock físico, stock reservado, movimientos
├── cart          → Carrito de compras
├── checkout      → Proceso de pago, expiración automática
├── payment       → Integración Mercado Pago, estado de pagos
├── order         → Pedidos, grupos de pedido por vendedor, items
├── invoice       → Facturas electrónicas por pedido
├── review        → Reseñas y calificaciones de productos
├── wishlist      → Lista de deseos del usuario
├── analytics     → Dashboard para admin y vendedores
└── common        → Enums compartidos, entidad auditable base
```

Cada módulo contiene:
- `domain/` — Entidades JPA
- `dto/` — Request y Response records
- `repository/` — Interfaces Spring Data JPA
- `service/` — Lógica de negocio
- `controller/` — Endpoints REST
- `mapper/` — Conversión entidad ↔ DTO

---

## Seguridad

### Autenticación

La API usa JWT stateless. Cada request autenticado debe incluir:

```
Authorization: Bearer <access_token>
```

El access token expira en 15 minutos. Para renovarlo, usa el refresh token (válido 30 días):

```
POST /auth/refresh
{ "refreshToken": "..." }
```

### Roles

| Rol | Descripción |
|---|---|
| `BUYER` | Comprador (rol por defecto al registrarse) |
| `SELLER` | Vendedor con tienda activa |
| `ADMIN` | Administrador de la plataforma |

Los endpoints con restricción de rol usan `@PreAuthorize` a nivel de método.

### Endpoints públicos

- `GET /products/**` — Catálogo público
- `GET /categories/**` — Categorías públicas
- `GET /sellers/{storeSlug}` — Perfil público de tienda
- `POST /auth/**` — Login y registro
- `POST /webhooks/**` — Webhooks de Mercado Pago
- `/swagger-ui/**`, `/v3/api-docs/**` — Documentación

---

## Endpoints principales

### Auth
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/register` | Registrar nuevo usuario |
| POST | `/auth/login` | Login, devuelve access + refresh token |
| POST | `/auth/refresh` | Renovar access token |
| POST | `/auth/logout` | Invalidar sesión actual |

### Catálogo
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/categories` | Listar categorías |
| GET | `/categories/{id}` | Detalle de categoría |
| POST | `/categories` | Crear categoría (ADMIN) |
| GET | `/products` | Listar productos (filtros: category, seller, search, minPrice, maxPrice) |
| GET | `/products/{id}` | Detalle de producto |
| POST | `/products` | Crear producto (SELLER) |
| PUT | `/products/{id}` | Actualizar producto (SELLER dueño) |
| DELETE | `/products/{id}` | Eliminar producto (SELLER dueño / ADMIN) |

### Carrito
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/cart` | Ver carrito activo |
| POST | `/cart/items` | Agregar ítem |
| PUT | `/cart/items/{itemId}` | Actualizar cantidad |
| DELETE | `/cart/items/{itemId}` | Eliminar ítem |
| DELETE | `/cart` | Vaciar carrito |

### Checkout y Pagos
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/checkout` | Iniciar checkout desde carrito |
| GET | `/checkout/{id}` | Ver estado del checkout |
| POST | `/checkout/{id}/pay` | Generar preferencia de pago en Mercado Pago |
| POST | `/webhooks/mercadopago` | Webhook de confirmación de pago |

### Pedidos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/orders` | Mis pedidos |
| GET | `/orders/{id}` | Detalle de pedido |
| PUT | `/orders/{id}/status` | Actualizar estado (SELLER / ADMIN) |

### Vendedor
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/sellers/register` | Registrar como vendedor |
| GET | `/sellers/me` | Mi perfil de vendedor |
| PUT | `/sellers/me` | Actualizar perfil de tienda |
| GET | `/sellers/{storeSlug}` | Perfil público de tienda |
| GET | `/sellers/me/products` | Mis productos |
| GET | `/sellers/me/orders` | Mis pedidos como vendedor |

### Admin
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/admin/sellers` | Listar todos los vendedores |
| PUT | `/admin/sellers/{id}/approve` | Aprobar vendedor |
| PUT | `/admin/sellers/{id}/suspend` | Suspender vendedor |
| GET | `/analytics/admin` | Dashboard admin |
| GET | `/analytics/seller` | Dashboard vendedor |

---

## Usuarios de prueba

Ejecuta ambos scripts en orden para tener todos los datos disponibles.

**Linux / macOS**
```bash
# 1. Categorías, vendedores y 250 productos
cp seed_data.sql /tmp/seed_data.sql
sudo -u postgres psql -d neogaming_dev -f /tmp/seed_data.sql

# 2. Compradores, órdenes, direcciones y reseñas
cp seed_orders_reviews.sql /tmp/seed_orders.sql
sudo -u postgres psql -d neogaming_dev -f /tmp/seed_orders.sql
```

**Windows** (cmd o PowerShell — PostgreSQL debe estar en el PATH)
```cmd
:: 1. Categorías, vendedores y 250 productos
psql -U postgres -d neogaming_dev -f "seed_data.sql"

:: 2. Compradores, órdenes, direcciones y reseñas
psql -U postgres -d neogaming_dev -f "seed_orders_reviews.sql"
```
> En Windows te pedirá la contraseña del usuario `postgres` para cada archivo. Ejecuta los comandos desde la carpeta raíz del proyecto.

### Admin

| Email | Contraseña | Rol |
|---|---|---|
| `admin@neogaming.co` | `Admin123!` | ADMIN |

Puede hacer todo: aprobar/suspender vendedores, moderar reseñas, ver dashboard global, gestionar categorías.

### Vendedores (SELLER)

| Email | Contraseña | Tienda | Ciudad |
|---|---|---|---|
| `andres.tech@neogaming.co` | `Password123!` | TechStore Colombia | Bogotá |
| `maria.games@neogaming.co` | `Password123!` | Gamer Paradise | Medellín |
| `carlos.components@neogaming.co` | `Password123!` | PC Master Race CO | Cali |
| `laura.gaming@neogaming.co` | `Password123!` | NextGen Gaming | Barranquilla |
| `juan.perifericos@neogaming.co` | `Password123!` | Periféricos Pro | Bucaramanga |

Cada vendedor tiene 50 productos activos con inventario. Pueden gestionar sus productos, ver sus pedidos y su dashboard de ventas.

### Compradores (CLIENT)

| Email | Contraseña | Nombre | Órdenes |
|---|---|---|---|
| `sofia.buyer@neogaming.co` | `Password123!` | Sofia Ramirez | 3 (DELIVERED, SHIPPED, PROCESSING) |
| `miguel.buyer@neogaming.co` | `Password123!` | Miguel Torres | 3 (DELIVERED, SHIPPED, PROCESSING) |

Sofia tiene 2 reseñas APPROVED. Miguel tiene 1 APPROVED y 1 PENDING (esperando moderación del admin).

### Categorías disponibles

**Padres:** Consolas y Videojuegos, Periféricos Gaming, Componentes PC, Monitores, Sillas y Escritorios, Juegos, Streaming y Contenido

**Subcategorías:** PlayStation, Xbox, Nintendo Switch, Teclados Gaming, Mouses Gaming, Audífonos Gaming, Tarjetas Gráficas, Procesadores, Memoria RAM

---

## Flujos de prueba en Postman

Importa `neogaming-postman-collection.json`. El token se guarda automáticamente al hacer login.

### Flujo 1 — Explorar el catálogo (sin autenticación)

```
GET /categories                          → Ver todas las categorías
GET /products                            → Listar productos
GET /products?search=mouse               → Buscar por nombre
GET /products?category=mouses-gaming     → Filtrar por categoría (slug)
GET /products/{id}                       → Ver detalle de un producto
GET /sellers/techstore-colombia          → Ver perfil público de una tienda
```

### Flujo 2 — Compra completa (como comprador)

```
POST /auth/login { sofia.buyer@neogaming.co / Password123! }
GET  /cart                               → Ver carrito (vacío)
POST /cart/items { productId, quantity } → Agregar producto
POST /cart/items { productId, quantity } → Agregar otro producto de otro vendedor
POST /checkout { addressId }             → Iniciar checkout
POST /checkout/{id}/pay                  → Obtener link de pago Mercado Pago
  → (en sandbox MP, aprobar el pago)
GET  /orders                             → Ver pedido generado
GET  /orders/{id}                        → Ver detalle con grupos por vendedor
```

### Flujo 3 — Ver pedidos ya existentes (como comprador)

```
POST /auth/login { sofia.buyer@neogaming.co / Password123! }
GET  /orders                             → Ver sus 3 órdenes (DELIVERED, SHIPPED, PROCESSING)
GET  /orders/{orderId}                   → Detalle con ítems y grupos
GET  /reviews                            → Ver sus 2 reseñas APPROVED
```

### Flujo 4 — Gestionar mi tienda (como vendedor)

```
POST /auth/login { andres.tech@neogaming.co / Password123! }
GET  /sellers/me                         → Ver perfil de la tienda
GET  /sellers/me/products                → Ver mis 50 productos
POST /products { ... }                   → Crear nuevo producto
PUT  /products/{id} { ... }              → Actualizar precio o descripción
GET  /sellers/me/orders                  → Ver pedidos donde aparezco
PUT  /orders/{id}/status { SHIPPED }     → Marcar como enviado (agrega tracking)
GET  /analytics/seller                   → Ver dashboard de ventas
```

### Flujo 5 — Administración (como admin)

```
POST /auth/login { admin@neogaming.co / Admin123! }
GET  /admin/sellers                      → Listar todos los vendedores
PUT  /admin/sellers/{id}/suspend         → Suspender un vendedor
POST /categories { name, slug }          → Crear nueva categoría
GET  /reviews?status=PENDING             → Ver reseñas pendientes de moderación
PUT  /reviews/{id}/moderate { APPROVED } → Aprobar la reseña de Miguel
GET  /analytics/admin                    → Dashboard global de la plataforma
```

### Flujo 6 — Reseñas (como comprador con compra previa)

```
POST /auth/login { miguel.buyer@neogaming.co / Password123! }
GET  /orders                             → Buscar la orden DELIVERED (ord4)
POST /reviews { productId, orderId, rating: 5, title, body }  → Crear reseña
  → Queda en estado PENDING hasta que admin la aprueba
```

---

## Colección Postman

Importa `neogaming-postman-collection.json`. La colección incluye:

- Variables automáticas: `accessToken` y `refreshToken` se guardan al hacer login
- 17 carpetas con todos los endpoints organizados por módulo
- Variables de entorno: `baseUrl`, `productId`, `orderId`, `sellerId`, etc.

---

## Email (desarrollo local)

MailHog captura todos los emails en desarrollo (confirmación de registro, notificaciones de pedido).

**Linux / macOS**
```bash
docker compose up -d mailhog
```

**Windows**
```cmd
docker compose up -d mailhog
```
> El comando es el mismo en ambos sistemas. Requiere Docker Desktop instalado y corriendo.

Interfaz web: `http://localhost:8025`

---

## Modelo de negocio

- **Comisión de plataforma:** 5% sobre cada venta (configurable vía `PLATFORM_FEE_PERCENTAGE`)
- **IVA:** 19% incluido en el precio (Colombia)
- **Moneda:** COP (Pesos Colombianos)
- **Pagos:** Mercado Pago con split automático vendedor / plataforma
- **Facturación:** Factura electrónica generada al confirmar el pago

---

## Notas de compatibilidad

Este proyecto usa Spring Boot 4.0.6 (prerrelease) con cambios de ruptura respecto a 3.x:

- **Spring Security 7:** `DaoAuthenticationProvider` recibe `UserDetailsService` en el constructor
- **Hibernate ORM 7.2:** `GenerationType.UUID` genera el UUID en Java, no en la BD
- **Jackson:** Spring Boot 4 auto-configura `tools.jackson` (v3.x); `com.fasterxml` (v2.x) requiere bean explícito (`JacksonConfig.java`)
- **TestRestTemplate:** Eliminado en SB4 — usar `RestTemplate` con `DefaultResponseErrorHandler`
