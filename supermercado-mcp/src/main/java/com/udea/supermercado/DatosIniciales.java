package com.udea.supermercado;

import com.udea.supermercado.model.Producto;
import com.udea.supermercado.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga un catálogo de ejemplo la primera vez que arranca el servidor
 * (solo si la tabla producto está vacía). Datos sintéticos de laboratorio.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProductoRepository productos;

    public DatosIniciales(ProductoRepository productos) {
        this.productos = productos;
    }

    @Override
    public void run(String... args) {
        if (productos.count() > 0) {
            return;
        }
        productos.saveAll(List.of(
                // Frutas y verduras
                new Producto("Banano", "Frutas y verduras", 3_500L, 120, "kg"),
                new Producto("Manzana roja", "Frutas y verduras", 7_900L, 80, "kg"),
                new Producto("Tomate chonto", "Frutas y verduras", 4_200L, 90, "kg"),
                new Producto("Cebolla cabezona", "Frutas y verduras", 3_800L, 100, "kg"),
                new Producto("Papa pastusa", "Frutas y verduras", 2_900L, 150, "kg"),
                new Producto("Aguacate Hass", "Frutas y verduras", 6_500L, 60, "und"),
                // Lácteos y huevos
                new Producto("Leche entera 1 litro", "Lácteos y huevos", 4_600L, 200, "und"),
                new Producto("Leche deslactosada 1 litro", "Lácteos y huevos", 5_200L, 120, "und"),
                new Producto("Huevos AA x30", "Lácteos y huevos", 18_500L, 50, "paquete"),
                new Producto("Queso campesino 500 g", "Lácteos y huevos", 11_900L, 40, "und"),
                new Producto("Yogur natural 1 litro", "Lácteos y huevos", 8_700L, 45, "und"),
                // Carnes
                new Producto("Pechuga de pollo", "Carnes", 16_900L, 60, "kg"),
                new Producto("Carne molida de res", "Carnes", 24_900L, 35, "kg"),
                new Producto("Filete de tilapia", "Carnes", 22_500L, 25, "kg"),
                // Despensa
                new Producto("Arroz blanco 1 kg", "Despensa", 4_800L, 180, "und"),
                new Producto("Frijol cargamanto 500 g", "Despensa", 7_400L, 70, "und"),
                new Producto("Aceite vegetal 1 litro", "Despensa", 12_900L, 80, "und"),
                new Producto("Pasta espagueti 500 g", "Despensa", 3_900L, 110, "und"),
                new Producto("Azúcar 1 kg", "Despensa", 4_500L, 130, "und"),
                new Producto("Café molido 500 g", "Despensa", 21_900L, 55, "und"),
                // Bebidas
                new Producto("Agua sin gas 600 ml", "Bebidas", 2_200L, 300, "und"),
                new Producto("Gaseosa cola 1,5 litros", "Bebidas", 5_900L, 90, "und"),
                new Producto("Jugo de naranja 1 litro", "Bebidas", 6_800L, 60, "und"),
                // Panadería
                new Producto("Pan tajado integral", "Panadería", 6_300L, 70, "paquete"),
                new Producto("Pan francés x6", "Panadería", 5_400L, 40, "paquete"),
                // Aseo
                new Producto("Detergente en polvo 1 kg", "Aseo", 14_900L, 45, "und"),
                new Producto("Jabón de manos 500 ml", "Aseo", 9_600L, 50, "und"),
                new Producto("Papel higiénico x12", "Aseo", 19_900L, 65, "paquete")
        ));
    }
}
