package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.AtenderNotaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.AtenderNotaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.EscribirNotaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ListarNotasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.NotaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Endpoints de las notas de una reserva para el siguiente turno (BL-08 · DEC-105). Son del personal: el huésped no
 * llega aquí ({@code PermisosPorRol}). Cada respuesta trae todas las notas de la reserva, de la más reciente a la más
 * antigua. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/reservas/{codigo}/notas")
public class NotaReservaController {

    private final UsuarioActual usuario;
    private final ListarNotasUseCase listarNotas;
    private final EscribirNotaUseCase escribirNota;
    private final AtenderNotaUseCase atenderNota;

    public NotaReservaController(UsuarioActual usuario, ListarNotasUseCase listarNotas,
                                 EscribirNotaUseCase escribirNota, AtenderNotaUseCase atenderNota) {
        this.usuario = usuario;
        this.listarNotas = listarNotas;
        this.escribirNota = escribirNota;
        this.atenderNota = atenderNota;
    }

    private static List<NotaResponse> respuesta(List<NotaResult> notas) {
        return notas.stream().map(NotaResponse::de).toList();
    }

    @GetMapping
    public List<NotaResponse> listarNotas(@PathVariable String codigo) {
        return respuesta(listarNotas.ejecutar(codigo));
    }

    /** El autor sale de la sesión (DEC-67). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<List<NotaResponse>> escribirNota(@PathVariable String codigo,
                                                           @Valid @RequestBody EscribirNotaRequest solicitud) {
        return ResponseEntity.created(URI.create("/api/reservas/" + codigo + "/notas"))
                .body(respuesta(escribirNota.ejecutar(solicitud.aCommand(codigo, usuario.id()))));
    }

    /** Queda quién la atendió y cuándo; deja de salir como pendiente. */
    @PutMapping("/{id}/atender")
    public List<NotaResponse> atenderNota(@PathVariable String codigo, @PathVariable String id) {
        return respuesta(atenderNota.ejecutar(new AtenderNotaCommand(codigo, id, usuario.id())));
    }
}
