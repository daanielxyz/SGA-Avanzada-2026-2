package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CambiarExtrasReservaUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cambiar solo los vehículos y las mascotas de una reserva, sin volver a cotizar las noches (R3 · DEC-116).
 * Recepción y administración, por la regla de {@code /api/reservas/**}. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
public class ExtrasReservaController {

    private final CambiarExtrasReservaUseCase cambiarExtras;

    public ExtrasReservaController(CambiarExtrasReservaUseCase cambiarExtras) {
        this.cambiarExtras = cambiarExtras;
    }

    /**
     * Antes de la llegada (PENDIENTE o CONFIRMADA); la diferencia de los extras entra al folio como ajuste «Cambio de
     * vehículos y mascotas». Una reserva en curso responde 409: lo de más se carga como consumo.
     */
    @PutMapping("/api/reservas/{codigo}/extras")
    public ReservaResponse cambiarExtras(@PathVariable String codigo,
                                         @Valid @RequestBody CambiarExtrasRequest solicitud) {
        return ReservaResponse.de(cambiarExtras.ejecutar(solicitud.aCommand(codigo)));
    }
}
