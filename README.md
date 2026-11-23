# Perfumes Store — Backend

Backend de **Perfumes Store**, proyecto de Fullstack 2. Está desarrollado con **Java 21+, Spring Boot, Spring Data JPA / Hibernate y MySQL**. Expone una **API REST versionada como `/api/v1`** para que el frontend en React consulte productos, categorías, usuarios y pedidos, y utilice las funciones de administración.

- **Backend (este repositorio):** https://github.com/Emilio-Parra/App-Perfume-Backend
- **Frontend:** https://github.com/Emilio-Parra/App-Perfume-eva2
- **API local:** http://localhost:8080/api/v1/productos
- **Frontend local:** http://localhost:5173
- **Swagger (documentación interactiva):** http://localhost:8080/swagger-ui.html

### Documentación técnica

| Documento | Contenido |
|---|---|
| [`docs/API.md`](docs/API.md) | Todos los endpoints: método, ruta, rol, cuerpo y respuestas |
| [`docs/MODELO_DATOS.md`](docs/MODELO_DATOS.md) | Tablas, columnas, relaciones y diagrama de la base de datos |
| [`docs/SEGURIDAD.md`](docs/SEGURIDAD.md) | Autenticación JWT, roles y matriz de permisos |
| [`docs/PRUEBAS.md`](docs/PRUEBAS.md) | Pruebas automatizadas, cómo ejecutarlas y resultados |
| [`docs/INTEGRACION.md`](docs/INTEGRACION.md) | Comunicación frontend ↔ backend, flujos y evidencias |

> **Importante:** estas instrucciones están pensadas para los **PC del instituto**, donde MySQL Workbench suele abrir la conexión local `root` sin pedir contraseña. En esta guía usaremos **`Root1234` en las dos claves que solicita `iniciar-backend.ps1`**: la del usuario MySQL `tienda_app` y, durante la primera ejecución, la del administrador de la tienda `admin@duoc.cl`. **No es la contraseña de `root`**. Es una clave de ejemplo conocida públicamente: **no la reutilices fuera de este proyecto académico ni almacenes datos reales con ella**.

## 1. ¿Qué se necesita?

- **Java JDK 21 o superior** (`java -version` para comprobarlo).
- **MySQL Server** instalado y funcionando (habitualmente puerto `3306`).
- **MySQL Workbench**, para configurar la base de datos por primera vez.
- **Git** y **VS Code** (opcionales como editores, pero recomendados).
- Conexión a Internet en la primera ejecución para que Maven descargue dependencias.
- Para usar la tienda visualmente, **Node.js y npm** para el frontend.

**No hace falta instalar Maven manualmente:** el repositorio incluye `mvnw.cmd`, el Maven Wrapper.

## 2. Primera vez: configurar MySQL Workbench en un PC del instituto

### A. Entrar como `root` y crear la base

1. Abrir **MySQL Workbench**.
2. Abrir la conexión local existente, normalmente **`Local instance MySQL80`**, que en los PC del instituto entra directamente con el usuario **`root`**.
3. Abrir una pestaña de consultas SQL (`SQL +`).
4. Copiar **todo** el siguiente código y ejecutarlo con el botón del **rayo**:

```sql
CREATE DATABASE IF NOT EXISTS tienda_perfumes
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'tienda_app'@'localhost'
  IDENTIFIED BY 'Root1234';

-- Deja la clave de este usuario de práctica igual en estos PC.
ALTER USER 'tienda_app'@'localhost'
  IDENTIFIED BY 'Root1234';

GRANT ALL PRIVILEGES ON tienda_perfumes.*
  TO 'tienda_app'@'localhost';
```

5. En el panel **SCHEMAS**, pulsar actualizar y comprobar que aparezca **`tienda_perfumes`**.

**No es necesario crear las tablas ni ejecutar manualmente el archivo de los 27 perfumes.** Spring Boot hará ambas tareas en el primer inicio.

> Si están trabajando en un PC con otra instalación de MySQL y `root` **sí** tiene contraseña, deben ingresar la que corresponda a esa instalación. No es necesario dejar `root` sin contraseña.

### B. Crear la conexión `Tienda Perfumes` en Workbench

1. Volver a la pantalla de inicio de Workbench (icono de la casa).
2. En **MySQL Connections**, pulsar **`+`**.
3. Completar los datos exactamente así:

| Campo | Valor |
|---|---|
| **Connection Name** | `Tienda Perfumes` |
| **Connection Method** | `Standard (TCP/IP)` |
| **Hostname** | `127.0.0.1` |
| **Port** | `3306` |
| **Username** | `tienda_app` |
| **Password** | `Root1234` (ingresarla en **Store in Vault...** o cuando la solicite) |
| **Default Schema** | `tienda_perfumes` |

