package com.udea.supermercado.repository;

import com.udea.supermercado.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByCategoriaIgnoreCaseOrderByNombre(String categoria);

    List<Producto> findByNombreContainingIgnoreCaseOrderByNombre(String texto);

    @Query("select distinct p.categoria from Producto p order by p.categoria")
    List<String> listarCategorias();
}
