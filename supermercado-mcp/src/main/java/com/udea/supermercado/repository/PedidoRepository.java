package com.udea.supermercado.repository;

import com.udea.supermercado.model.EstadoPedido;
import com.udea.supermercado.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteTelefonoOrderByIdDesc(String telefono);

    Optional<Pedido> findFirstByClienteTelefonoAndEstadoOrderByIdDesc(String telefono, EstadoPedido estado);
}
