package com.udea.supermercado.dto;

public record ProductoDto(Long id, String nombre, String categoria, Long precioCop, String unidad, Integer stock) {
}
