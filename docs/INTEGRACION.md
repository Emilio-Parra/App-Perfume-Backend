# Integración Frontend ↔ Backend — Perfumes Store

Este documento explica cómo se comunican el **frontend (React + Vite)** y el **backend (Spring Boot + MySQL)**: arquitectura, endpoints que usa cada pantalla, manejo del token JWT, flujos principales, errores y evidencias de las pruebas.

| Proyecto | Repositorio | URL local |
|---|---|---|
| Frontend | https://github.com/Emilio-Parra/App-Perfume-eva2 | http://localhost:5173 |
| Backend | https://github.com/Emilio-Parra/App-Perfume-Backend | http://localhost:8080/api/v1 |

---

## 1. Arquitectura

```mermaid
flowchart LR
    U[Navegador] --> F["Frontend React<br/>Vite :5173"]
    F -- "fetch /api/...<br/>+ Authorization: Bearer" --> P["Proxy de Vite<br/>/api → /api/v1"]
    P --> B["Backend Spring Boot :8080"]
    subgraph Backend
      B --> SEC["JwtAuthenticationFilter<br/>+ SecurityConfig"]
      SEC --> C[Controladores REST]
      C --> S["Servicios<br/>(lógica de negocio)"]
      S --> R[Repositorios JPA]
    end
    R --> DB[(MySQL<br/>tienda_perfumes)]
```

| Capa | Tecnología | Responsabilidad |
|---|---|---|
| Presentación | React 19, React Bootstrap, Vite | Pantallas de la tienda y del panel admin/vendedor |
| Servicios del front | `src/service/*.js` | Llamadas `fetch` a la API |
| Autenticación del front | `src/service/authFetch.js` | Agrega el token JWT a cada llamada y detecta la sesión vencida |
| API | Spring Boot 4, Spring Web MVC | Endpoints REST versionados (`/api/v1`) |
| Seguridad | Spring Security + JJWT | Validación del token y permisos por rol |
| Negocio | Servicios Java | Totales, stock, pagos, validaciones y reglas |
| Datos | Spring Data JPA + MySQL 8 | Persistencia |

---

## 2. Comunicación REST

### Proxy de Vite
El frontend llama a rutas relativas (`/api/productos`). En desarrollo, Vite las reenvía al backend y agrega la versión:

```js
// vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    rewrite: (path) => path.replace(/^\/api(?!\/v1)/, '/api/v1'),
  },
},
```

| El front llama a | Vite lo envía a |
|---|---|
| `/api/productos` | `http://localhost:8080/api/v1/productos` |
| `/api/pedidos/5/pago` | `http://localhost:8080/api/v1/pedidos/5/pago` |
| `/api/v1/productos/imagen/x.png` | sin cambios (ya trae la versión) |

Como el navegador ve todo en el mismo origen (`localhost:5173`), no hay problemas de CORS. Igual, el backend tiene CORS configurado para `http://localhost:5173`, por si el frontend llama directo al puerto 8080.

### Envío automático del token (`authFetch.js`)
Al iniciar, `main.jsx` ejecuta `instalarAuthFetch()`, que envuelve `window.fetch`. Para **toda** llamada a `/api/...`:

1. Lee el token guardado en `localStorage['usuario_actual'].token`.
2. Agrega el encabezado `Authorization: Bearer <token>`.
3. Si la respuesta es **401** y había un token, borra la sesión y emite el evento `sesion-expirada`. `App.jsx` lo escucha, avisa al usuario y lo lleva al login.

Así no hubo que modificar cada `fetch` de los más de 20 que tiene el frontend.

```js
// src/main.jsx
import { instalarAuthFetch } from './service/authFetch'
instalarAuthFetch()
```

---

## 3. Endpoints que usa cada parte del frontend

