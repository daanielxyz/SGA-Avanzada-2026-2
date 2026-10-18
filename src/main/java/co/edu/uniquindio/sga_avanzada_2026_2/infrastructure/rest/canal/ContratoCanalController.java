package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CancelarReservaExternaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarDisponibilidadCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.ReceptorCanalExterno;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.ApartamentoDisponibleResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.CancelacionResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.AutenticadorCanal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * El contrato con los canales externos (CU-12 · CU-13 · CU-16 · DEC-73), fuera de los endpoints de usuarios: cada
 * mensaje trae la clave del canal en {@code X-Canal-Clave}, nunca un token JWT. Sin clave válida responde 401
 * {@code CANAL_NO_AUTENTICADO}. Solo traduce HTTP ⇄ adaptador (DEC-33).
 */
@RestController
@RequestMapping("/api/canales-externos/{canalId}")
public class ContratoCanalController {

    private static final Map<String, HttpStatus> ESTADOS = Map.of(
            "EXITOSO", HttpStatus.CREATED,
            "DUPLICADO", HttpStatus.OK,
            "CONFLICTO", HttpStatus.CONFLICT);

    private final ReceptorCanalExterno receptor;
    private final ObjectMapper json;

    public ContratoCanalController(ReceptorCanalExterno receptor, ObjectMapper json) {
        this.receptor = receptor;
        this.json = json;
    }

    /** CU-13: los apartamentos libres para esas fechas y ese grupo. */
    @GetMapping("/disponibilidad")
    public List<ApartamentoDisponibleResponse> consultarDisponibilidad(
            @PathVariable String canalId,
            @RequestHeader(name = AutenticadorCanal.ENCABEZADO, required = false) String clave,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam @Min(value = 1, message = "debe ser al menos 1") int ocupantes) {
        String consulta = json.writeValueAsString(Map.of("entrada", entrada.toString(), "salida", salida.toString(),
                "ocupantes", ocupantes));
        return receptor.consultarDisponibilidad(clave,
                        new ConsultarDisponibilidadCanalCommand(canalId, entrada, salida, ocupantes, consulta))
                .stream()
                .map(ApartamentoDisponibleResponse::de)
                .toList();
    }

    /** CU-12: 201 si la crea, 200 si ya la tenía (RN-19), 409 si choca con otra (RN-18). */
    @PostMapping("/reservas")
    public ResponseEntity<ReservaExternaResponse> recibirReserva(
            @PathVariable String canalId,
            @RequestHeader(name = AutenticadorCanal.ENCABEZADO, required = false) String clave,
            @Valid @RequestBody ReservaExternaRequest solicitud) {
        IntegracionExternaResult resultado = receptor.recibir(clave,
                solicitud.aCommand(canalId, json.writeValueAsString(solicitud)));
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(ESTADOS.get(resultado.resultado()));
        if (resultado.codigoReserva() != null) {
            respuesta.location(URI.create("/api/reservas/" + resultado.codigoReserva()));
        }
        return respuesta.body(ReservaExternaResponse.de(resultado));
    }

    /** CU-16: cancela una reserva que trajo este mismo canal, con la política congelada (RN-12 · RN-13). */
    @PutMapping("/reservas/{idExterno}/cancelar")
    public CancelacionResponse cancelarReserva(
            @PathVariable String canalId, @PathVariable String idExterno,
            @RequestHeader(name = AutenticadorCanal.ENCABEZADO, required = false) String clave) {
        return CancelacionResponse.de(receptor.cancelar(clave, new CancelarReservaExternaCommand(canalId, idExterno,
                json.writeValueAsString(Map.of("idExterno", idExterno)))));
    }
}
