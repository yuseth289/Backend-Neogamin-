# NeoGaming Frontend Context for Claude

## 1. Objetivo de este documento

Este documento está pensado para que Claude pueda **construir el frontend** de NeoGaming sin tener que leer todo el backend.

Úsalo como guía para:

- definir rutas de la app
- decidir qué pantallas existen
- saber qué endpoint consume cada vista
- entender qué estados de UI hay que soportar
- implementar auth, guards, checkout y dashboards por rol

## 2. Stack frontend actual

Ya existe un frontend base en `front-neo/` con:

- Angular `21`
- Angular Router
- SSR habilitado
- Tailwind/PostCSS en dependencias

Archivo clave:

- [front-neo/package.json](/home/yuseth28/Documentos/plan%20a/front-neo/package.json:1)
- [front-neo/src/app/app.routes.ts](/home/yuseth28/Documentos/plan%20a/front-neo/src/app/app.routes.ts:1)

Importante:

- `app.routes.ts` está vacío.
- Claude puede proponer la estructura de rutas desde cero.

## 3. Qué es NeoGaming

NeoGaming es un marketplace multi-vendedor de gaming:

- un usuario común compra productos
- un usuario puede solicitar convertirse en vendedor
- un admin aprueba vendedores y modera partes del sistema

Roles reales del backend:

- `CLIENT`
- `SELLER`
- `ADMIN`

Importante:

- aunque en comentarios antiguos aparezca `BUYER`, el rol real del comprador en código es `CLIENT`

## 4. Reglas globales de API que el frontend debe asumir

### Base auth

El backend usa JWT.

Request autenticado:

```http
Authorization: Bearer <accessToken>
```

Flujo:

- `accessToken` dura poco
- `refreshToken` sirve para renovar sesión
- el refresh también viene desde API, no desde cookie httpOnly

Endpoints auth:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### Formato de respuesta

Casi toda respuesta viene así:

```json
{
  "status": "success",
  "message": "opcional",
  "data": {},
  "timestamp": "2026-05-20T00:00:00Z"
}
```

Las listas paginadas vienen dentro de `data`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

### Reglas de acceso

Rutas públicas del backend:

- `POST /auth/**`
- `GET /products/**`
- `GET /categories/**`
- `GET /sellers/{storeSlug}`
- `POST /webhooks/**`

Todo lo demás requiere JWT.

### Fuente de verdad

Si hay diferencias entre README, tests viejos y backend real, usar como fuente de verdad:

1. controllers
2. services
3. `SecurityConfig`

## 5. Qué app frontend debería existir

Claude debería pensar la app en 4 áreas:

### 5.1 Público

- home
- catálogo
- detalle de producto
- categorías
- tienda pública de vendedor
- login / registro
- wishlist pública compartida

### 5.2 Cliente autenticado

- perfil
- direcciones
- carrito
- checkout
- estado post-pago
- mis órdenes
- mis facturas
- mis reseñas
- mis wishlists

### 5.3 Seller

- onboarding de vendedor
- perfil seller
- cuentas bancarias
- mis productos
- crear/editar producto
- imágenes de producto
- inventario
- ofertas
- órdenes del seller por grupo
- analytics seller

### 5.4 Admin

- gestión de sellers
- moderación de reseñas
- facturas admin
- analytics admin
- categorías admin

## 6. Propuesta de rutas frontend

Claude puede usar algo cercano a esto:

### Público

- `/`
- `/catalog`
- `/catalog/category/:categoryId`
- `/product/:slug`
- `/seller/:storeSlug`
- `/login`
- `/register`
- `/wishlist/public/:id`

### Cliente autenticado

- `/account`
- `/account/addresses`
- `/cart`
- `/checkout`
- `/orders`
- `/orders/:id`
- `/invoices`
- `/reviews/me`
- `/wishlists`
- `/wishlists/:id`

### Seller

- `/become-seller`
- `/seller/dashboard`
- `/seller/profile`
- `/seller/accounts`
- `/seller/products`
- `/seller/products/new`
- `/seller/products/:id`
- `/seller/products/:id/inventory`
- `/seller/products/:id/offers`
- `/seller/orders`
- `/seller/analytics`

### Admin

- `/admin`
- `/admin/sellers`
- `/admin/reviews`
- `/admin/invoices`
- `/admin/categories`
- `/admin/analytics`

## 7. Guards y layout que Claude debería implementar

### Guards

- `guestOnlyGuard`
  - bloquea `/login` y `/register` si ya hay sesión

- `authGuard`
  - exige JWT

- `roleGuard`
  - exige `CLIENT`, `SELLER` o `ADMIN` según ruta

