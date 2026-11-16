package com.tienda.perfumes.config;

import com.tienda.perfumes.repository.ProductoRepository;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class CatalogoInicialConfig {

    @Bean
    public ApplicationRunner cargarCatalogoInicial(
            ProductoRepository productoRepository,
            DataSource dataSource) {
        return args -> {
            // Los datos reales del cliente nunca se reemplazan al reiniciar.
            if (productoRepository.count() > 0) {
                System.out.println("Catalogo inicial: hay productos; no se importan datos de prueba.");
                return;
            }

            // Este Runner se ejecuta despues de que Hibernate crea las tablas.
            ResourceDatabasePopulator importador = new ResourceDatabasePopulator();
            importador.setSqlScriptEncoding("UTF-8");
            importador.addScript(new ClassPathResource("catalogo_inicial_perfumes.sql"));
            DatabasePopulatorUtils.execute(importador, dataSource);

            System.out.println("Catalogo inicial importado. Productos disponibles: "
                    + productoRepository.count());
        };
    }
}
