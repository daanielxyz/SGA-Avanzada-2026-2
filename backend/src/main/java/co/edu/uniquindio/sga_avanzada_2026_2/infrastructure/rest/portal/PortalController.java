package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.LiberarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.PagarEnLineaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaPortalResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservarDesdePortalUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Reserva desde el portal y pago en línea (CU-10 · DEC-69). Son públicos: quien reserva no necesita cuenta, porque
 * la reserva se la crea (DEC-68). Para pagar o liberar la reserva se necesita la clave que se recibió al reservar, o
 * una sesión de su huésped para pagar (DEC-93).
 */
@RestController
@RequestMapping("/api/portal/reservas")
public class PortalController {

    /** La clave de la reserva (DEC-93): en una cabecera y no en la URL, para que no quede en los registros. */
    static final String CABECERA_CLAVE = "X-Reserva-Clave";

    private final ReservarDesdePortalUseCase reservarDesdePortal;
    private final PagarEnLineaUseCase pagarEnLinea;
    private final LiberarReservaUseCase liberarReserva;
    private final AccesoReservaPortal acceso;

    public PortalController(ReservarDesdePortalUseCase reservarDesdePortal, PagarEnLineaUseCase pagarEnLinea,
                            LiberarReservaUseCase liberarReserva, AccesoReservaPortal acceso) {
        this.reservarDesdePortal = reservarDesdePortal;
        this.pagarEnLinea = pagarEnLinea;
        this.liberarReserva = liberarReserva;
        this.acceso = acceso;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReservaPortalResponse> reservar(@Valid @RequestBody ReservaPortalRequest solicitud) {
        ReservaPortalResult creada = reservarDesdePortal.ejecutar(solicitud.aCommand());
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.reserva().codigo()))
                .body(ReservaPortalResponse.de(creada));
    }

    /**
     * Si la pasarela aprueba, la reserva queda CONFIRMADA; si rechaza, 409 {@code PAGO_RECHAZADO} y sigue igual.
     * Pide la clave o la sesión del huésped dueño; sin ellas, 403 antes de cobrar (DEC-93).
     */
    @PostMapping("/{codigo}/pago")
    public PagoPortalResponse pagar(@PathVariable String codigo,
                                    @RequestHeader(name = CABECERA_CLAVE, required = false) String clave,
                                    @Valid @RequestBody PagoEnLineaRequest solicitud) {
        acceso.exigirClaveOSesion(codigo, clave);
        return PagoPortalResponse.de(pagarEnLinea.ejecutar(solicitud.aCommand(codigo)));
    }

    /**
     * Quien reservó vuelve desde el pago: la reserva PENDIENTE se cancela sin penalidad y sus noches quedan libres
     * (DEC-93). Solo con la clave; el huésped con sesión cancela con la política, en {@code PUT /api/reservas/…}.
     */
    @PutMapping("/{codigo}/liberar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@PathVariable String codigo,
                        @RequestHeader(name = CABECERA_CLAVE, required = false) String clave) {
        acceso.exigirClave(codigo, clave);
        liberarReserva.ejecutar(codigo);
    }
}
