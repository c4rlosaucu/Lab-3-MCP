package com.udea.supermercado.service;

import com.udea.supermercado.dto.ClienteDto;
import com.udea.supermercado.dto.ItemPedidoDto;
import com.udea.supermercado.dto.PedidoDto;
import com.udea.supermercado.dto.ProductoDto;
import com.udea.supermercado.model.Cliente;
import com.udea.supermercado.model.EstadoPedido;
import com.udea.supermercado.model.ItemPedido;
import com.udea.supermercado.model.Pedido;
import com.udea.supermercado.model.Producto;
import com.udea.supermercado.repository.ClienteRepository;
import com.udea.supermercado.repository.PedidoRepository;
import com.udea.supermercado.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramientas MCP del supermercado a domicilio.
 *
 * Flujo típico que sigue el chatbot:
 *   1. registrarCliente / buscarCliente        -> identifica al cliente por teléfono
 *   2. listarCategorias / listarProductos / buscarProductos -> consulta el catálogo
 *   3. crearPedido + agregarProducto           -> arma el carrito
 *   4. verPedido                               -> muestra el resumen y el total
 *   5. confirmarPedido (o cancelarPedido)      -> cierra el pedido (acciones que cambian datos)
 *
 * Todas las reglas (stock, estados, domicilio) se validan AQUÍ, en el servidor,
 * y no se delegan en lo que diga el modelo de IA.
 */
@Component
public class SupermercadoToolsService {

    private final ProductoRepository productos;
    private final ClienteRepository clientes;
    private final PedidoRepository pedidos;

    public SupermercadoToolsService(ProductoRepository productos,
                                    ClienteRepository clientes,
                                    PedidoRepository pedidos) {
        this.productos = productos;
        this.clientes = clientes;
        this.pedidos = pedidos;
    }

    // =====================================================================
    // CATÁLOGO
    // =====================================================================

    @Tool(name = "listarCategorias",
          description = "Devuelve las categorías de productos que vende el supermercado.")
    @Transactional(readOnly = true)
    public List<String> listarCategorias() {
        return productos.listarCategorias();
    }

    @Tool(name = "listarProductos",
          description = "Lista los productos del supermercado con precio en pesos colombianos (COP) y stock disponible. "
                  + "Si se indica una categoría devuelve solo esa; si el parámetro va vacío devuelve todo el catálogo.")
    @Transactional(readOnly = true)
    public List<ProductoDto> listarProductos(
            @ToolParam(description = "Categoría a filtrar, por ejemplo 'Lácteos y huevos'. Dejar vacío para ver todas.")
            String categoria) {
        List<Producto> lista = sinValor(categoria)
                ? productos.findAll()
                : productos.findByCategoriaIgnoreCaseOrderByNombre(categoria.trim());
        List<ProductoDto> salida = new ArrayList<>();
        for (Producto p : lista) {
            salida.add(aDto(p));
        }
        return salida;
    }

    @Tool(name = "buscarProductos",
          description = "Busca productos cuyo nombre contiene el texto indicado (sin distinguir mayúsculas).")
    @Transactional(readOnly = true)
    public List<ProductoDto> buscarProductos(
            @ToolParam(description = "Texto a buscar en el nombre del producto, por ejemplo 'leche'") String texto) {
        if (sinValor(texto)) {
            throw new IllegalArgumentException("Indique el texto a buscar");
        }
        List<ProductoDto> salida = new ArrayList<>();
        for (Producto p : productos.findByNombreContainingIgnoreCaseOrderByNombre(texto.trim())) {
            salida.add(aDto(p));
        }
        return salida;
    }

    // =====================================================================
    // CLIENTES
    // =====================================================================

