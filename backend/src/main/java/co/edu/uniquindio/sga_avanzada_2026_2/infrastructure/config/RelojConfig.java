package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj único del sistema en la zona horaria de Colombia (EST-06 · ALO-02). Los casos de uso lo reciben por
 * constructor y pasan {@code hoy}/{@code ahora} al dominio, que nunca llama a {@code now()}.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj(@Value("${sga.zona-horaria:America/Bogota}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }
}
