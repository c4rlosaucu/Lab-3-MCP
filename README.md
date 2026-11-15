# Laboratorio 3: servidor MCP con Spring Boot, Spring AI, MySQL, Claude Desktop y Google ADK

Trabajo de la materia **Arquitectura de Software** (Ingeniería de Sistemas, Universidad de Antioquia).

- **Estudiante:** Carlos Alberto Aucu Gomez
- **Profesor:** Diego José Luis Botia V.
- **Entrega:** 15 de noviembre de 2026

Este repositorio reúne tres proyectos que muestran cómo un mismo servidor del **Model Context Protocol (MCP)** puede ser usado por distintos clientes de IA: MCP Inspector, Claude Desktop y un agente de Google Agent Development Kit (ADK).

## Contenido

| Carpeta | Qué es | Puerto | Base de datos |
|---|---|---|---|
| `labmcp/` | Servidor MCP de tareas (caso base del laboratorio, con las actividades resueltas) | 8088 | `labmcp` |
| `agentemcp/` | Agentes de Google ADK: `TareaAgent` y `SupermercadoAgent` | 8080 (interfaz web de ADK) | no usa |
| `supermercado-mcp/` | Servidor MCP del ejercicio propuesto: pedidos a domicilio de un supermercado | 8089 | `supermercado_mcp` |

## Cómo se relacionan las piezas

```
Usuario
  |-- Claude Desktop (cliente MCP, puente mcp-remote) --\
  |                                                      >-- Servidor MCP (Spring Boot + Spring AI, endpoint /mcp)
  |-- Agente Google ADK (McpToolset) -------------------/        |
  |-- MCP Inspector (pruebas manuales) -----------------/        v
                                                        Servicio Java (@Tool) -> Spring Data JPA -> MySQL
```

El servidor concentra la lógica de negocio y publica métodos Java como herramientas MCP con la anotación `@Tool`. Los clientes solo descubren esas herramientas y las invocan. Por eso un servidor sirve a varios clientes sin cambios.

## Base del trabajo

El código de partida viene de los repositorios del profesor:

- Servidor MCP: https://github.com/diegobotia/labmcp
- Agente Google ADK: https://github.com/diegobotia/agentemcp

Sobre esa base se hicieron estos cambios:

- **`labmcp`**: herramientas nuevas `contarTareas` y `listCompletadas` (ahora son 9), validación estricta de fecha ISO (`yyyy-MM-dd`), corrección de `updateTarea` (el texto `null` ya no se guarda como fecha y los espacios se recortan) y 8 pruebas unitarias con Mockito.
- **`agentemcp`**: modelo cambiado a `gemini-3.5-flash-lite` y nueva clase `SupermercadoAgent`.
- **`supermercado-mcp`**: proyecto nuevo para el ejercicio propuesto.

## Requisitos

- Java 21
- MySQL 8 en `localhost:3306`
- Node.js y npm (para MCP Inspector y para `mcp-remote`)
- Una clave de API de Google Gemini (solo para el agente ADK)
- Maven no hace falta instalarlo, cada proyecto incluye `mvnw` y `mvnw.cmd`

## Variables de entorno

Ninguna clave ni contraseña está escrita en el código. Se leen de variables de entorno.

| Variable | Para qué |
|---|---|
| `PWD_UDEA` | Contraseña del usuario `root` de MySQL (la usan `labmcp` y `supermercado-mcp`) |
| `GOOGLE_API_KEY` | Clave de Gemini (la usa `agentemcp`) |

En PowerShell, en la misma ventana donde se va a ejecutar el proyecto:

```powershell
$env:PWD_UDEA = Read-Host "Contraseña de MySQL"
$env:GOOGLE_API_KEY = Read-Host "Clave de Gemini"
```

## Puesta en marcha

### 1. Servidor de tareas (`labmcp`)

1. Crear la base de datos ejecutando `labmcp/src/main/resources/schema.sql` en MySQL Workbench (crea `labmcp`, la tabla `tarea` y tres tareas de ejemplo).
2. Arrancar:

```powershell
cd labmcp
.\mvnw.cmd spring-boot:run
```

El endpoint MCP queda en `http://localhost:8088/mcp`.

### 2. Servidor del supermercado (`supermercado-mcp`)

La base `supermercado_mcp` se crea sola al arrancar y se llena con 28 productos sintéticos.

```powershell
cd supermercado-mcp
.\mvnw.cmd spring-boot:run
```

El endpoint MCP queda en `http://localhost:8089/mcp`. Se puede correr al mismo tiempo que `labmcp` porque usan puertos distintos.

### 3. Agente de Google ADK (`agentemcp`)

Con al menos uno de los servidores MCP encendido:

```powershell
cd agentemcp
.\mvnw.cmd clean install
.\mvnw.cmd exec:java@runAdkWebServer
```

