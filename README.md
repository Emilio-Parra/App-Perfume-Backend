# Perfumes Store — Backend

Backend de **Perfumes Store**, proyecto de Fullstack 2. Está desarrollado con **Java 21+, Spring Boot, Spring Data JPA / Hibernate y MySQL**. Expone una API REST para que el frontend en React consulte productos, categorías, usuarios y pedidos, y utilice las funciones de administración.

- **Backend (este repositorio):** https://github.com/Emilio-Parra/App-Perfume-Backend
- **Frontend:** https://github.com/Emilio-Parra/App-Perfume-eva2
- **API local:** http://localhost:8080/api/productos
- **Frontend local:** http://localhost:5173

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

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\iniciar-backend.ps1
```

El script solicitará:

1. **`Clave MySQL de tienda_app`:** escribir **`Root1234`**.
2. **`Clave inicial del administrador`:** escribir también **`Root1234` la primera vez**, para crear la cuenta administrativa. En los siguientes inicios, **si ya existe la cuenta en esa misma base de datos**, se puede dejar en blanco y presionar Enter.

**En resumen: en la primera ejecución se escribe `Root1234` en ambas preguntas.** El correo del administrador inicial es **`admin@duoc.cl`**, por lo que el acceso de práctica queda como correo **`admin@duoc.cl`** y contraseña **`Root1234`**. La contraseña no se guarda en el repositorio; el backend almacena su hash BCrypt en MySQL. Por seguridad, utilicen esas credenciales **solo en los PC de práctica y nunca en un despliegue público**.

**Dejar abierta esa terminal** mientras se use la aplicación. Para detener el backend, presionar **Ctrl + C**.

Si funciona, aparecerá un mensaje similar a `Tomcat started on port 8080` y `Started PerfumesApplication`.

**Comprobación:** abrir http://localhost:8080/api/productos en el navegador. Debe mostrar una lista JSON de perfumes.

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

```powershell
git clone https://github.com/Emilio-Parra/App-Perfume-eva2.git
cd App-Perfume-eva2\app-perfumes
npm install
npm run dev
```

Abrir **http://localhost:5173**.

El frontend consulta la API mediante `/api`, que Vite dirige al backend en el puerto **8080**. Por eso **los dos servidores deben estar ejecutándose al mismo tiempo**.

Las imágenes de los 27 perfumes originales se sirven desde `app-perfumes/public/img/` del frontend; sus rutas se guardan en MySQL. Las imágenes nuevas cargadas en administración se guardan localmente en la carpeta `uploads/` del backend, que está excluida de Git. Si quieren compartir esas nuevas imágenes entre equipos, deben respaldarlas por separado.

## 6. Errores frecuentes y solución

| Problema | Qué revisar |
|---|---|
| **Access denied for user `tienda_app`** | Comprobar usuario y contraseña `Root1234`; volver a probar la conexión **Tienda Perfumes** en Workbench. |
| **Unknown database `tienda_perfumes`** | Falta ejecutar el bloque SQL del paso 2A desde la conexión de `root`. |
| **Communications link failure / Connection refused** | Verificar que MySQL Server esté iniciado y que use el puerto `3306`. |
| **Port 8080 already in use** | Ya hay otro backend en ejecución. Detenerlo con **Ctrl + C** antes de iniciar uno nuevo. |
| **La tienda abre, pero no aparecen perfumes** | Revisar `http://localhost:8080/api/productos`, la conexión a MySQL y los mensajes de carga inicial. |
| **No aparecen las fotos** | Comprobar que las imágenes existan en `app-perfumes/public/img/` y que el frontend esté iniciado. |
| **`Administrador inicial: credenciales no configuradas`** | Si es la primera vez en esa base, detener el backend y volver a iniciarlo indicando la contraseña inicial del admin. Si el administrador ya existe, se puede ignorar. |
| **`mvnw.cmd` no funciona / error de Java** | Verificar `java -version` y que se está ejecutando el comando dentro de la carpeta donde está `pom.xml`. |

## 7. Organización básica del proyecto

```text
App-Perfume-Backend/
├── src/
│   └── main/
│       ├── java/com/tienda/perfumes/
│       │   ├── config/       # Configuración, seguridad y carga inicial
│       │   ├── controller/   # Endpoints REST
│       │   ├── dto/          # Datos de entrada de las solicitudes
│       │   ├── model/        # Entidades de MySQL
│       │   ├── repository/   # Acceso a base de datos con JPA
│       │   └── service/      # Lógica del negocio
│       └── resources/
│           ├── application.properties
│           └── catalogo_inicial_perfumes.sql
├── iniciar-backend.ps1     # Inicio guiado en Windows
├── mvnw.cmd                # Maven Wrapper para Windows
├── pom.xml                 # Dependencias de Spring Boot
└── README.md
```

**Notas de seguridad:** `Root1234` se documenta aquí únicamente para simplificar las prácticas en los computadores del instituto. Esta base y contraseña son **solo para desarrollo académico**. No subir contraseñas personales, respaldos con datos reales, variables de entorno ni carpetas `uploads/` a un repositorio público.

## 8. Consultas útiles en MySQL Workbench

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

