package com.udea.supermercado.dto;

import java.util.List;

public record PedidoDto(Long id, String estado, String cliente, String telefono, String direccionEntrega,
                        List<ItemPedidoDto> items, Long subtotalCop, Long costoDomicilioCop, Long totalCop,
                        String fechaCreacion) {
}