4. Pulsar **Test Connection**. Debe aparecer **Successfully made the MySQL connection**.
5. Guardar con **OK** y abrir la conexión **Tienda Perfumes**.

De esta forma habrá dos conexiones: la original de `root` para configurar MySQL y **Tienda Perfumes** para trabajar con la base del proyecto.

## 3. Descargar y ejecutar el backend

Abrir **PowerShell** o la terminal de VS Code en una carpeta de trabajo y ejecutar:

```powershell
git clone https://github.com/Emilio-Parra/App-Perfume-Backend.git
cd App-Perfume-Backend
```

Si ya descargaron el repositorio, **no lo clonen otra vez**: abran la carpeta que contiene `pom.xml`, `mvnw.cmd`, `src` e `iniciar-backend.ps1`.

**Forma recomendada de iniciar:**

**Inicio de sesión con Google (también en los PC del instituto):** antes de iniciar Spring Boot, configura el Client ID en **esta misma terminal del BACKEND**. Debes repetir esta línea cada vez que abras una terminal nueva para arrancar el servidor; no hace falta reinstalar Google ni crear otro proyecto en Google Cloud.

```powershell
$env:GOOGLE_CLIENT_ID = "94975653203-jvn7a69uau90scm9c1gu2uk5sh5e3sq2.apps.googleusercontent.com"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

El script solicitará:

1. **`Clave MySQL de tienda_app`:** escribir **`Root1234`**.
2. **`Clave inicial del administrador`:** escribir también **`Root1234` la primera vez**, para crear la cuenta administrativa. En los siguientes inicios, **si ya existe la cuenta en esa misma base de datos**, se puede dejar en blanco y presionar Enter.

**En resumen: en la primera ejecución se escribe `Root1234` en ambas preguntas.** El correo del administrador inicial es **`admin@duoc.cl`**, por lo que el acceso de práctica queda como correo **`admin@duoc.cl`** y contraseña **`Root1234`**. La contraseña no se guarda en el repositorio; el backend almacena su hash BCrypt en MySQL. Por seguridad, utilicen esas credenciales **solo en los PC de práctica y nunca en un despliegue público**.

**Dejar abierta esa terminal** mientras se use la aplicación. Para detener el backend, presionar **Ctrl + C**.

Si funciona, aparecerá un mensaje similar a `Tomcat started on port 8080` y `Started PerfumesApplication`.

**Comprobación:** abrir http://localhost:8080/api/v1/productos en el navegador. Debe mostrar una lista JSON de perfumes. Para explorar el contrato completo, usar **Swagger UI** en http://localhost:8080/swagger-ui.html o el JSON en http://localhost:8080/v3/api-docs.

### Alternativa: iniciar con Maven Wrapper directamente

Si ya configuraron la variable de entorno `DB_PASSWORD` en **esa misma terminal**, también se puede iniciar con:

```powershell
.\mvnw.cmd spring-boot:run
```

Para quienes recién empiezan, es más fácil usar **`iniciar-backend.ps1`**, porque pide la contraseña y configura las variables de ese arranque sin modificar archivos del proyecto.

## 4. ¿Qué hace automáticamente el backend?

En el primer inicio sobre una base de datos nueva:

1. Se conecta a MySQL con el usuario **`tienda_app`**.
2. Hibernate, con `spring.jpa.hibernate.ddl-auto=update`, crea las tablas necesarias: `productos`, `categorias`, `usuarios`, `administradores`, `pedidos` y `detalle_pedidos`.
3. La clase `CatalogoInicialConfig.java` revisa la tabla `productos`. Si está vacía, ejecuta automáticamente `src/main/resources/catalogo_inicial_perfumes.sql` e incorpora **27 perfumes y 3 categorías**.
4. Si se indicó una contraseña inicial, `AdministradorInicialConfig.java` crea la cuenta administrativa `admin@duoc.cl` cuando no existe.

**En los siguientes inicios**, si ya hay productos, el catálogo **no se vuelve a cargar**, no se duplica y no se sobreescriben stock, precios ni pedidos.

> Cada PC usa su **propia MySQL local**. Los cambios que haga un compañero en su computador no aparecen automáticamente en el de otro. El catálogo inicial permite partir con los mismos datos de ejemplo.

## 5. Iniciar también el frontend React

Abrir **una segunda terminal**, manteniendo el backend funcionando en la primera:

**Inicio de sesión con Google (primera vez en cada PC):** en la **raíz del FRONTEND** (`App-Perfume-eva2`, junto a `package.json`), crea un archivo llamado `.env.local` si aún no existe. Si ya existe, **no lo reemplaces**: solo agrega o actualiza esta línea:

```env
VITE_GOOGLE_CLIENT_ID=94975653203-jvn7a69uau90scm9c1gu2uk5sh5e3sq2.apps.googleusercontent.com
```

Hazlo **antes de `npm run dev`**. Reinicia Vite si ya estaba abierto. El archivo `.env.google.example` de GitHub sirve de referencia, pero Vite lee `.env.local`. El origen autorizado de Google Cloud es `http://localhost:5173`.

