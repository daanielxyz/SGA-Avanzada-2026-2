package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * El alojamiento de este despliegue (ALO-01): su id sale de {@code sga.alojamiento.id}, nunca de la URL ni del
 * cuerpo, así un cliente no puede operar sobre otro alojamiento (DEC-64).
 */
@Component
public class AlojamientoActual {

    private final String id;

    public AlojamientoActual(@Value("${sga.alojamiento.id:ALO-1}") String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