Abrir http://localhost:8080 y elegir el agente (`TareaAgent` o `SupermercadoAgent`) en la lista. Si el servidor MCP no está disponible, el agente arranca en modo de respaldo y lo avisa en su respuesta.

### 4. Probar con MCP Inspector

```powershell
npx @modelcontextprotocol/inspector
```

En la interfaz: transporte **Streamable HTTP** y la URL del servidor (`http://localhost:8088/mcp` u `http://localhost:8089/mcp`). Luego, pestaña **Tools** para listar y ejecutar herramientas.

### 5. Conectar Claude Desktop

Claude Desktop se conecta por STDIO, así que se usa el puente `mcp-remote`. Se agrega una entrada por servidor en `claude_desktop_config.json`, dentro del objeto `mcpServers`:

```json
{
  "mcpServers": {
    "tareas-mcp": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8088/mcp"]
    },
    "supermercado-mcp": {
      "command": "npx",
      "args": ["-y", "mcp-remote", "http://localhost:8089/mcp"]
    }
  }
}
```

Después hay que cerrar Claude Desktop por completo y volver a abrirlo. En Windows el archivo puede estar bajo `AppData\Local\Claude-Data\...` en lugar de `AppData\Roaming\Claude`. Conviene validar el JSON antes de reiniciar.

## Herramientas MCP

### `labmcp` (9)

`listTareas`, `listPendientes`, `listCompletadas`, `contarTareas`, `buscarTareas`, `crearTarea`, `markComplete`, `updateTarea`, `eliminarTarea`.

Las fechas deben tener formato ISO `yyyy-MM-dd`. Una fecha en otro formato o inexistente se rechaza con un mensaje claro.

### `supermercado-mcp` (12)

| Bloque | Herramientas |
|---|---|
| Catálogo | `listarCategorias`, `listarProductos`, `buscarProductos` |
| Clientes | `registrarCliente`, `buscarCliente` |
| Pedidos | `crearPedido`, `agregarProducto`, `quitarProducto`, `verPedido`, `listarPedidosCliente`, `confirmarPedido`, `cancelarPedido` |

Reglas de negocio:

- El domicilio cuesta 6.000 COP y es gratis cuando el subtotal llega a 80.000 COP.
- El precio unitario se guarda al agregar cada producto al pedido.
- El stock se descuenta al confirmar y se devuelve si se cancela un pedido ya confirmado.
- El teléfono del cliente se normaliza a solo dígitos y debe tener al menos 7.
- Un pedido pasa por los estados `CARRITO`, `CONFIRMADO` y `CANCELADO`.
- El agente `SupermercadoAgent` pide confirmación explícita al usuario antes de llamar a `confirmarPedido` o `cancelarPedido`.

## Pruebas automatizadas

```powershell
# labmcp: 8 pruebas de TareaToolsService (no necesitan MySQL)
cd labmcp
.\mvnw.cmd clean test "-Dtest=TareaToolsServiceTest"

# supermercado-mcp: 5 pruebas de la regla de cálculo del pedido
cd ..\supermercado-mcp
.\mvnw.cmd clean test "-Dtest=PedidoTest"
```

Las pruebas `*ApplicationTests` arrancan el contexto de Spring y necesitan MySQL encendido y `PWD_UDEA` definida. La capa de herramientas del supermercado se verificó manualmente con MCP Inspector y con los dos clientes de chat, no tiene pruebas automatizadas.

## Limitaciones conocidas

- Los endpoints `/mcp` no tienen autenticación. Es un montaje de laboratorio para uso local. En un uso real harían falta autenticación, TLS y escuchar solo en `127.0.0.1`.
- Los servidores usan el usuario `root` de MySQL. Lo recomendable es un usuario con permisos mínimos.
- En el supermercado, el cliente se identifica solo por teléfono y la confirmación del usuario depende de las instrucciones del agente, no de una verificación del servidor.
- No hay bloqueo pesimista de stock, por lo que dos pedidos simultáneos del mismo producto podrían competir.

## Diferencias respecto a la guía

La guía del laboratorio y los repositorios del profesor no coinciden en todo, y aquí se siguió siempre lo que trae el repositorio: puerto 8088 (la guía a veces dice 8888), base de datos `labmcp` (la guía dice `tareas_mcp`), nombres de herramientas `listTareas`, `markComplete` y `updateTarea`, y paquete `com.example.labmcp`. Además, el comando del Inspector de la guía corresponde a una versión antigua, y se usó la versión actual con Streamable HTTP. El modelo `gemini-2.5-flash` ya no está disponible para usuarios nuevos y se usó `gemini-3.5-flash-lite`.

## Informe

El informe completo del laboratorio, con el procedimiento, las capturas, los errores encontrados y las actividades 1 a 7, se entrega por separado en formato Word.
