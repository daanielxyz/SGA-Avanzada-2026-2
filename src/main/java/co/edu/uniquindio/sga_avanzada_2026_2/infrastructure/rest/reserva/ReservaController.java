package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConfirmarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.DeclararNoShow;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegada;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ModificarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints del ciclo de la reserva antes de la llegada: crear, consultar, modificar, confirmar, cancelar y no-show.
 * La llegada y la salida están en {@link EstanciaController}. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final CrearReserva crearReserva;
    private final ConsultarReserva consultarReserva;
    private final ModificarReserva modificarReserva;
    private final IndicarHoraLlegada indicarHoraLlegada;
    private final ConfirmarReserva confirmarReserva;
    private final CancelarReserva cancelarReserva;
    private final DeclararNoShow declararNoShow;

    public ReservaController(CrearReserva crearReserva, ConsultarReserva consultarReserva,
                             ModificarReserva modificarReserva, IndicarHoraLlegada indicarHoraLlegada,
                             ConfirmarReserva confirmarReserva, CancelarReserva cancelarReserva,
                             DeclararNoShow declararNoShow) {
        this.crearReserva = crearReserva;
        this.consultarReserva = consultarReserva;
        this.modificarReserva = modificarReserva;
        this.indicarHoraLlegada = indicarHoraLlegada;
        this.confirmarReserva = confirmarReserva;
        this.cancelarReserva = cancelarReserva;
        this.declararNoShow = declararNoShow;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest solicitud) {
        ReservaResult creada = crearReserva.ejecutar(solicitud.aCommand());
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.codigo()))
                .body(ReservaResponse.de(creada));
    }

    @GetMapping("/{codigo}")
    public ReservaResponse consultar(@PathVariable String codigo) {
        return ReservaResponse.de(consultarReserva.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/modificar")
    public ReservaResponse modificar(@PathVariable String codigo,
                                     @Valid @RequestBody ModificarReservaRequest solicitud) {
        return ReservaResponse.de(modificarReserva.ejecutar(solicitud.aCommand(codigo)));
    }

    @PutMapping("/{codigo}/hora-llegada")
    public ReservaResponse indicarHoraLlegada(@PathVariable String codigo,
                                              @Valid @RequestBody HoraLlegadaRequest solicitud) {
        return ReservaResponse.de(indicarHoraLlegada.ejecutar(solicitud.aCommand(codigo)));
    }

    @PutMapping("/{codigo}/confirmar")
    public ReservaResponse confirmar(@PathVariable String codigo) {
        return ReservaResponse.de(confirmarReserva.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/cancelar")
    public CancelacionResponse cancelar(@PathVariable String codigo) {
        return CancelacionResponse.de(cancelarReserva.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/no-show")
    public CancelacionResponse declararNoShow(@PathVariable String codigo) {
        return CancelacionResponse.de(declararNoShow.ejecutar(codigo));
    }
}
