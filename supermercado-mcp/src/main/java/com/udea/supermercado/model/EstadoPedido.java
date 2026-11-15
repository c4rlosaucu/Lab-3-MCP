package com.udea.supermercado.model;

/** Ciclo de vida de un pedido: CARRITO (en armado) -> CONFIRMADO, o CANCELADO. */
public enum EstadoPedido {
    CARRITO,
    CONFIRMADO,
    CANCELADO
}
