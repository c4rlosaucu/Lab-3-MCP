package com.udea.supermercado.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedido")
public class Pedido {

    /** Compras desde este valor (COP) tienen domicilio gratis. */
    public static final long UMBRAL_DOMICILIO_GRATIS = 80_000L;
    /** Costo del domicilio (COP) cuando no se alcanza el umbral. */
    public static final long COSTO_DOMICILIO = 6_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado = EstadoPedido.CARRITO;

    private String direccionEntrega;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaConfirmacion;

    private Long subtotal = 0L;
    private Long costoDomicilio = 0L;
    private Long total = 0L;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    public Pedido() {}

    public Pedido(Cliente cliente, String direccionEntrega) {
        this.cliente = cliente;
        this.direccionEntrega = direccionEntrega;
    }

    /** Busca la línea del pedido que corresponde a un producto, o null si no está. */
    public ItemPedido buscarItem(Long productoId) {
        for (ItemPedido item : items) {
            if (item.getProducto().getId().equals(productoId)) {
                return item;
            }
        }
        return null;
    }

    /** Agrega el producto o suma la cantidad si ya estaba en el pedido. */
    public ItemPedido agregarItem(Producto producto, int cantidad) {
        ItemPedido existente = buscarItem(producto.getId());
        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
            existente.setPrecioUnitario(producto.getPrecio());
            recalcular();
            return existente;
        }
        ItemPedido nuevo = new ItemPedido(this, producto, cantidad);
        items.add(nuevo);
        recalcular();
        return nuevo;
    }

    public boolean quitarItem(Long productoId) {
        ItemPedido item = buscarItem(productoId);
        if (item == null) {
            return false;
        }
        items.remove(item);
        recalcular();
        return true;
    }

    /** Regla de negocio: subtotal + domicilio (gratis desde 80.000 COP; 0 si el pedido está vacío). */
    public void recalcular() {
        long suma = 0L;
        for (ItemPedido item : items) {
            suma += item.getSubtotal();
        }
        this.subtotal = suma;
        if (items.isEmpty() || suma >= UMBRAL_DOMICILIO_GRATIS) {
            this.costoDomicilio = 0L;
        } else {
            this.costoDomicilio = COSTO_DOMICILIO;
        }
        this.total = this.subtotal + this.costoDomicilio;
    }

    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setFechaConfirmacion(LocalDateTime fechaConfirmacion) { this.fechaConfirmacion = fechaConfirmacion; }
    public Long getSubtotal() { return subtotal; }
    public Long getCostoDomicilio() { return costoDomicilio; }
    public Long getTotal() { return total; }
    public List<ItemPedido> getItems() { return items; }
}
