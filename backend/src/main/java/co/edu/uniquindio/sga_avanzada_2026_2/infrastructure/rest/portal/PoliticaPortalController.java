package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.ConsultarPoliticaVigenteUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La política de cancelación que el huésped ve antes de pagar, sin sesión (DEC-41). Solo traduce HTTP ⇄ caso de uso
 * (DEC-33).
 */
@RestController
public class PoliticaPortalController {

    private final AlojamientoActual alojamiento;
    private final ConsultarPoliticaVigenteUseCase consultarPolitica;

    public PoliticaPortalController(AlojamientoActual alojamiento, ConsultarPoliticaVigenteUseCase consultarPolitica) {
        this.alojamiento = alojamiento;
        this.consultarPolitica = consultarPolitica;
    }

    @GetMapping("/api/portal/politica")
    public PoliticaPortalResponse consultar() {
        return PoliticaPortalResponse.de(consultarPolitica.ejecutar(alojamiento.id()));
    }
}
