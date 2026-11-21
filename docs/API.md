# Documentación de la API REST — Perfumes Store

API del backend de **Perfumes Store** (DSY1104 – Evaluación Parcial 3).

| Dato | Valor |
|---|---|
| URL base | `http://localhost:8080/api/v1` |
| Formato | JSON (`Content-Type: application/json`), salvo la subida de imágenes (`multipart/form-data`) |
| Autenticación | Token JWT en el encabezado `Authorization: Bearer <token>` |
| Documentación interactiva | Swagger UI: http://localhost:8080/swagger-ui.html |
| Especificación OpenAPI | http://localhost:8080/v3/api-docs |
| Total de endpoints | 28 |

---

## Índice

1. [Convenciones](#1-convenciones)
2. [Autenticación](#2-autenticación)
3. [Productos](#3-productos)
4. [Imágenes de productos](#4-imágenes-de-productos)
5. [Categorías](#5-categorías)
6. [Pedidos (boletas)](#6-pedidos-boletas)
7. [Usuarios (clientes)](#7-usuarios-clientes)
8. [Administrador](#8-administrador)
9. [Gestión de usuarios (panel admin)](#9-gestión-de-usuarios-panel-admin)
10. [Errores](#10-errores)
11. [Resumen de permisos](#11-resumen-de-permisos)

---

## 1. Convenciones

### Versionado
Todas las rutas comienzan con **`/api/v1`**. El prefijo está centralizado en `config/ApiRutas.java` (`ApiRutas.V1`). Si en el futuro hay cambios incompatibles, se publicará `/api/v2` sin romper a los clientes de la v1.

### Columna "Acceso"
| Valor | Significado |
|---|---|
| 🌐 Público | No necesita token |
| 🔑 ADMIN | Token de administrador |
| 🔑 VENDEDOR | Token de vendedor |
| 🔑 CLIENTE | Token de cliente |

### Códigos de respuesta
| Código | Cuándo |
|---|---|
| `200 OK` | Consulta o actualización correcta |
| `201 Created` | Registro creado |
| `204 No Content` | Registro eliminado |
| `400 Bad Request` | Datos inválidos (validación) o regla de negocio no cumplida |
| `401 Unauthorized` | Falta el token, está vencido o fue alterado; o credenciales incorrectas |
| `403 Forbidden` | El token es válido, pero el rol no tiene permiso |
| `404 Not Found` | El registro no existe |
| `409 Conflict` | Choca con datos existentes: correo/RUN repetido, sin stock, pedido ya pagado, etc. |
| `500 Internal Server Error` | Error inesperado (el detalle queda en la consola del servidor) |

---

## 2. Autenticación

El login devuelve los datos del usuario y un **token JWT** válido por **120 minutos**. Ese token se envía en las siguientes peticiones:

```http
GET /api/v1/pedidos/admin
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOi...
```

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/usuarios/login` | 🌐 Público | Login de **cliente o vendedor** |
| `POST` | `/admin/login` | 🌐 Público | Login de **administrador** |
| `POST` | `/usuarios/logout` | 🌐 Público | Confirma el cierre de sesión (el frontend borra el token) |
| `POST` | `/admin/logout` | 🌐 Público | Igual, para el administrador |

### `POST /usuarios/login`

**Cuerpo:**
```json
{ "correo": "ana@duoc.cl", "pass": "abcd" }
```

**Respuesta `200`:**
```json
{
  "id": 3,
  "rut": "123456785",
  "correo": "ana@duoc.cl",
  "nombre": "Ana",
  "apellido": "Paz",
  "rol": "cliente",
  "fechaNac": null,
  "region": "RM",
  "comuna": "Maipu",
  "direccion": "Calle 1",
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJjb3JyZ...",
  "tipoToken": "Bearer",
  "expiraEnMinutos": 120
}
```
Si el usuario es vendedor, `rol` viene como `"vendedor"`.

**Respuesta `401`:**
```json
{ "error": "Correo o contraseña incorrectos." }
```

### `POST /admin/login`

**Cuerpo:**
```json
{ "correo": "admin@duoc.cl", "pass": "Root1234" }
```

**Respuesta `200`:**
```json
{
  "id": 1,
  "correo": "admin@duoc.cl",
  "nombre": "Administrador",
  "apellido": "Perfumes Store",
  "rol": "admin",
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiO...",
  "tipoToken": "Bearer",
  "expiraEnMinutos": 120
}
```

---

## 3. Productos

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/productos` | 🌐 Público | Listar todos los productos |
| `GET` | `/productos/{id}` | 🌐 Público | Detalle de un producto |
| `POST` | `/productos` | 🔑 ADMIN | Crear un producto |
| `PUT` | `/productos/{id}` | 🔑 ADMIN | Actualizar un producto |
| `DELETE` | `/productos/{id}` | 🔑 ADMIN | Eliminar un producto |

### `GET /productos/{id}` — respuesta `200`
```json
{
  "id": 1,
  "marca": "Hawas",
  "modelo": "Malibu",
  "tipo": "arabe",
  "genero": "hombre",
  "precio": 39990,
  "descuento": 15,
  "stock": 0,
  "volumen": "100 ml",
  "imagenes": "/img/arabe/images.jpg",
  "descripcion": "Fragancia aromática y frutal de la línea Hawas de Rasasi, lanzada en 2025..."
}
```

### `POST /productos` y `PUT /productos/{id}`

**Cuerpo (`ProductoRequest`):**
```json
{
  "marca": "Lattafa",
  "modelo": "Eclaire",
  "tipo": "arabe",
  "genero": "mujer",
  "precio": 29990,
  "descuento": 10,
  "stock": 15,
  "volumen": "100 ml",
  "imagenes": "/api/v1/productos/imagen/2265c168-4727-4378-9591-97cea6de90f9.png",
  "descripcion": "Fragancia gourmand con caramelo, leche y vainilla."
}
```

| Campo | Regla |
|---|---|
| `marca` | Obligatorio, máx. 100 caracteres |
| `modelo` | Obligatorio, máx. 150 caracteres |
| `tipo` | Obligatorio: nombre de la categoría |
| `genero` | `hombre`, `mujer` o `unisex` (si se omite: `unisex`) |
| `precio` | Obligatorio, entero mayor a 0 |
| `descuento` | Entero entre 0 y 90 (si se omite: 0) |
| `stock` | Obligatorio, entero mayor o igual a 0 |
| `volumen` | Obligatorio, máx. 50 caracteres (ej. `100 ml`) |
| `imagenes` | Obligatorio: ruta de la imagen |
| `descripcion` | Obligatoria, mínimo 20 caracteres |

Respuestas: `201` (creado) o `200` (actualizado) con el producto; `400` si un campo no cumple; `404` si el ID no existe.

### `DELETE /productos/{id}`
Respuesta `204`. Las boletas ya emitidas **no se pierden**, porque cada detalle de pedido guarda una copia de la marca, el modelo y el precio.

---

## 4. Imágenes de productos

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/productos/imagen` | 🔑 ADMIN | Subir una imagen (JPG, PNG o WebP, máx. 5 MB) |
| `GET` | `/productos/imagen/{nombre}` | 🌐 Público | Descargar una imagen subida |

### `POST /productos/imagen`
- Tipo: `multipart/form-data`, campo **`archivo`**.
- El servidor revisa la **firma real del archivo** (no solo la extensión) y le asigna un nombre aleatorio (UUID).

**Respuesta `201`:**
```json
{ "url": "/api/v1/productos/imagen/2265c168-4727-4378-9591-97cea6de90f9.png" }
```
Esa `url` se guarda en el campo `imagenes` del producto.

---

## 5. Categorías

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/categorias` | 🌐 Público | Listar categorías |
| `GET` | `/categorias/{id}` | 🌐 Público | Obtener una categoría |
| `POST` | `/categorias` | 🔑 ADMIN | Crear una categoría |
| `PUT` | `/categorias/{id}` | 🔑 ADMIN | Actualizar una categoría |
| `DELETE` | `/categorias/{id}` | 🔑 ADMIN | Eliminar una categoría |

**Cuerpo (`CategoriaRequest`):**
```json
{ "nombre": "nicho", "descripcion": "Perfumería de nicho y ediciones limitadas" }
```

**Respuesta `GET /categorias`:**
```json
[
  { "id": 1, "nombre": "arabe", "descripcion": "Fragancias árabes, intensas y duraderas" },
  { "id": 2, "nombre": "nicho", "descripcion": "Perfumería de nicho y ediciones limitadas" }
]
```

**Reglas de negocio:**
- No se permiten nombres repetidos (sin importar mayúsculas) → `409`.
- No se puede eliminar una categoría que tiene productos → `409`:
  ```json
  { "status": 409, "error": "No se puede eliminar \"arabe\": tiene 9 producto(s) asociado(s).", "ruta": "/api/v1/categorias/1", "fecha": "..." }
  ```
- Si se renombra una categoría, sus productos se actualizan automáticamente.

---

## 6. Pedidos (boletas)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/pedidos` | 🔑 CLIENTE | Crear un pedido (estado `pendiente`) |
| `POST` | `/pedidos/{id}/pago` | 🔑 CLIENTE | Simular el pago de un pedido propio |
| `GET` | `/pedidos/admin` | 🔑 ADMIN, VENDEDOR | Listar todos los pedidos con su detalle |
| `GET` | `/pedidos/{id}` | 🔑 ADMIN, VENDEDOR | Detalle de un pedido |

### `POST /pedidos`

**Cuerpo (`CrearPedidoRequest`):**
```json
{
  "usuarioId": 3,
  "items": [
    { "id": 1, "cantidad": 2 }
  ],
  "cliente": { "nombre": "Ana", "apellido": "Paz", "correo": "ana@duoc.cl" },
  "envio": {
    "calle": "Calle 1",
    "departamento": "12B",
    "region": "RM",
    "comuna": "Maipu",
    "indicaciones": "Dejar en conserjería"
  }
}
```

**Respuesta `201`:**
```json
{
  "id": 3,
  "codigo": "ORDER784A3B4071",
  "estado": "pendiente",
  "fecha": "2026-11-18T14:20:11.547988649",
  "total": 67984,
  "cantidadProductos": 1
}
```

**Reglas de negocio:**
- **El precio no lo envía el navegador:** el backend lo toma de MySQL y aplica el descuento. Por ejemplo, $39.990 con 15% de descuento = $33.992 × 2 = **$67.984**.
- Se valida que haya stock suficiente → `409` si no:
  ```json
  { "status": 409, "error": "No hay stock suficiente de Hawas Malibu. Disponibles: 2.", ... }
  ```
- `usuarioId` debe coincidir con el usuario del token → `403` si no: *"No puedes crear pedidos para otro usuario."*
- Crear el pedido **no** descuenta stock: se descuenta al aprobarse el pago.

### `POST /pedidos/{id}/pago`
Simula una pasarela de pago (`SimuladorPago`, 70% de aprobación, configurable en `app.pago.probabilidad-exito`).

**Respuesta `200`:**
```json
{ "id": 3, "codigo": "ORDER784A3B4071", "estado": "pagado", "intentos": 1, "total": 67984 }
```
- Pago aprobado → `estado: "pagado"` y se descuenta el stock.
- Pago rechazado → `estado: "fallido"`; se puede reintentar (`intentos` aumenta).
- Pagar un pedido ya pagado → `409`. Pagar el pedido de otro cliente → `403`.

### `GET /pedidos/{id}` — respuesta `200`
```json
{
  "id": 3,
  "codigo": "ORDER784A3B4071",
  "fecha": "2026-11-18T14:20:11",
  "estado": "pagado",
  "intentos": 1,
  "total": 67984,
  "cliente": { "nombre": "Ana", "apellido": "Paz", "correo": "ana@duoc.cl" },
  "envio": { "calle": "Calle 1", "departamento": null, "region": "RM", "comuna": "Maipu", "indicaciones": null },
  "usuarioId": 3,
  "items": [
    { "id": 1, "marca": "Hawas", "modelo": "Malibu", "tipo": "arabe", "imagenes": "/img/arabe/images.jpg", "precio": 33992, "cantidad": 2 }
  ]
}
```
`GET /pedidos/admin` devuelve una lista de objetos con este mismo formato.

---

## 7. Usuarios (clientes)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/usuarios/registro` | 🌐 Público | Registro de un cliente nuevo |
| `GET` | `/usuarios/total-clientes` | 🔑 ADMIN | Cantidad de clientes registrados |

### `POST /usuarios/registro`

**Cuerpo (`RegistroRequest`):**
```json
{
  "rut": "123456785",
  "correo": "ana@duoc.cl",
  "nombre": "Ana",
  "apellido": "Paz",
  "pass": "abcd",
  "fechaNac": "2000-05-14",
  "region": "Región Metropolitana",
  "comuna": "Maipú",
  "direccion": "Calle 1"
}
```

| Campo | Regla |
|---|---|
| `rut` | Obligatorio, **RUN válido** sin puntos ni guion (algoritmo módulo 11, `@RutValido`) |
| `correo` | Obligatorio, máx. 100, solo `@duoc.cl`, `@profesor.duoc.cl` o `@gmail.com` |
| `nombre` | Obligatorio, máx. 50 |
| `apellido` | Obligatorio, máx. 100 |
| `pass` | Obligatoria, entre 4 y 10 caracteres (se guarda cifrada con **BCrypt**) |
| `direccion` | Obligatoria, máx. 300 |

Respuestas: `201` con el usuario creado (sin la contraseña); `400` si un campo no cumple; `409` si el correo o el RUN ya existen.

### `GET /usuarios/total-clientes` — respuesta `200`
```json
{ "totalClientes": 1 }
```

---

## 8. Administrador

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/admin/perfil` | 🔑 ADMIN | Perfil del administrador autenticado |
| `PUT` | `/admin/perfil` | 🔑 ADMIN | Cambiar nombre, apellido o contraseña |

**Cuerpo de `PUT /admin/perfil`:**
```json
{ "nombre": "Emilio", "apellido": "Parra", "pass": "nueva123" }
```
`pass` es opcional: si se omite o va vacía, se mantiene la contraseña actual.

La cuenta inicial (`admin@duoc.cl`) la crea el backend al arrancar, con las variables `ADMIN_EMAIL` y `ADMIN_PASSWORD`. **No existe registro público de administradores.**

---

## 9. Gestión de usuarios (panel admin)

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/admin/usuarios` | 🔑 ADMIN | Listar clientes y vendedores |
| `POST` | `/admin/usuarios` | 🔑 ADMIN | Crear un cliente o un vendedor |
| `PUT` | `/admin/usuarios/{correo}` | 🔑 ADMIN | Editar un cliente o vendedor |
| `DELETE` | `/admin/usuarios/{correo}` | 🔑 ADMIN | Eliminar (solo si no tiene pedidos) |

**Cuerpo (`UsuarioAdminRequest`):**
```json
{
  "rut": "111111111",
  "correo": "vendedor@duoc.cl",
  "nombre": "Vale",
  "apellido": "Rojas",
  "pass": "vend123",
  "rol": "vendedor",
  "direccion": "Local 1"
}
```
- `rol` acepta solo **`cliente`** o **`vendedor`** (si se omite: `cliente`). Intentar asignar `admin` → `400`.
- `pass` es obligatoria al crear y opcional al editar.
- Eliminar un usuario con pedidos → `409` (sus boletas deben conservarse).

---

## 10. Errores

Todos los errores tienen el mismo formato, generado por `GlobalExceptionHandler` (y por `SecurityConfig` para 401/403):

```json
{
  "status": 400,
  "error": "El precio debe ser mayor a 0.",
  "errores": { "precio": "El precio debe ser mayor a 0." },
  "ruta": "/api/v1/productos",
  "fecha": "2026-11-18T13:53:20.509"
}
```

| Campo | Descripción |
|---|---|
| `status` | Código HTTP |
| `error` | Mensaje listo para mostrar al usuario |
| `errores` | Solo en errores de validación: un mensaje por cada campo inválido |
| `ruta` | Ruta que se llamó |
| `fecha` | Momento del error |

### Ejemplos reales

| Situación | Código | `error` |
|---|---|---|
| Producto que no existe | 404 | `No existe el producto con ID 9999.` |
| Crear producto sin token | 401 | `Debes iniciar sesión para acceder a este recurso.` |
| Token vencido o alterado | 401 | `Tu sesión expiró o el token no es válido. Inicia sesión nuevamente.` |
| Cliente entra al panel | 403 | `No tienes permisos para realizar esta acción.` |
| RUN inválido | 400 | `RUN inválido. Escríbelo sin puntos ni guion (ej. 19011022K).` |
| Correo ya registrado | 409 | `El correo ya está registrado.` |
| Categoría repetida | 409 | `Ya existe una categoría llamada "arabe".` |
| JSON mal escrito | 400 | `El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos.` |

---

## 11. Resumen de permisos

| Endpoint | Público | CLIENTE | VENDEDOR | ADMIN |
|---|:-:|:-:|:-:|:-:|
| `GET /productos`, `GET /productos/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /categorias`, `GET /categorias/{id}` | ✅ | ✅ | ✅ | ✅ |
| `GET /productos/imagen/{nombre}` | ✅ | ✅ | ✅ | ✅ |
| `POST /usuarios/registro`, logins y logouts | ✅ | ✅ | ✅ | ✅ |
| `POST /pedidos`, `POST /pedidos/{id}/pago` | ❌ 401 | ✅ | ❌ 403 | ❌ 403 |
| `GET /pedidos/admin`, `GET /pedidos/{id}` | ❌ 401 | ❌ 403 | ✅ | ✅ |
| `POST/PUT/DELETE /productos`, `POST /productos/imagen` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |
| `POST/PUT/DELETE /categorias` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |
| `/admin/perfil`, `/admin/usuarios/**`, `/usuarios/total-clientes` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ |

Esta matriz está verificada con pruebas automatizadas en `SeguridadApiIntegrationTest` (ver [`PRUEBAS.md`](PRUEBAS.md)).