**Al probar en una base MySQL nueva:** primero registra tu cuenta de Gmail en la tienda usando el formulario normal; luego cierra sesión y usa **Acceder con Google**. La cuenta también debe estar autorizada como usuario de prueba en Google Cloud mientras la aplicación esté en modo de prueba.

```powershell
git clone https://github.com/Emilio-Parra/App-Perfume-eva2.git
cd App-Perfume-eva2
npm install
npm run dev
```

Abrir **http://localhost:5173**.

El frontend conserva por ahora llamadas a `/api/...`. Como el backend responde en **`/api/v1/...`**, el archivo `vite.config.js` del frontend **debe incluir una regla `rewrite`** para traducir esos caminos durante `npm run dev`. Por eso **ambos servidores deben estar funcionando al mismo tiempo**.

```js
rewrite: (path) => path.replace(/^\/api(?!\/v1)/, '/api/v1'),
```

Esta adaptación es para desarrollo. No reemplaza la configuración del servidor en un despliegue de producción.

El frontend envía el token JWT automáticamente en cada llamada a `/api` gracias a `src/service/authFetch.js`. Si el token vence, el frontend cierra la sesión y vuelve a la pantalla de login. El detalle está en [`docs/INTEGRACION.md`](docs/INTEGRACION.md).

Las imágenes de los 27 perfumes originales se sirven desde `public/img/` del frontend; sus rutas se guardan en MySQL. Las imágenes nuevas cargadas en administración se guardan localmente en la carpeta `uploads/` del backend, que está excluida de Git. Si quieren compartir esas nuevas imágenes entre equipos, deben respaldarlas por separado.

## 6. API REST v1, Swagger y seguridad

### Versionado de rutas

La versión actual de la API utiliza el prefijo **`/api/v1`**, centralizado en `src/main/java/com/tienda/perfumes/config/ApiRutas.java` como `ApiRutas.V1`.

| Área | Ruta base |
|---|---|
| Productos | `/api/v1/productos` |
| Categorías | `/api/v1/categorias` |
| Pedidos | `/api/v1/pedidos` |
| Usuarios | `/api/v1/usuarios` |
| Administrador | `/api/v1/admin` |
| Gestión de clientes | `/api/v1/admin/usuarios` |
| Imágenes | `/api/v1/productos/imagen` |

Se versiona para que en el futuro se pueda incorporar `/api/v2` sin cambiar el comportamiento que esperan los clientes de la versión v1.

### Swagger / OpenAPI

El backend incorpora `springdoc-openapi-starter-webmvc-ui` (3.2.0) y `OpenApiConfig.java`. Con Spring Boot iniciado:

- **Interfaz Swagger:** http://localhost:8080/swagger-ui.html
- **Definición OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **Ejemplo de endpoint público:** http://localhost:8080/api/v1/productos

Los controladores incluyen etiquetas `@Tag` y descripciones `@Operation` para facilitar la exploración de los endpoints. Swagger permite consultar y probar operaciones, pero las operaciones protegidas seguirán necesitando una sesión con el rol adecuado.

### Autenticación con JWT y roles

El login devuelve un **token JWT** firmado (HS384, válido por 120 minutos). El frontend lo guarda y lo envía en cada petición con el encabezado:

```
Authorization: Bearer <token>
```

El backend **no guarda sesiones** (`SessionCreationPolicy.STATELESS`): `JwtAuthenticationFilter` valida la firma y el vencimiento del token en cada solicitud y registra el rol del usuario en Spring Security.

| Rol | Cómo inicia sesión | Qué puede hacer |
|---|---|---|
| **ADMIN** | `POST /api/v1/admin/login` (tabla `administradores`) | Acceso total |
| **VENDEDOR** | `POST /api/v1/usuarios/login` (tabla `usuarios`, rol `vendedor`) | Ver productos (listado y detalle) y órdenes (listado y detalle) |
| **CLIENTE** | `POST /api/v1/usuarios/login` (tabla `usuarios`, rol `cliente`) | Solo la tienda: comprar y pagar sus propios pedidos |

