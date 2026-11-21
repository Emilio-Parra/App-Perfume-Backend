# Pruebas — Perfumes Store Backend

La pauta pide que la lógica de negocio esté **"correctamente encapsulada y testeada"**. El backend tiene **64 pruebas automatizadas, todas aprobadas**, y además se hicieron pruebas manuales de la API y del frontend.

| Resumen | |
|---|---|
| Pruebas automatizadas | **64** (64 ✅, 0 ❌) |
| Herramientas | JUnit 6, Mockito, AssertJ, Spring MockMvc, Spring Security Test |
| Base de datos de pruebas | H2 en memoria, modo MySQL (no necesita MySQL encendido) |
| Cobertura de líneas | **74.8%** (JaCoCo) |

---

## 1. Cómo ejecutarlas

Desde la carpeta `App-Perfume-Backend`:

```bash
.\mvnw test          # Windows
./mvnw test          # Mac / Linux
```

Resultado esperado al final:

```
[INFO] Tests run: 64, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| Archivo generado | Contenido |
|---|---|
| `target/surefire-reports/` | Resultado de cada prueba |
| `target/site/jacoco/index.html` | Reporte de cobertura (abrir en el navegador) |

### Configuración de las pruebas
`src/test/resources/application.properties` reemplaza la configuración normal solo al ejecutar las pruebas:
- Usa **H2 en memoria** en lugar de MySQL.
- No carga el catálogo inicial (el script usa sintaxis propia de MySQL).
- Fija el pago simulado en **100% de aprobación**, para que el resultado sea predecible.

---

## 2. Tipos de prueba

| Tipo | Qué prueba | Cómo |
|---|---|---|
| **Unitarias** | La lógica de cada servicio por separado | **Mockito** reemplaza los repositorios por objetos simulados: no se toca la base de datos |
| **De integración** | La API completa: filtro JWT → Spring Security → controlador → servicio → base de datos H2 | **MockMvc** hace peticiones HTTP reales a la aplicación levantada |

### Cambio de diseño para poder probar el pago
El pago simulado usaba `Math.random()` dentro de `PedidoService`, y algo al azar no se puede probar. Se extrajo a la clase **`SimuladorPago`**, que en las pruebas se reemplaza por un simulador que siempre aprueba o siempre rechaza. Así se verifican los dos caminos (pago aprobado y pago rechazado).

---

## 3. Detalle de las pruebas

### `PedidoServiceTest` — 9 pruebas (unitarias)
| Grupo | Caso | Resultado |
|---|---|:-:|
| Crear pedido | Calcula el total con el precio de MySQL y el descuento (no con el del navegador) | ✅ |
| Crear pedido | Crear el pedido NO descuenta stock (se descuenta al pagar) | ✅ |
| Crear pedido | Rechaza comprar más unidades que el stock disponible (409) | ✅ |
| Crear pedido | Rechaza un producto que no existe (404) | ✅ |
| Crear pedido | Un cliente no puede crear pedidos a nombre de otro (403) | ✅ |
| Pagar pedido | Pago aprobado: queda `pagado` y descuenta el stock | ✅ |
| Pagar pedido | Pago rechazado: queda `fallido` y el stock no cambia | ✅ |
| Pagar pedido | No se puede pagar dos veces el mismo pedido (409) | ✅ |
| Pagar pedido | Un cliente no puede pagar el pedido de otro (403) | ✅ |

### `UsuarioServiceTest` — 9 pruebas (unitarias)
| Caso | Resultado |
|---|:-:|
| Registro: guarda la contraseña cifrada con BCrypt, nunca en texto plano | ✅ |
| Registro: rechaza un correo ya registrado (409) | ✅ |
| Registro: rechaza un RUT ya registrado (409) | ✅ |
| Login: acepta la contraseña correcta y rechaza una incorrecta | ✅ |
| Panel: el admin puede crear un VENDEDOR | ✅ |
| Panel: sin rol, el usuario se crea como cliente | ✅ |
| Panel: NO se puede crear un administrador desde el formulario | ✅ |
| Panel: la contraseña es obligatoria al crear | ✅ |
| Panel: no se elimina un usuario que tiene pedidos (409) | ✅ |

### `CategoriaServiceTest` — 6 pruebas (unitarias)
| Caso | Resultado |
|---|:-:|
| Crea una categoría quitando espacios sobrantes | ✅ |
| No permite nombres repetidos, sin importar mayúsculas (409) | ✅ |
| No elimina una categoría que tiene productos (409) | ✅ |
| Elimina una categoría vacía | ✅ |
| Al renombrar, los productos de la categoría se actualizan | ✅ |
| Responde 404 si la categoría no existe | ✅ |

### `ProductoServiceTest` — 4 pruebas (unitarias)
| Caso | Resultado |
|---|:-:|
| Crea el producto limpiando textos y con descuento 0 si no se indica | ✅ |
| Si no se indica género, queda como unisex | ✅ |
| Actualiza un producto existente conservando su ID | ✅ |
| Actualizar o eliminar un producto inexistente responde 404 | ✅ |

### `JwtServiceTest` — 6 pruebas (unitarias)
| Caso | Resultado |
|---|:-:|
| Genera un token con el usuario, el rol y los datos extra | ✅ |
| Rechaza un token cuyo rol fue modificado (la firma no coincide) | ✅ |
| Rechaza un token firmado con otra clave | ✅ |
| Rechaza un token vencido | ✅ |
| Rechaza texto que no es un JWT | ✅ |
| No permite una clave secreta de menos de 32 caracteres | ✅ |

### `RutValidatorTest` — 16 pruebas (unitarias, parametrizadas)
| Caso | Valores probados | Resultado |
|---|---|:-:|
| Acepta RUN válidos (7 casos) | `111111111`, `123456785`, `222222222`, `760864285`, `10000013K`, `10000013k`, `10000017` | ✅ |
| Rechaza RUN inválidos (7 casos) | `123456789` (dígito incorrecto), `19011022K`, `12.345.678-5` (con puntos), `12345678-5` (con guion), `abc`, `1234`, `1234567890` | ✅ |
| Vacío o nulo se deja pasar (`@NotBlank` es quien lo exige) | `null`, `"  "` | ✅ |
| Ignora espacios al inicio y al final | `"  111111111 "` | ✅ |

### `SeguridadApiIntegrationTest` — 13 pruebas (integración)
| Grupo | Caso | Resultado |
|---|---|:-:|
| Sin iniciar sesión | Puede ver el catálogo (GET público) | ✅ |
| Sin iniciar sesión | No puede crear productos ni ver órdenes (401) | ✅ |
| Sin iniciar sesión | Un token alterado o vencido se rechaza (401) | ✅ |
| Rol ADMIN | Inicia sesión y recibe un token JWT; con él ve su perfil | ✅ |
| Rol ADMIN | Contraseña incorrecta: 401 | ✅ |
| Rol ADMIN | Puede crear y eliminar productos | ✅ |
| Rol ADMIN | Los datos inválidos se rechazan con 400 y el campo con error | ✅ |
| Rol ADMIN | Puede ver usuarios y órdenes | ✅ |
| Rol VENDEDOR | Puede ver productos (listado y detalle) y órdenes (listado) | ✅ |
| Rol VENDEDOR | No puede crear, borrar ni ver usuarios (403) | ✅ |
| Rol CLIENTE | No puede ver las órdenes de otros ni el panel (403) | ✅ |
| Rol CLIENTE | **Flujo completo: registro → login → compra → pago → stock descontado** | ✅ |
| Rol CLIENTE | El registro con un RUN inválido se rechaza (400) | ✅ |

### `PerfumesApplicationTests` — 1 prueba
| Caso | Resultado |
|---|:-:|
| La aplicación completa arranca sin errores (`contextLoads`) | ✅ |

---

## 4. Cobertura de código (JaCoCo)

| Paquete | Líneas | Ramas (if/else) |
|---|:-:|:-:|
| `security` | 100.0% | 66.7% |
| `validation` | 100.0% | 88.9% |
| `dto` | 100.0% | — |
| `model` | 90.0% | — |
| `config` | 86.5% | 37.5% |
| `service` | 72.6% | 50.0% |
| `controller` | 58.6% | 10.9% |
| `exception` | 38.5% | 33.3% |
| **Total** | **74.8%** | **43.6%** |

La lógica de negocio (`service`), la seguridad (`security`) y las validaciones (`validation`) son las partes con más cobertura, que es donde están las reglas que exige la pauta. El paquete `exception` tiene menos porque incluye respuestas para errores poco frecuentes (por ejemplo, 405 o 500).

---

## 5. Pruebas manuales de la API

Se levantó el backend con MySQL y se llamó a cada endpoint con `curl`:

| # | Prueba | Esperado | Obtenido |
|---|---|:-:|:-:|
| 1 | Listar productos sin token | 200 | ✅ 200 (27 productos) |
| 2 | Producto que no existe (`/productos/9999`) | 404 | ✅ 404 |
| 3 | Crear producto sin token | 401 | ✅ 401 |
| 4 | Login de admin con contraseña incorrecta | 401 | ✅ 401 |
| 5 | Login de admin correcto | 200 + token | ✅ 200 |
| 6 | Crear producto con precio `-5` | 400 | ✅ 400 *"El precio debe ser mayor a 0."* |
| 7 | Crear, editar y eliminar producto (admin) | 201 / 200 / 204 | ✅ |
| 8 | Crear categoría repetida | 409 | ✅ 409 |
| 9 | Eliminar categoría con productos | 409 | ✅ 409 |
| 10 | Registro con RUN inválido | 400 | ✅ 400 |
| 11 | Registro con correo repetido | 409 | ✅ 409 |
| 12 | Cliente intenta ver usuarios del panel | 403 | ✅ 403 |
| 13 | Cliente crea un pedido | 201 | ✅ 201 |
| 14 | Cliente crea un pedido a nombre de otro | 403 | ✅ 403 |
| 15 | Pedido con más unidades que el stock | 409 | ✅ 409 *"No hay stock suficiente..."* |
| 16 | Cliente paga su pedido | 200 | ✅ 200 (`pagado` / `fallido`) |
| 17 | Admin intenta comprar | 403 | ✅ 403 |
| 18 | Vendedor ve productos y órdenes | 200 | ✅ 200 |
| 19 | Vendedor intenta crear o borrar productos, crear categorías o ver usuarios | 403 | ✅ 403 |
| 20 | Cliente cambia su rol a `ADMIN` dentro del token | 401 | ✅ 401 |
| 21 | Subir imagen sin token / con token de admin | 401 / 201 | ✅ |
| 22 | Swagger UI y especificación OpenAPI | 200 | ✅ (28 endpoints documentados) |

## 6. Pruebas en el navegador (frontend + backend)

Se probó la aplicación completa en Chromium (automatizado con Playwright):

| Rol | Prueba | Resultado |
|---|---|:-:|
| ADMIN | Login, panel completo; todas las llamadas llevan `Authorization: Bearer` | ✅ |
| ADMIN | Productos (27), categorías (3), órdenes, usuarios y dashboard cargan sin errores | ✅ |
| ADMIN | Crear usuario con rol Cliente o Vendedor | ✅ |
| VENDEDOR | El panel muestra solo **Órdenes** y **Productos**, sin botones de crear, editar ni eliminar | ✅ |
| VENDEDOR | Detalle de una orden (boleta) y de un producto | ✅ |
| CLIENTE | Login y compra sin ingresar el token a mano (lo agrega `authFetch.js`) | ✅ |
| CLIENTE | Con un token inválido, la sesión se cierra y vuelve al login | ✅ |
| CLIENTE | No ve el botón del panel | ✅ |
