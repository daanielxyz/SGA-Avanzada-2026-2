package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarBitacoraUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ConsultarBitacoraCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.DesactivarCanalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.ListarCanalesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints con los que el administrador gestiona los canales de venta y consulta su bitácora (CU-54 · CU-45). El
 * contrato que usan los propios canales está en {@link ContratoCanalController}. Solo traduce HTTP ⇄ caso de uso
 * (DEC-33).
 */
@RestController
@RequestMapping("/api/canales")
public class CanalController {

    private final AlojamientoActual alojamiento;
    private final ListarCanalesUseCase listarCanales;
    private final RegistrarCanalUseCase registrarCanal;
    private final DesactivarCanalUseCase desactivarCanal;
    private final ConsultarBitacoraUseCase consultarBitacora;

    public CanalController(AlojamientoActual alojamiento, ListarCanalesUseCase listarCanales,
                           RegistrarCanalUseCase registrarCanal, DesactivarCanalUseCase desactivarCanal,
                           ConsultarBitacoraUseCase consultarBitacora) {
        this.alojamiento = alojamiento;
        this.listarCanales = listarCanales;
        this.registrarCanal = registrarCanal;
        this.desactivarCanal = desactivarCanal;
        this.consultarBitacora = consultarBitacora;
    }

    @GetMapping
    public List<CanalResponse> listar() {
        return listarCanales.ejecutar(alojamiento.id()).stream().map(CanalResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CanalResponse registrar(@Valid @RequestBody RegistrarCanalRequest solicitud) {
        return CanalResponse.de(registrarCanal.ejecutar(solicitud.aCommand(alojamiento.id())));
    }

    @PutMapping("/{id}/desactivar")
    public CanalResponse desactivar(@PathVariable String id) {
        return CanalResponse.de(desactivarCanal.ejecutar(id));
    }

    /**
     * @param desde primer día, incluido
     * @param hasta último día, incluido
     */
    @GetMapping("/{id}/eventos")
    public PaginaResponse<EventoCanalResponse> consultarBitacora(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(consultarBitacora.ejecutar(new ConsultarBitacoraCommand(id, desde, hasta, pagina)),
                EventoCanalResponse::de);
    }
}