- Sin token, una ruta protegida responde **401**. Con token, pero sin el rol necesario, responde **403**.
- Los vendedores los crea el administrador desde el panel (**Usuarios → Nuevo usuario → Rol: Vendedor**).
- En Swagger se puede probar con token: hacer login, copiar el `token` de la respuesta y pegarlo en el botón **Authorize**.

El detalle completo está en [`docs/SEGURIDAD.md`](docs/SEGURIDAD.md).

> **Aviso:** la clave del token tiene un valor de desarrollo en `application.properties`. En un despliegue real se debe definir con la variable de entorno `JWT_SECRET` y no subirla al repositorio.

### Pruebas automatizadas

El proyecto incluye **64 pruebas** (JUnit 6, Mockito y MockMvc) que usan una base **H2 en memoria**, por lo que **no necesitan MySQL encendido**:

```bash
.\mvnw test
```

Al terminar se genera el reporte de cobertura en `target/site/jacoco/index.html`. El detalle está en [`docs/PRUEBAS.md`](docs/PRUEBAS.md).

## 7. Errores frecuentes y solución

| Problema | Qué revisar |
|---|---|
| **Access denied for user `tienda_app`** | Comprobar usuario y contraseña `Root1234`; volver a probar la conexión **Tienda Perfumes** en Workbench. |
| **Unknown database `tienda_perfumes`** | Falta ejecutar el bloque SQL del paso 2A desde la conexión de `root`. |
| **Communications link failure / Connection refused** | Verificar que MySQL Server esté iniciado y que use el puerto `3306`. |
| **Port 8080 already in use** | Ya hay otro backend en ejecución. Detenerlo con **Ctrl + C** antes de iniciar uno nuevo. |
| **La tienda abre, pero no aparecen perfumes** | Revisar `http://localhost:8080/api/v1/productos`, la conexión a MySQL y los mensajes de carga inicial. |
| **404 en `/api/productos`** | Esa es la ruta antigua. Probar `/api/v1/productos`. En el frontend, verificar el `rewrite` del proxy de Vite. |
| **No abre Swagger** | Comprobar que el backend esté iniciado y probar `/swagger-ui.html` y `/v3/api-docs`. |
| **Error 401 al guardar o ver datos del panel** | El token no se envió o venció (dura 120 minutos). Cerrar sesión y volver a entrar. |
| **Error 403** | El token es válido, pero el rol no tiene permiso (por ejemplo, un vendedor intentando editar un producto). |
| **No aparecen las fotos** | Comprobar que las imágenes existan en `public/img/` del frontend y que el frontend esté iniciado. |
| **`mvnw test` falla** | Las pruebas no usan MySQL. Revisar el mensaje del test que falló en `target/surefire-reports/`. |
| **`Administrador inicial: credenciales no configuradas`** | Si es la primera vez en esa base, detener el backend y volver a iniciarlo indicando la contraseña inicial del admin. Si el administrador ya existe, se puede ignorar. |
| **`mvnw.cmd` no funciona / error de Java** | Verificar `java -version` y que se está ejecutando el comando dentro de la carpeta donde está `pom.xml`. |

## 8. Organización básica del proyecto

```text
App-Perfume-Backend/
├── docs/                     # Documentación técnica (API, modelo, seguridad, pruebas, integración)
├── src/
│   ├── main/
│   │   ├── java/com/tienda/perfumes/
│   │   │   ├── config/       # API v1, OpenAPI, Spring Security, CORS y carga inicial
│   │   │   ├── controller/   # Endpoints REST
│   │   │   ├── dto/          # Datos de entrada con sus validaciones (@Valid)
│   │   │   ├── exception/    # Manejo global de errores (400, 401, 403, 404, 409, 500)
│   │   │   ├── model/        # Entidades de MySQL
│   │   │   ├── repository/   # Acceso a base de datos con JPA
│   │   │   ├── security/     # Generación y validación de tokens JWT
│   │   │   ├── service/      # Lógica del negocio
│   │   │   └── validation/   # Validación propia del RUN (@RutValido)
│   │   └── resources/
│   │       ├── application.properties
│   │       └── catalogo_inicial_perfumes.sql
│   └── test/                 # 64 pruebas automatizadas (H2 en memoria)
├── iniciar-backend.ps1       # Inicio guiado en Windows
├── mvnw.cmd                  # Maven Wrapper para Windows
├── pom.xml                   # Dependencias de Spring Boot
└── README.md
```