### Layouts

- layout público
- layout de cuenta autenticada
- layout seller
- layout admin

### Interceptor

Claude debería crear un `HttpInterceptor` que:

- agregue `Authorization` si hay `accessToken`
- si recibe `401`, intente `POST /auth/refresh`
- reintente el request original
- si refresh falla, limpie sesión y redirija a login

## 8. Estado de sesión que el frontend necesita

La respuesta auth devuelve:

```json
{
  "accessToken": "jwt",
  "refreshToken": "uuid",
  "userId": "uuid",
  "role": "CLIENT"
}
```

Claude debería manejar un `auth state` con:

- `accessToken`
- `refreshToken`
- `userId`
- `role`
- `isAuthenticated`

Idealmente, además cargar `GET /users/me` después de login/refresh para hidratar header, perfil y guards.

## 9. Pantallas clave y qué endpoint consume cada una

## 9.1 Home / catálogo

Objetivo UI:

- mostrar categorías
- mostrar catálogo paginado
- permitir búsqueda

Endpoints:

- `GET /categories`
- `GET /products`
- `GET /products/search?q=...`
- `GET /products/category/{categoryId}`

Datos útiles:

- `ProductSummaryResponse` ya trae `finalPrice`
- `primaryImageUrl` viene listo para card

UI states:

- loading de catálogo
- empty state si no hay resultados
- filtros / búsqueda sin resultados

## 9.2 Detalle de producto

Endpoint principal:

- `GET /products/{slug}`

Endpoints complementarios:

- `GET /products/{productId}/offers/current`
- `GET /products/{productId}/reviews`
- `GET /products/{productId}/reviews/summary`

Acciones desde esa vista:

- agregar al carrito
- agregar a wishlist
- ver tienda del seller

## 9.3 Login / registro

Endpoints:

- `POST /auth/register`
- `POST /auth/login`

Payload register:

```json
{
  "email": "juan@example.com",
  "password": "Password123!",
  "firstName": "Juan",
  "lastName": "García",
  "phone": "3001234567"
}
```

Payload login:

```json
{
  "email": "juan@example.com",
  "password": "Password123!"
}
```

UX importante:

- después de login/register, persistir tokens y rol
- redirigir según rol:
  - `CLIENT` -> `/`
  - `SELLER` -> `/seller/dashboard` o `/seller/products`
  - `ADMIN` -> `/admin`

## 9.4 Perfil y direcciones

Endpoints perfil:

- `GET /users/me`
- `PUT /users/me`
- `PUT /users/me/password`

Endpoints direcciones:

- `GET /users/me/addresses`
- `POST /users/me/addresses`
- `GET /users/me/addresses/{id}`
- `PUT /users/me/addresses/{id}`
- `DELETE /users/me/addresses/{id}`
- `PATCH /users/me/addresses/{id}/set-primary`

Payload dirección:

```json
{
  "label": "Casa",
  "street": "Cra 7",
  "number": "# 32-15",
  "floor": "3",
  "apartment": "301",
  "city": "Bogotá",
  "department": "Cundinamarca",
  "postalCode": "110311"
}
```

UX importante:

- resaltar dirección principal
- impedir checkout si el usuario no tiene direcciones

## 9.5 Carrito

Endpoints:

- `GET /cart`
- `POST /cart/items`
- `PATCH /cart/items/{id}?quantity=n`
- `DELETE /cart/items/{id}`
- `DELETE /cart`

Payload agregar al carrito:

```json
{
  "productId": "uuid",
  "quantity": 2
}
```

Datos importantes de respuesta:

- `items`
- `total`
- `totalItems`
- `hasPriceChanges`

UX importante:

- si `hasPriceChanges` es `true`, mostrar alerta global
- permitir editar quantity inline
- manejar carrito vacío

## 9.6 Checkout

Endpoints:

- `POST /checkout`
- `GET /checkout/current`
- `DELETE /checkout/current`

Payload:

```json
{
  "addressId": "uuid",
  "paymentMethod": "MP_PSE"
}
```

Respuesta importante:

- `id`
- `status`
- `items`
- `subtotal`
- `shippingCost`
- `total`
- `paymentMethod`
- `expiresAt`
- `minutesLeft`

UX importante:

- mostrar resumen congelado del checkout
- mostrar countdown con `minutesLeft`
- permitir cancelar checkout
- si expira, informar que debe reiniciar compra

## 9.7 Pago

Endpoint:

- `POST /payments/checkout/{checkoutId}`

Respuesta importante:

- `checkoutUrl`

Flujo frontend:

1. crear checkout
2. llamar endpoint de pago
3. redirigir al usuario a `checkoutUrl`

