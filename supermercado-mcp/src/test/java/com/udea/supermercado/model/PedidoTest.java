package com.udea.supermercado.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la regla de negocio del pedido (sin base de datos):
 * el domicilio cuesta 6.000 COP y es gratis desde 80.000 COP.
 */
class PedidoTest {

    private Producto producto(long id, String nombre, long precio, int stock) {
        Producto p = new Producto(nombre, "Despensa", precio, stock, "und");
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private Pedido pedidoVacio() {
        return new Pedido(new Cliente("Ana", "3001234567", "Calle 1 # 2-3"), "Calle 1 # 2-3");
    }

    @Test
    void pedidoVacio_noCobraDomicilio() {
        Pedido pedido = pedidoVacio();
        pedido.recalcular();
        assertEquals(0L, pedido.getSubtotal());
        assertEquals(0L, pedido.getCostoDomicilio());
        assertEquals(0L, pedido.getTotal());
    }

    @Test
    void compraPequena_cobraDomicilio() {
        Pedido pedido = pedidoVacio();
        pedido.agregarItem(producto(1, "Arroz", 4_800L, 10), 2);   // 9.600
        assertEquals(9_600L, pedido.getSubtotal());
        assertEquals(Pedido.COSTO_DOMICILIO, pedido.getCostoDomicilio());
        assertEquals(15_600L, pedido.getTotal());
    }

    @Test
    void compraDesdeElUmbral_tieneDomicilioGratis() {
        Pedido pedido = pedidoVacio();
        pedido.agregarItem(producto(2, "Café", 21_900L, 10), 4);   // 87.600
        assertEquals(87_600L, pedido.getSubtotal());
        assertEquals(0L, pedido.getCostoDomicilio());
        assertEquals(87_600L, pedido.getTotal());
    }

    @Test
    void agregarElMismoProducto_sumaLaCantidad() {
        Pedido pedido = pedidoVacio();
        Producto leche = producto(3, "Leche", 4_600L, 50);
        pedido.agregarItem(leche, 2);
        pedido.agregarItem(leche, 3);
        assertEquals(1, pedido.getItems().size());
        assertEquals(5, pedido.getItems().get(0).getCantidad());
        assertEquals(23_000L, pedido.getSubtotal());
    }

    @Test
    void quitarProducto_recalculaElTotal() {
        Pedido pedido = pedidoVacio();
        pedido.agregarItem(producto(4, "Pasta", 3_900L, 20), 1);
        pedido.agregarItem(producto(5, "Azúcar", 4_500L, 20), 1);
        assertTrue(pedido.quitarItem(4L));
        assertEquals(4_500L, pedido.getSubtotal());
        assertFalse(pedido.quitarItem(99L));
    }
}
