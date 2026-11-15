package com.udea.supermercado.dto;

public record ItemPedidoDto(Long productoId, String producto, Integer cantidad, String unidad,
                            Long precioUnitarioCop, Long subtotalCop) {
}
