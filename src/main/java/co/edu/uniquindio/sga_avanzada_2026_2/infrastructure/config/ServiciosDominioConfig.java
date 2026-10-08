package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ActivadorApartamentoService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.BajaApartamentoDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.BloqueoOperativoDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CambioCapacidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.CancelacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.NoShowDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra como beans los servicios de dominio, que son Java puro y sin estado, para que los casos de uso los
 * reciban por constructor.
 */
@Configuration
public class ServiciosDominioConfig {

    @Bean
    public ActivadorApartamentoService activadorApartamentoService() {
        return new ActivadorApartamentoService();
    }

    @Bean
    public BajaApartamentoDomainService bajaApartamentoDomainService() {
        return new BajaApartamentoDomainService();
    }

    @Bean
    public BloqueoOperativoDomainService bloqueoOperativoDomainService() {
        return new BloqueoOperativoDomainService();
    }

    @Bean
    public CambioCapacidadDomainService cambioCapacidadDomainService() {
        return new CambioCapacidadDomainService();
    }

    @Bean
    public DisponibilidadDomainService disponibilidadDomainService() {
        return new DisponibilidadDomainService();
    }

    @Bean
    public TarificacionDomainService tarificacionDomainService() {
        return new TarificacionDomainService();
    }

    @Bean
    public CancelacionDomainService cancelacionDomainService() {
        return new CancelacionDomainService();
    }

    @Bean
    public NoShowDomainService noShowDomainService() {
        return new NoShowDomainService();
    }
}
