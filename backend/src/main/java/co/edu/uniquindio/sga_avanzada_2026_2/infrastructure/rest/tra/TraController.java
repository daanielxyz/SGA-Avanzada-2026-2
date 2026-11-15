package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tra.ConsultarEnvioTraUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tra.EnviarTraUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tra.ListarEnviosTraUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * El envío del TRA al Ministerio (DEC-130), para recepción y administración: en qué va el de una reserva,
 * «Reintentar» y la lista de los que fallaron. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
public class TraController {

    private final ConsultarEnvioTraUseCase consultarEnvio;
    private final EnviarTraUseCase enviarTra;
    private final ListarEnviosTraUseCase listarEnvios;

    public TraController(ConsultarEnvioTraUseCase consultarEnvio, EnviarTraUseCase enviarTra,
                         ListarEnviosTraUseCase listarEnvios) {
        this.consultarEnvio = consultarEnvio;
        this.enviarTra = enviarTra;
        this.listarEnvios = listarEnvios;
    }

    /** 404 mientras el grupo no haya llegado: el envío nace con la llegada. */
    @GetMapping("/api/reservas/{codigo}/tra")
    public EnvioTraResponse consultar(@PathVariable String codigo) {
        return EnvioTraResponse.de(consultarEnvio.ejecutar(codigo));
    }

    /**
     * Envía ya lo que falta. Responde 200 también si el Ministerio no lo recibió: el envío queda {@code CON_ERROR}
     * con el motivo. 409 si ya se envió, el grupo no ha llegado o faltan datos del TRA.
     */
    @PutMapping("/api/reservas/{codigo}/tra/enviar")
    public EnvioTraResponse enviar(@PathVariable String codigo) {
        return EnvioTraResponse.de(enviarTra.ejecutar(codigo));
    }

    /** Los envíos de un estado, del intento más reciente al más antiguo, de 10 en 10. */
    @GetMapping("/api/tra/envios")
    public PaginaResponse<EnvioTraResponse> listar(
            @RequestParam(defaultValue = "CON_ERROR")
            @Pattern(regexp = "PENDIENTE|ENVIADO|CON_ERROR", message = "debe ser PENDIENTE, ENVIADO o CON_ERROR")
            String estado,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarEnvios.ejecutar(estado, pagina), EnvioTraResponse::de);
    }
}
