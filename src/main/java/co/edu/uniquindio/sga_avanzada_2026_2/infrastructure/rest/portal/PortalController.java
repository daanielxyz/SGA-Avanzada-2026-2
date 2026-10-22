package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.PagarEnLineaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaPortalResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservarDesdePortalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.ReservaResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Reserva desde el portal y pago en línea (CU-10 · DEC-69). Son públicos: quien reserva no necesita cuenta, porque
 * la reserva se la crea (DEC-68), y quien paga solo necesita el código de la reserva.
 */
@RestController
@RequestMapping("/api/portal/reservas")
public class PortalController {

    private final ReservarDesdePortalUseCase reservarDesdePortal;
    private final PagarEnLineaUseCase pagarEnLinea;

    public PortalController(ReservarDesdePortalUseCase reservarDesdePortal, PagarEnLineaUseCase pagarEnLinea) {
        this.reservarDesdePortal = reservarDesdePortal;
        this.pagarEnLinea = pagarEnLinea;
    }

    @PostMapping
    public ResponseEntity<ReservaPortalResponse> reservar(@Valid @RequestBody ReservaPortalRequest solicitud) {
        ReservaPortalResult creada = reservarDesdePortal.ejecutar(solicitud.aCommand());
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.reserva().codigo()))
                .body(ReservaPortalResponse.de(creada));
    }

    /** Si la pasarela aprueba, la reserva queda CONFIRMADA; si rechaza, 409 {@code PAGO_RECHAZADO} y sigue igual. */
    @PostMapping("/{codigo}/pago")
    public ReservaResponse pagar(@PathVariable String codigo, @Valid @RequestBody PagoEnLineaRequest solicitud) {
        return ReservaResponse.de(pagarEnLinea.ejecutar(solicitud.aCommand(codigo)));
    }
}