Nota:

- el backend no expone en esta capa una pantalla de polling formal
- Claude debería crear una pantalla de retorno tipo `/checkout/result` o reutilizar `/orders`
- como el pago se confirma por webhook, el frontend debe refrescar órdenes/checkout tras volver de Mercado Pago

## 9.8 Órdenes e historial de compra

Endpoints:

- `GET /orders`
- `GET /orders/{id}`
- `GET /invoices`
- `GET /invoices/order/{orderId}`

UI:

- lista de órdenes del cliente
- detalle por orden con grupos por vendedor
- timeline de estado
- acceso a factura

Estados de pedido útiles para UI:

- `PAYMENT_APPROVED`
- `PROCESSING`
- `PARTIALLY_SHIPPED`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`
- `REFUNDED`

## 9.9 Wishlists

Endpoints:

- `GET /wishlists`
- `POST /wishlists`
- `GET /wishlists/{id}`
- `PUT /wishlists/{id}`
- `DELETE /wishlists/{id}`
- `POST /wishlists/{id}/items/{productId}`
- `DELETE /wishlists/{id}/items/{productId}`
- `GET /wishlists/public/{id}`

UI:

- lista de listas
- detalle de lista
- compartir lista pública
- toggle agregar/quitar producto

## 9.10 Reseñas

Endpoints:

- `GET /products/{productId}/reviews`
- `GET /products/{productId}/reviews/summary`
- `POST /reviews`
- `GET /reviews/me`
- `DELETE /reviews/{id}`

UI:

- reseñas en detalle de producto
- pantalla “mis reseñas”
- formulario de reseña solo si compró

Reglas útiles:

- una reseña por usuario/producto
- la reseña entra en `PENDING`
- una aprobada no la puede borrar el cliente

## 9.11 Onboarding seller

Endpoint:

- `POST /sellers/register`

Payload:

```json
{
  "storeName": "Gaming Shop Bogotá",
  "storeDescription": "Tienda especializada en periféricos gaming",
  "tipoDocumento": "NIT",
  "numeroDocumento": "900123456-7",
  "razonSocial": "Gaming Shop SAS",
  "tipoRegimen": "RESPONSABLE_IVA",
  "phone": "3001234567",
  "address": "Cra 10 # 20-30",
  "city": "Bogotá",
  "department": "Cundinamarca"
}
```

UX:

- formulario de solicitud seller
- pantalla de estado “pendiente de aprobación”

## 9.12 Área seller

### Perfil y cuentas bancarias

Endpoints:

- `GET /sellers/me`
- `PUT /sellers/me`
- `GET /sellers/me/accounts`
- `POST /sellers/me/accounts`
- `PATCH /sellers/me/accounts/{id}/activate`
- `DELETE /sellers/me/accounts/{id}`

UX:

- lista de cuentas
- marcar activa
- no dejar borrar la activa sin feedback claro

### Productos seller

Endpoints:

- `GET /products/me`
- `POST /products`
- `GET /products/me/{id}`
- `PUT /products/me/{id}`
- `PATCH /products/me/{id}/publish`
- `PATCH /products/me/{id}/pause`
- `DELETE /products/me/{id}`

Payload crear/editar producto:

```json
{
  "name": "Teclado Mecánico",
  "description": "TKL para gaming",
  "brand": "Logitech",
  "sku": "LGT-001",
  "categoryId": "uuid",
  "basePrice": 350000.00,
  "ivaPercent": 19.00
}
```

UX:

- tabla de productos
- filtros por estado
- badge `DRAFT`, `ACTIVE`, `PAUSED`
- acciones publicar / pausar / eliminar

### Imágenes de producto

Endpoints:

- `POST /products/me/{id}/images`
- `PATCH /products/me/{id}/images/{imageId}/primary`
- `DELETE /products/me/{id}/images/{imageId}`

UX:

- reorder visual simple
- destacar imagen principal

### Inventario

Endpoints:

- `GET /inventory/{productId}`
- `POST /inventory/{productId}/stock`
- `PATCH /inventory/{productId}/stock`
- `GET /inventory/{productId}/movements`

UX:

- mostrar `physicalStock`, `reservedStock`, `availableStock`
- tabla de movimientos

### Ofertas

Endpoints:

- `GET /products/me/{productId}/offers`
- `POST /products/me/{productId}/offers`
- `DELETE /products/me/{productId}/offers/{offerId}`

UX:

- crear promoción por producto
- listar vigentes e históricas

### Analytics seller

Endpoint:

- `GET /analytics/seller`

Claude puede construir dashboard con:

- métricas resumen
- evolución mensual
- top productos

## 9.13 Área admin

### Sellers

Endpoints:

- `GET /admin/sellers`
- `PATCH /admin/sellers/{id}/approve`
- `PATCH /admin/sellers/{id}/suspend`

UI:

- tabla por estado
- aprobar / suspender

### Moderación de reseñas

Endpoints:

- `GET /admin/reviews`
- `PATCH /admin/reviews/{id}/moderate`

UI:

- cola de pendientes
- aprobar o rechazar con motivo

### Facturas admin

Endpoints:

- `GET /admin/invoices`
- `GET /admin/invoices/{id}`
- `PATCH /admin/invoices/{id}/cancel`

### Categorías admin

Endpoints:

- `POST /categories`
- `PUT /categories/{id}`
- `DELETE /categories/{id}`

### Analytics admin

Endpoint:

- `GET /analytics/admin`

## 10. DTOs útiles para modelar en frontend

Claude debería crear interfaces/types para estos contratos:

- `ApiResponse<T>`
- `PageResponse<T>`
- `TokenResponse`
- `UserResponse`
- `AddressResponse`
- `ProductSummaryResponse`
- `ProductResponse`
- `CartResponse`
- `CheckoutResponse`
- `PaymentResponse`
- `OrderSummaryResponse`
- `OrderResponse`
- `InvoiceResponse`
- `ReviewResponse`
- `WishlistResponse`
- `SellerResponse`
- `PublicSellerResponse`

Especialmente útiles:

### ProductSummaryResponse

Trae lo necesario para cards:

- `id`
- `sellerId`
- `categoryId`
- `name`
- `slug`
- `brand`
- `basePrice`
- `finalPrice`
- `status`
- `primaryImageUrl`

### ProductResponse

Trae detalle completo:

- nombre
- descripción
- marca
- SKU
- precio base
- IVA
- precio final
- estado
- imágenes

### CartResponse

- `id`
- `items`
- `total`
- `totalItems`
- `hasPriceChanges`

### CheckoutResponse

- `id`
- `status`
- `items`
- `subtotal`
- `shippingCost`
- `total`
- `paymentMethod`
- `expiresAt`
- `minutesLeft`

### PaymentResponse

- `id`
- `checkoutId`
- `mpPaymentId`
- `mpPreferenceId`
- `checkoutUrl`
- `status`
- `paymentMethod`
- `amount`

## 11. Estados y enums que el frontend debe contemplar

### Roles

- `CLIENT`
- `SELLER`
- `ADMIN`

### Estado producto

- `DRAFT`
- `ACTIVE`
- `PAUSED`
- `DELETED`

### Estado checkout

- `IN_PROGRESS`
- `COMPLETED`
- `EXPIRED`
- `CANCELLED`

### Estado pedido

- `PENDING`
- `PAYMENT_PENDING`
- `PAYMENT_APPROVED`
- `PAYMENT_REJECTED`
- `PROCESSING`
- `PARTIALLY_SHIPPED`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`
- `REFUNDED`