| Archivo del frontend | Función / pantalla | Método y endpoint | Rol |
|---|---|---|---|
| `service/productosService.js` | Catálogo, detalle, carrito | `GET /productos` | Público |
| `service/productosService.js` | Admin: crear producto | `POST /productos` | ADMIN |
| `service/productosService.js` | Admin: editar producto | `PUT /productos/{id}` | ADMIN |
| `service/productosService.js` | Admin: eliminar producto | `DELETE /productos/{id}` | ADMIN |
| `components/admin/ProductoForm.jsx` | Subir foto del perfume | `POST /productos/imagen` | ADMIN |
| `service/categoriasService.js` | Filtros y panel de categorías | `GET /categorias` | Público |
| `service/categoriasService.js` | Admin: crear, editar y eliminar | `POST/PUT/DELETE /categorias` | ADMIN |
| `components/RegistroPage.jsx` | Registro de cliente | `POST /usuarios/registro` | Público |
| `components/LoginPage.jsx` | Login de cliente o vendedor | `POST /usuarios/login` | Público |
| `components/LoginPage.jsx` | Login de administrador (`admin@duoc.cl`) | `POST /admin/login` y `GET /admin/perfil` | Público / ADMIN |
| `App.jsx` | Cerrar sesión | `POST /usuarios/logout` o `/admin/logout` | Público |
| `service/pedidosService.js` | Checkout: crear la orden | `POST /pedidos` | CLIENTE |
| `service/pedidosService.js` | Pagar la orden | `POST /pedidos/{id}/pago` | CLIENTE |
| `components/admin/AdminPage.jsx` | Órdenes, dashboard, reportes | `GET /pedidos/admin` | ADMIN, VENDEDOR |
| `components/admin/AdminDashboard.jsx` | Total de clientes | `GET /usuarios/total-clientes` | ADMIN |
| `service/usuariosService.js` | Admin: usuarios (clientes y vendedores) | `GET/POST/PUT/DELETE /admin/usuarios` | ADMIN |
| `components/admin/AdminPage.jsx`, `AdminPerfil.jsx` | Perfil del administrador | `GET/PUT /admin/perfil` | ADMIN |

La especificación de cada endpoint está en [`API.md`](API.md).

---

## 4. Flujos principales

### 4.1 Login
```mermaid
sequenceDiagram
    actor U as Usuario
    participant L as LoginPage.jsx
    participant B as Backend
    U->>L: correo + contraseña
    alt correo = admin@duoc.cl
        L->>B: POST /api/admin/login
        B-->>L: 200 {datos, token}
        L->>B: GET /api/admin/perfil (Authorization: Bearer token)
        B-->>L: 200 perfil
    else cliente o vendedor
        L->>B: POST /api/usuarios/login
        B-->>L: 200 {datos, rol, token}
    end
    L->>L: localStorage.usuario_actual = {datos, token}
    L-->>U: admin/vendedor → panel · cliente → tienda
```

### 4.2 Compra (cliente)
```mermaid
sequenceDiagram
    actor C as Cliente
    participant F as Frontend
    participant B as Backend
    participant DB as MySQL
    C->>F: Agrega perfumes al carrito y completa el checkout
    F->>B: POST /api/pedidos {usuarioId, items[id, cantidad], cliente, envio}
    B->>DB: Lee precio, descuento y stock reales
    B->>B: Calcula el total y valida el stock
    B->>DB: Guarda el pedido (pendiente) y su detalle
    B-->>F: 201 {id, codigo, total, estado: pendiente}
    F->>B: POST /api/pedidos/{id}/pago
    B->>B: SimuladorPago.aprobar()
    alt aprobado
        B->>DB: estado = pagado, descuenta stock
        B-->>F: 200 {estado: pagado}
        F-->>C: Compra exitosa + boleta
    else rechazado
        B->>DB: estado = fallido, intentos + 1
        B-->>F: 200 {estado: fallido}
        F-->>C: Pago fallido (puede reintentar)
    end
```

> El frontend solo envía el **ID** y la **cantidad** de cada producto. El precio lo calcula el backend con los datos de MySQL, así nadie puede cambiar el precio desde el navegador.

### 4.3 Panel según el rol
| Rol | Vista inicial | Menú visible | Llamadas a la API |
|---|---|---|---|
| ADMIN | Dashboard | Dashboard, Órdenes, Productos, Categorías, Usuarios, Reportes, Perfil | Todas las del panel |
| VENDEDOR | Órdenes | Órdenes, Productos (solo lectura, con detalle) | `GET /pedidos/admin`, `GET /productos`, `GET /categorias` |
| CLIENTE | — | No entra al panel | — |

El frontend oculta las opciones según `src/utils/roles.js`, pero **la seguridad real está en el backend**: si alguien forzara una vista, la API respondería **403**.

---

## 5. Manejo de errores en la integración

El backend responde todos los errores con el mismo formato:

```json
{ "status": 409, "error": "No hay stock suficiente de Hawas Malibu. Disponibles: 2.", "ruta": "/api/v1/pedidos", "fecha": "..." }
```

