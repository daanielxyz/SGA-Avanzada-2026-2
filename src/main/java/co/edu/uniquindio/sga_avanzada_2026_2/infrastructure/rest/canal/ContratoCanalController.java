package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.IntegracionExternaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos.ReceptorReservaExterna;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;

/**
 * Contrato HTTP que usan los canales externos, no el personal (CAN-03 · CU-12). Responde según lo que pasó con la
 * reserva: 201 si se creó, 200 si ya se había recibido (RN-19) y 409 si no hay disponibilidad (RN-18); un rechazo por
 * otra regla es el 409 {@code REGLA_NEGOCIO} del manejador global (DEC-62).
 */
// TODO(B4): este endpoint aún no autentica al canal con su credencial (DEC-47 · DEC-66).
@RestController
@RequestMapping("/api/canales/{id}")
public class ContratoCanalController {

    private static final Map<String, HttpStatus> ESTADOS = Map.of(
            "EXITOSO", HttpStatus.CREATED,
            "DUPLICADO", HttpStatus.OK,
            "CONFLICTO", HttpStatus.CONFLICT);

    private final ReceptorReservaExterna receptor;
    private final ObjectMapper json;

    public ContratoCanalController(ReceptorReservaExterna receptor, ObjectMapper json) {
        this.receptor = receptor;
        this.json = json;
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaExternaResponse> recibirReserva(@PathVariable String id,
                                                                 @Valid @RequestBody ReservaExternaRequest solicitud) {
        IntegracionExternaResult resultado = receptor.recibir(
                solicitud.aCommand(id, json.writeValueAsString(solicitud)));
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(ESTADOS.get(resultado.resultado()));
        if (resultado.codigoReserva() != null) {
            respuesta.location(URI.create("/api/reservas/" + resultado.codigoReserva()));
        }
        return respuesta.body(ReservaExternaResponse.de(resultado));
    }
}