### Estado grupo de orden

- `PENDING`
- `CONFIRMED`
- `PREPARING`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`

### Estado reseña

- `PENDING`
- `APPROVED`
- `REJECTED`

## 12. Notas UX importantes

- El catálogo público solo debe mostrar productos `ACTIVE`.
- El seller debe ver sus productos en todos sus estados.
- El carrito puede detectar cambios de precio.
- El checkout expira; hay que comunicarlo claramente.
- El pago no se confirma en el frontend directamente: se confirma vía webhook.
- El frontend debe saber que el usuario puede volver de Mercado Pago antes de que la orden ya esté visible.
- La cuenta seller puede existir en estado `PENDING`, así que no todo usuario que inició ese proceso tendrá aún experiencia seller completa.

## 13. Qué debería construir Claude primero

Orden recomendado:

1. shell de app + router + layouts
2. auth store + interceptor + guards
3. catálogo público
4. detalle de producto
5. carrito
6. direcciones
7. checkout
8. órdenes / facturas
9. área seller
10. área admin

## 14. Resumen corto

Si Claude quiere una lectura rápida:

- Es un marketplace multi-seller.
- El frontend actual está prácticamente vacío.
- Debe construir una app Angular 21 con rutas públicas, cuenta, seller y admin.
- El backend ya resuelve auth, catálogo, carrito, checkout, Mercado Pago, órdenes, facturas, reseñas, wishlists y analytics.
- El flujo crítico es:
  - catálogo -> carrito -> checkout -> pago MP -> webhook -> orden -> factura
- El frontend debe manejar JWT, refresh token, role guards y estados de checkout/pago con cuidado.
