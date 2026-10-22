package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CancelarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConfirmarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.DeclararNoShowUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ListarReservasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ListarReservasCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ModificarReservaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ReservaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

/**
 * Endpoints del ciclo de la reserva antes de la llegada: crear, consultar, modificar, confirmar, cancelar y no-show.
 * La llegada y la salida están en {@link EstanciaController}. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final UsuarioActual usuario;
    private final CrearReservaUseCase crearReserva;
    private final ConsultarReservaUseCase consultarReserva;
    private final ModificarReservaUseCase modificarReserva;
    private final IndicarHoraLlegadaUseCase indicarHoraLlegada;
    private final ConfirmarReservaUseCase confirmarReserva;
    private final CancelarReservaUseCase cancelarReserva;
    private final DeclararNoShowUseCase declararNoShow;
    private final ListarReservasUseCase listarReservas;

    public ReservaController(UsuarioActual usuario, CrearReservaUseCase crearReserva,
                             ConsultarReservaUseCase consultarReserva, ModificarReservaUseCase modificarReserva,
                             IndicarHoraLlegadaUseCase indicarHoraLlegada, ConfirmarReservaUseCase confirmarReserva,
                             CancelarReservaUseCase cancelarReserva, DeclararNoShowUseCase declararNoShow,
                             ListarReservasUseCase listarReservas) {
        this.usuario = usuario;
        this.crearReserva = crearReserva;
        this.consultarReserva = consultarReserva;
        this.modificarReserva = modificarReserva;
        this.indicarHoraLlegada = indicarHoraLlegada;
        this.confirmarReserva = confirmarReserva;
        this.cancelarReserva = cancelarReserva;
        this.declararNoShow = declararNoShow;
        this.listarReservas = listarReservas;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest solicitud) {
        ReservaResult creada = crearReserva.ejecutar(solicitud.aCommand());
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.codigo()))
                .body(ReservaResponse.de(creada));
    }

    /** El huésped solo ve las suyas (USU-04). */
    /** Solo recepción y administración; el huésped ve las suyas en {@code /api/mis-reservas} (DEC-72). */
    @GetMapping
    public PaginaResponse<ReservaResponse> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false)
            @Pattern(regexp = "PENDIENTE|CONFIRMADA|EN_CURSO|FINALIZADA|CANCELADA|NO_SHOW",
                    message = "debe ser PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA o NO_SHOW")
            String estado,
            @RequestParam(required = false) String apartamento,
            @RequestParam(required = false)
            @Pattern(regexp = "PORTAL|DIRECTO|EXTERNO", message = "debe ser PORTAL, DIRECTO o EXTERNO") String canal,
            @RequestParam(required = false) String titular,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarReservas.ejecutar(new ListarReservasCommand(desde, hasta, estado, apartamento,
                canal, titular, pagina)), ReservaResponse::de);
    }

    @GetMapping("/{codigo}")
    public ReservaResponse consultar(@PathVariable String codigo) {
        ReservaResult reserva = consultarReserva.ejecutar(codigo);
        usuario.exigirAccesoA(reserva.titularId());
        return ReservaResponse.de(reserva);
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

    /** El huésped solo cancela las suyas (USU-04 · CU-14). */
    @PutMapping("/{codigo}/cancelar")
    public CancelacionResponse cancelar(@PathVariable String codigo) {
        usuario.exigirAccesoA(consultarReserva.ejecutar(codigo).titularId());
        return CancelacionResponse.de(cancelarReserva.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/no-show")
    public CancelacionResponse declararNoShow(@PathVariable String codigo) {
        return CancelacionResponse.de(declararNoShow.ejecutar(codigo));
    }
}
