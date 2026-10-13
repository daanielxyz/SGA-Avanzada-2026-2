package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActivarApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarCapacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarEstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ConsultarApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.LevantarBloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.LevantarBloqueoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ListarApartamentos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RegistrarBloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RetirarApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

/**
 * Endpoints de apartamentos. Solo traduce HTTP ⇄ caso de uso; los errores los responde el manejador global
 * (DEC-33).
 */
@RestController
@RequestMapping("/api/apartamentos")
public class ApartamentoController {

    /** Los estados entre los que se mueve el personal de servicio (EOPE-04). */
    private static final Set<String> RUTA_DE_PREPARACION =
            Set.of("PENDIENTE_PREPARACION", "EN_PREPARACION", "PREPARADO");

    private final AlojamientoActual alojamiento;
    private final UsuarioActual usuario;
    private final CrearApartamento crearApartamento;
    private final ConsultarApartamento consultarApartamento;
    private final ListarApartamentos listarApartamentos;
    private final ActivarApartamento activarApartamento;
    private final RetirarApartamento retirarApartamento;
    private final CambiarCapacidad cambiarCapacidad;
    private final CambiarEstadoOperativo cambiarEstadoOperativo;
    private final RegistrarBloqueo registrarBloqueo;
    private final LevantarBloqueo levantarBloqueo;

    public ApartamentoController(AlojamientoActual alojamiento, UsuarioActual usuario,
                                 CrearApartamento crearApartamento, ConsultarApartamento consultarApartamento,
                                 ListarApartamentos listarApartamentos,
                                 ActivarApartamento activarApartamento, RetirarApartamento retirarApartamento,
                                 CambiarCapacidad cambiarCapacidad, CambiarEstadoOperativo cambiarEstadoOperativo,
                                 RegistrarBloqueo registrarBloqueo, LevantarBloqueo levantarBloqueo) {
        this.alojamiento = alojamiento;
        this.usuario = usuario;
        this.crearApartamento = crearApartamento;
        this.consultarApartamento = consultarApartamento;
        this.listarApartamentos = listarApartamentos;
        this.activarApartamento = activarApartamento;
        this.retirarApartamento = retirarApartamento;
        this.cambiarCapacidad = cambiarCapacidad;
        this.cambiarEstadoOperativo = cambiarEstadoOperativo;
        this.registrarBloqueo = registrarBloqueo;
        this.levantarBloqueo = levantarBloqueo;
    }

    @PostMapping
    public ResponseEntity<ApartamentoResponse> crear(@Valid @RequestBody CrearApartamentoRequest solicitud) {
        ApartamentoResult creado = crearApartamento.ejecutar(solicitud.aCommand(alojamiento.id()));
        return ResponseEntity.created(URI.create("/api/apartamentos/" + creado.codigo()))
                .body(ApartamentoResponse.de(creado));
    }

    @GetMapping("/{codigo}")
    public ApartamentoResponse consultar(@PathVariable String codigo) {
        return ApartamentoResponse.de(consultarApartamento.ejecutar(codigo));
    }

    /** Listado del administrador: incluye los inactivos (CU-31). */
    @GetMapping
    public PaginaResponse<ApartamentoResponse> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarApartamentos.ejecutar(alojamiento.id(), pagina), ApartamentoResponse::de);
    }

    @PutMapping("/{codigo}/activar")
    public ApartamentoResponse activar(@PathVariable String codigo) {
        return ApartamentoResponse.de(activarApartamento.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/retirar")
    public ApartamentoResponse retirar(@PathVariable String codigo) {
        return ApartamentoResponse.de(retirarApartamento.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/capacidad")
    public CambioCapacidadResponse cambiarCapacidad(@PathVariable String codigo,
                                                    @Valid @RequestBody CambiarCapacidadRequest solicitud) {
        return CambioCapacidadResponse.de(cambiarCapacidad.ejecutar(solicitud.aCommand(codigo)));
    }

    /**
     * El personal de servicio solo mueve el apartamento por la ruta de preparación; declararlo fuera de servicio,
     * o devolverlo de ahí, es del administrador (EOPE-04 · DEC-22).
     */
    @PutMapping("/{codigo}/estado-operativo")
    public ApartamentoResponse cambiarEstadoOperativo(@PathVariable String codigo,
                                                      @Valid @RequestBody CambiarEstadoOperativoRequest solicitud) {
        String actual = consultarApartamento.ejecutar(codigo).estadoOperativo();
        if (!RUTA_DE_PREPARACION.contains(actual) || !RUTA_DE_PREPARACION.contains(solicitud.estado())) {
            usuario.exigirAdministrador("sacar un apartamento de la ruta de preparación o devolverlo a ella");
        }
        return ApartamentoResponse.de(cambiarEstadoOperativo.ejecutar(solicitud.aCommand(codigo)));
    }

    @PostMapping("/{codigo}/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApartamentoResponse registrarBloqueo(@PathVariable String codigo,
                                                @Valid @RequestBody RegistrarBloqueoRequest solicitud) {
        return ApartamentoResponse.de(registrarBloqueo.ejecutar(solicitud.aCommand(codigo)));
    }

    @PutMapping("/{codigo}/bloqueos/{id}/levantar")
    public ApartamentoResponse levantarBloqueo(@PathVariable String codigo, @PathVariable String id) {
        return ApartamentoResponse.de(levantarBloqueo.ejecutar(new LevantarBloqueoCommand(codigo, id)));
    }
}