**Notas de seguridad:** `Root1234` se documenta aquí únicamente para simplificar las prácticas en los computadores del instituto. Esta base y contraseña son **solo para desarrollo académico**. No subir contraseñas personales, respaldos con datos reales, variables de entorno ni carpetas `uploads/` a un repositorio público.

## 9. Consultas útiles en MySQL Workbench

Abrir la conexión **Tienda Perfumes**, crear una pestaña SQL y ejecutar primero:

```sql
USE tienda_perfumes;
```

Después se pueden ejecutar las siguientes consultas de manera individual con el botón del rayo. Son consultas de **solo lectura**: muestran datos, pero no borran ni modifican información.

### Ver todos los perfumes

```sql
SELECT id, marca, modelo, tipo, genero, precio, descuento, stock
FROM productos
ORDER BY marca, modelo;
```

### Ver únicamente los perfumes de nicho

```sql
SELECT id, marca, modelo, precio, stock
FROM productos
WHERE tipo = 'nicho'
ORDER BY marca, modelo;
```

### Ver perfumes árabes o de diseñador

```sql
SELECT marca, modelo, tipo, precio, stock
FROM productos
WHERE tipo IN ('arabe', 'diseñador')
ORDER BY tipo, marca;
```

### Contar productos por categoría

```sql
SELECT tipo AS categoria, COUNT(*) AS cantidad
FROM productos
GROUP BY tipo
ORDER BY tipo;
```

### Ver las categorías registradas

```sql
SELECT id, nombre, descripcion
FROM categorias
ORDER BY nombre;
```

### Consultar stock bajo (menos de 5 unidades)

```sql
SELECT marca, modelo, stock
FROM productos
WHERE stock < 5
ORDER BY stock ASC, marca;
```

### Ver las compras o pedidos realizados

```sql
SELECT id, codigo, fecha, estado, total, usuario_id
FROM pedidos
ORDER BY fecha DESC;
```

> Si todavía no se ha realizado ninguna compra en ese computador, la consulta estará vacía. Los pedidos se guardan en la base local de cada PC.

### Ver qué productos contiene cada pedido

```sql
SELECT p.codigo AS pedido, d.marca, d.modelo,
       d.cantidad, d.precio,
       (d.cantidad * d.precio) AS subtotal
FROM detalle_pedidos AS d
JOIN pedidos AS p ON p.id = d.pedido_id
ORDER BY p.fecha DESC, p.codigo;
```

### Ver cantidad de pedidos y montos por estado

```sql
SELECT estado,
       COUNT(*) AS cantidad_pedidos,
       COALESCE(SUM(total), 0) AS monto_total
FROM pedidos
GROUP BY estado
ORDER BY cantidad_pedidos DESC;
```

> El monto suma pedidos registrados de cada estado; **no equivale necesariamente a ventas pagadas o confirmadas**.

### Ver los clientes registrados

```sql
SELECT id, nombre, apellido, correo, rol
FROM usuarios
ORDER BY id;
```

### Comprobar que existe el administrador

```sql
SELECT id, correo, nombre, apellido
FROM administradores;
```

> Por seguridad, no es necesario consultar ni copiar el campo `password_hash`: las contraseñas se validan desde el backend.



## 10. Trabajo en equipo y comprobación de la versión

Antes de iniciar en otro PC, desde **cada repositorio** ejecuten `git pull origin main` para traer los cambios publicados. En el backend comprueben que existen `ApiRutas.java`, `OpenApiConfig.java` y que `SecurityConfig.java` contiene reglas para `/api/v1`. En el frontend, comprueben que `vite.config.js` contiene la línea `rewrite` indicada arriba.

Si el backend funciona en `/api/v1/productos`, pero el frontend aún consulta `/api/productos` y **no tiene el `rewrite`**, puede haber errores 404. Los cambios deben estar subidos a **ambos repositorios** antes de pedirle a otro compañero que los clone.

```powershell
# Desde la carpeta correspondiente a cada repositorio:
git pull origin main
```

Para documentar los cambios del backend, utilicen la terminal en `App-Perfume-Backend`:

```powershell
git status
git add README.md
git commit -m "docs: explicar API v1 y Swagger"
git push origin main
```

> La cuenta `admin@duoc.cl` y su contraseña son locales. En un primer arranque de práctica se puede usar `Root1234` en las dos preguntas de `iniciar-backend.ps1`, pero eso no reemplaza la contraseña de un administrador existente. Un valor anterior como `admin123` no queda fijado en el código por esta documentación.