Los servicios del frontend leen la propiedad **`error`** y la muestran al usuario:

```js
// src/service/pedidosService.js
if (!respuesta.ok) {
  throw new Error(datos?.error || datos?.message || `Error HTTP ${respuesta.status}`);
}
```

| Código | Qué hace el frontend |
|---|---|
| 400 | Muestra el mensaje del campo inválido en el formulario |
| 401 (con token guardado) | `authFetch.js` cierra la sesión y `App.jsx` lleva al login: *"Tu sesión expiró. Inicia sesión nuevamente."* |
| 401 (login) | *"Correo o contraseña incorrectos."* |
| 403 | Muestra *"No tienes permisos para realizar esta acción."* |
| 404 / 409 | Muestra el mensaje del backend (ej. sin stock, correo repetido, categoría con productos) |

---

## 6. Formato de los datos

Los nombres de los campos JSON son los mismos en el frontend y en el backend:

| Objeto | Campos |
|---|---|
| Producto | `id, marca, modelo, tipo, genero, precio, descuento, stock, volumen, imagenes, descripcion` |
| Categoría | `id, nombre, descripcion` |
| Usuario (sesión) | `id, rut, correo, nombre, apellido, rol, fechaNac, region, comuna, direccion, token` |
| Pedido | `id, codigo, fecha, estado, intentos, total, cliente{...}, envio{...}, usuarioId, items[...]` |

- Montos en **pesos chilenos, como enteros** (`39990`).
- Fechas en formato **ISO 8601** (`2026-11-18T14:20:11`).
- La propiedad `tipo` del producto corresponde al **nombre de la categoría**.

---

## 7. Evidencias de comunicación

### 7.1 Petición real a través del proxy de Vite
Login del cliente y creación de un pedido llamando a `localhost:5173`, igual que el navegador:

```http
POST http://localhost:5173/api/pedidos
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9.eyJjb3JyZW8iOiJhbmFAZHVvYy5jbCIsInN1YiI6IjMi...
Content-Type: application/json

{"usuarioId":3,"items":[{"id":2,"cantidad":1}],
 "cliente":{"nombre":"Ana","apellido":"Paz","correo":"ana@duoc.cl"},
 "envio":{"calle":"Calle 1","region":"RM","comuna":"Maipu"}}
```
```http
HTTP/1.1 201 Created
Content-Type: application/json

{"total":63636,"cantidadProductos":1,"codigo":"ORDER859B1484C6",
 "fecha":"2026-11-21T13:35:17.095","estado":"pendiente","id":4}
```

### 7.2 Llamadas del panel de administración (capturadas en el navegador)
Al iniciar sesión como admin y recorrer el panel, todas las llamadas protegidas salieron **con el token** y respondieron 200:

| Método | Ruta | Token | Respuesta |
|---|---|:-:|:-:|
| POST | `/api/admin/login` | — | 200 |
| GET | `/api/admin/perfil` | ✅ | 200 |
| GET | `/api/admin/usuarios` | ✅ | 200 |
| GET | `/api/pedidos/admin` | ✅ | 200 |
| GET | `/api/usuarios/total-clientes` | ✅ | 200 |

### 7.3 Resultados por rol en el navegador
| Prueba | Resultado |
|---|:-:|
| Admin: el panel carga 27 productos, 3 categorías, órdenes y usuarios | ✅ |
| Vendedor: el panel muestra solo Órdenes y Productos, sin botones de edición; 0 errores de API | ✅ |
| Cliente: compra desde el navegador → `201` con código de orden | ✅ |
| Cliente con token inválido → `401`, se borra la sesión y vuelve al login | ✅ |

Más pruebas en [`PRUEBAS.md`](PRUEBAS.md).

---

## 8. Cómo levantar todo para probar la integración

```powershell
# Terminal 1 — backend
cd App-Perfume-Backend
.\iniciar-backend.ps1          # o: .\mvnw spring-boot:run

# Terminal 2 — frontend
cd App-Perfume-eva2
npm install
npm run dev
```

1. Abrir http://localhost:5173.
2. **Admin:** `admin@duoc.cl` con la clave definida al iniciar el backend → crear un vendedor en Usuarios.
3. **Vendedor:** iniciar sesión → ver órdenes y productos.
4. **Cliente:** registrarse → comprar → ver la boleta.
5. **Swagger:** http://localhost:8080/swagger-ui.html → *Authorize* con el token del login.
