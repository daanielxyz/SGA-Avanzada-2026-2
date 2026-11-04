package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarOcupacionUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Calendario de ocupación de recepción y administración (CU-24).
 */
@RestController
@RequestMapping("/api/ocupacion")
public class OcupacionController {

    private final AlojamientoActual alojamiento;
    private final ConsultarOcupacionUseCase consultarOcupacion;

    public OcupacionController(AlojamientoActual alojamiento, ConsultarOcupacionUseCase consultarOcupacion) {
        this.alojamiento = alojamiento;
        this.consultarOcupacion = consultarOcupacion;
    }

    /**
     * @param hasta       exclusiva (EST-01)
     * @param apartamento opcional: solo ese apartamento
     */
    @GetMapping
    public PaginaResponse<OcupacionResponse> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String apartamento,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(consultarOcupacion.ejecutar(alojamiento.id(), desde, hasta, apartamento, pagina),
                OcupacionResponse::de);
    }
}