    @Tool(name = "registrarCliente",
          description = "Registra un cliente nuevo para domicilios. Si el teléfono ya existe, actualiza su nombre y dirección.")
    @Transactional
    public ClienteDto registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombre,
            @ToolParam(description = "Teléfono del cliente (solo números, mínimo 7 dígitos)") String telefono,
            @ToolParam(description = "Dirección de entrega habitual, con barrio o referencia") String direccion) {
        if (sinValor(nombre)) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (sinValor(direccion)) {
            throw new IllegalArgumentException("La dirección es obligatoria");
        }
        String tel = normalizarTelefono(telefono);
        Cliente cliente = clientes.findByTelefono(tel).orElse(null);
        if (cliente == null) {
            cliente = new Cliente(nombre.trim(), tel, direccion.trim());
        } else {
            cliente.setNombre(nombre.trim());
            cliente.setDireccion(direccion.trim());
        }
        return aDto(clientes.save(cliente));
    }

    @Tool(name = "buscarCliente",
          description = "Busca un cliente por su número de teléfono. Devuelve un error si no está registrado.")
    @Transactional(readOnly = true)
    public ClienteDto buscarCliente(
            @ToolParam(description = "Teléfono del cliente (solo números)") String telefono) {
        return aDto(clienteObligatorio(normalizarTelefono(telefono)));
    }

    // =====================================================================
    // PEDIDOS
    // =====================================================================

    @Tool(name = "crearPedido",
          description = "Crea un pedido nuevo (carrito vacío) para un cliente registrado. "
                  + "Si el cliente ya tiene un carrito abierto, devuelve ese mismo en lugar de crear otro.")
    @Transactional
    public PedidoDto crearPedido(
            @ToolParam(description = "Teléfono del cliente registrado") String telefonoCliente,
            @ToolParam(description = "Dirección de entrega de este pedido; vacío para usar la dirección registrada del cliente")
            String direccionEntrega) {
        String tel = normalizarTelefono(telefonoCliente);
        Cliente cliente = clienteObligatorio(tel);
        Pedido abierto = pedidos.findFirstByClienteTelefonoAndEstadoOrderByIdDesc(tel, EstadoPedido.CARRITO).orElse(null);
        if (abierto != null) {
            if (!sinValor(direccionEntrega)) {
                abierto.setDireccionEntrega(direccionEntrega.trim());
            }
            return aDto(abierto);
        }
        String direccion = sinValor(direccionEntrega) ? cliente.getDireccion() : direccionEntrega.trim();
        return aDto(pedidos.save(new Pedido(cliente, direccion)));
    }

    @Tool(name = "agregarProducto",
          description = "Agrega un producto al pedido (carrito). Si ya estaba, suma la cantidad. "
                  + "Falla si no hay stock suficiente o si el pedido ya no es un carrito abierto.")
    @Transactional
    public PedidoDto agregarProducto(
            @ToolParam(description = "Id del pedido (carrito)") Long pedidoId,
            @ToolParam(description = "Id del producto, obtenido de listarProductos o buscarProductos") Long productoId,
            @ToolParam(description = "Cantidad a agregar, mayor que cero") Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        Pedido pedido = carritoAbierto(pedidoId);
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el producto " + productoId));
        ItemPedido existente = pedido.buscarItem(productoId);
        int total = cantidad + (existente == null ? 0 : existente.getCantidad());
        if (producto.getStock() < total) {
            throw new IllegalArgumentException("Stock insuficiente de '" + producto.getNombre()
                    + "': hay " + producto.getStock() + " " + producto.getUnidad() + " disponibles");
        }
        pedido.agregarItem(producto, cantidad);
        return aDto(pedidos.save(pedido));
    }

    @Tool(name = "quitarProducto",
          description = "Quita por completo un producto del pedido (carrito abierto).")
    @Transactional
    public PedidoDto quitarProducto(
            @ToolParam(description = "Id del pedido (carrito)") Long pedidoId,
            @ToolParam(description = "Id del producto a quitar") Long productoId) {
        Pedido pedido = carritoAbierto(pedidoId);
        if (!pedido.quitarItem(productoId)) {
            throw new IllegalArgumentException("El producto " + productoId + " no está en el pedido " + pedidoId);
        }
        return aDto(pedidos.save(pedido));
    }

    @Tool(name = "verPedido",
          description = "Muestra el detalle de un pedido: estado, productos, subtotal, domicilio y total en COP.")
    @Transactional(readOnly = true)
    public PedidoDto verPedido(
            @ToolParam(description = "Id del pedido") Long pedidoId) {
        return aDto(pedidoObligatorio(pedidoId));
    }

    @Tool(name = "listarPedidosCliente",
          description = "Lista los pedidos de un cliente (del más reciente al más antiguo) por su teléfono.")
    @Transactional(readOnly = true)
    public List<PedidoDto> listarPedidosCliente(
            @ToolParam(description = "Teléfono del cliente (solo números)") String telefono) {
        List<PedidoDto> salida = new ArrayList<>();
        for (Pedido p : pedidos.findByClienteTelefonoOrderByIdDesc(normalizarTelefono(telefono))) {
            salida.add(aDto(p));
        }
        return salida;
    }

    @Tool(name = "confirmarPedido",
          description = "CONFIRMA el pedido: descuenta el stock y lo deja programado para domicilio. "
                  + "Acción que modifica datos: úsala solo después de que el cliente apruebe el resumen y el total.")
    @Transactional
    public PedidoDto confirmarPedido(
            @ToolParam(description = "Id del pedido (carrito) a confirmar") Long pedidoId) {
        Pedido pedido = carritoAbierto(pedidoId);
        if (pedido.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " está vacío: agregue productos antes de confirmar");
        }
        // 1) validar todo el stock antes de tocar nada
        for (ItemPedido item : pedido.getItems()) {
            Producto p = item.getProducto();
            if (p.getStock() < item.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente de '" + p.getNombre()
                        + "': hay " + p.getStock() + " y el pedido pide " + item.getCantidad());
            }
        }
        // 2) descontar stock
        for (ItemPedido item : pedido.getItems()) {
            Producto p = item.getProducto();
            p.setStock(p.getStock() - item.getCantidad());
        }
        pedido.recalcular();
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        pedido.setFechaConfirmacion(LocalDateTime.now());
        return aDto(pedidos.save(pedido));
    }

    @Tool(name = "cancelarPedido",
          description = "CANCELA un pedido en estado carrito o confirmado (si estaba confirmado, devuelve el stock). "
                  + "Acción que modifica datos: pide confirmación al cliente antes de usarla.")
    @Transactional
    public PedidoDto cancelarPedido(
            @ToolParam(description = "Id del pedido a cancelar") Long pedidoId) {
        Pedido pedido = pedidoObligatorio(pedidoId);
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " ya estaba cancelado");
        }
        if (pedido.getEstado() == EstadoPedido.CONFIRMADO) {
            for (ItemPedido item : pedido.getItems()) {
                Producto p = item.getProducto();
                p.setStock(p.getStock() + item.getCantidad());
            }
        }
        pedido.setEstado(EstadoPedido.CANCELADO);
        return aDto(pedidos.save(pedido));
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private Pedido pedidoObligatorio(Long pedidoId) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("Indique el id del pedido");
        }
        return pedidos.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el pedido " + pedidoId));
    }

    private Pedido carritoAbierto(Long pedidoId) {
        Pedido pedido = pedidoObligatorio(pedidoId);
        if (pedido.getEstado() != EstadoPedido.CARRITO) {
            throw new IllegalArgumentException("El pedido " + pedidoId + " está " + pedido.getEstado()
                    + " y ya no se puede modificar");
        }
        return pedido;
    }

    private Cliente clienteObligatorio(String telefonoNormalizado) {
        return clientes.findByTelefono(telefonoNormalizado)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay un cliente registrado con el teléfono " + telefonoNormalizado
                                + ". Use registrarCliente primero"));
    }

    /** Deja solo dígitos y exige un mínimo de 7. */
    public static String normalizarTelefono(String telefono) {
        if (telefono == null) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
        }
        String soloDigitos = telefono.replaceAll("\\D", "");
        if (soloDigitos.length() < 7) {
            throw new IllegalArgumentException("Teléfono inválido: debe tener al menos 7 dígitos");
        }
        return soloDigitos;
    }

    private static boolean sinValor(String s) {
        return s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim());
    }

    private static ProductoDto aDto(Producto p) {
        return new ProductoDto(p.getId(), p.getNombre(), p.getCategoria(), p.getPrecio(), p.getUnidad(), p.getStock());
    }

    private static ClienteDto aDto(Cliente c) {
        return new ClienteDto(c.getId(), c.getNombre(), c.getTelefono(), c.getDireccion());
    }

    private static PedidoDto aDto(Pedido p) {
        List<ItemPedidoDto> items = new ArrayList<>();
        for (ItemPedido i : p.getItems()) {
            items.add(new ItemPedidoDto(i.getProducto().getId(), i.getProducto().getNombre(), i.getCantidad(),
                    i.getProducto().getUnidad(), i.getPrecioUnitario(), i.getSubtotal()));
        }
        return new PedidoDto(p.getId(), p.getEstado().name(), p.getCliente().getNombre(), p.getCliente().getTelefono(),
                p.getDireccionEntrega(), items, p.getSubtotal(), p.getCostoDomicilio(), p.getTotal(),
                String.valueOf(p.getFechaCreacion()));
    }
}
