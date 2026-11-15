package com.udea.agente_mcp;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.BaseTool;
import com.google.adk.tools.mcp.McpToolset;
import com.google.adk.tools.mcp.StreamableHttpServerParameters;

import java.time.Duration;
import java.util.List;

/**
 * Chatbot (cliente MCP) del supermercado a domicilio.
 * Se conecta al servidor MCP supermercado-mcp (puerto 8089) y usa sus tools
 * para atender pedidos. ADK la descubre por el campo ROOT_AGENT.
 */
public class SupermercadoAgent {

    public static BaseAgent ROOT_AGENT = buildAgent();

    private static final String MODELO = "gemini-3.5-flash-lite";

    private static final String INSTRUCCION = """
            Eres el asistente virtual de un supermercado con servicio a domicilio en Colombia.
            Hablas en español, con trato amable y respuestas cortas. Todos los precios están en pesos colombianos (COP).

            Reglas de trabajo:
            1. Identifica al cliente por su teléfono. Si buscarCliente indica que no existe, pide nombre,
               teléfono y dirección y usa registrarCliente.
            2. Para recomendar o buscar productos usa SIEMPRE las tools (listarCategorias, listarProductos,
               buscarProductos). No inventes productos, precios ni stock.
            3. Para armar el pedido usa crearPedido y agregarProducto con los ids reales de producto.
               Si el cliente pide algo sin stock, informa cuánto hay disponible.
            4. Antes de confirmar, muestra el resumen con verPedido: productos, cantidades, subtotal,
               costo de domicilio y total. El domicilio cuesta 6.000 COP y es gratis desde 80.000 COP de subtotal.
            5. Usa confirmarPedido o cancelarPedido SOLO después de que el cliente lo autorice de forma
               explícita en su último mensaje (por ejemplo, "sí, confirma"). Nunca los llames por tu cuenta.
            6. Si una tool devuelve un error, explícalo al cliente con claridad y propón una alternativa.
            """;

    private static BaseAgent buildAgent() {
        StreamableHttpServerParameters params = StreamableHttpServerParameters.builder()
                .url("http://localhost:8089/mcp")
                .timeout(Duration.ofSeconds(30L))
                .build();
        McpToolset mcpToolset = new McpToolset(params);

        try {
            List<BaseTool> mcpTools = mcpToolset.getTools(null)
                    .toList()
                    .blockingGet();
            System.out.println("Tools MCP supermercado: " + mcpTools.size());

            return LlmAgent.builder()
                    .name("SupermercadoAgent")
                    .model(MODELO)
                    .description("Asistente de pedidos a domicilio del supermercado")
                    .instruction(INSTRUCCION)
                    .tools(mcpTools)
                    .build();
        } catch (Exception e) {
            System.err.println("MCP supermercado no disponible: " + e.getMessage());
            return LlmAgent.builder()
                    .name("SupermercadoAgent")
                    .model(MODELO)
                    .description("Asistente del supermercado (servidor MCP no disponible)")
                    .instruction("El servidor MCP del supermercado no está disponible. Díselo al usuario y pídele que lo intente más tarde.")
                    .build();
        }
    }
}
