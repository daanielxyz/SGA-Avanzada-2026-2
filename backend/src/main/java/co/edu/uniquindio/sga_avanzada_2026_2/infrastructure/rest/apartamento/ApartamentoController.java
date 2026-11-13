package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.DeclararFueraDeServicioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.FijarLimiteMascotasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.MarcarPendientePreparacionUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.MarcarPreparadoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.IniciarPreparacionUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ListarPendientesPreparacionUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActualizarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActivarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CambiarCapacidadUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ConsultarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.LevantarBloqueoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.LevantarBloqueoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ListarApartamentosUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RetirarApartamentoUseCase;
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
    private final ActualizarApartamentoUseCase actualizarApartamento;
    private final ListarPendientesPreparacionUseCase listarPendientes;
    private final CrearApartamentoUseCase crearApartamento;
    private final ConsultarApartamentoUseCase consultarApartamento;
    private final ListarApartamentosUseCase listarApartamentos;
    private final ActivarApartamentoUseCase activarApartamento;
    private final RetirarApartamentoUseCase retirarApartamento;
    private final CambiarCapacidadUseCase cambiarCapacidad;
    private final IniciarPreparacionUseCase iniciarPreparacion;
    private final MarcarPreparadoUseCase marcarPreparado;
    private final MarcarPendientePreparacionUseCase marcarPendientePreparacion;
    private final DeclararFueraDeServicioUseCase declararFueraDeServicio;
    private final RegistrarBloqueoUseCase registrarBloqueo;
    private final LevantarBloqueoUseCase levantarBloqueo;
    private final FijarLimiteMascotasUseCase fijarLimiteMascotas;

    public ApartamentoController(ActualizarApartamentoUseCase actualizarApartamento,
                                 ListarPendientesPreparacionUseCase listarPendientes,
                                 AlojamientoActual alojamiento, UsuarioActual usuario,
                                 CrearApartamentoUseCase crearApartamento,
                                 ConsultarApartamentoUseCase consultarApartamento,
                                 ListarApartamentosUseCase listarApartamentos,
                                 ActivarApartamentoUseCase activarApartamento,
                                 RetirarApartamentoUseCase retirarApartamento,
                                 CambiarCapacidadUseCase cambiarCapacidad,
                                 IniciarPreparacionUseCase iniciarPreparacion,
                                 MarcarPreparadoUseCase marcarPreparado,
                                 MarcarPendientePreparacionUseCase marcarPendientePreparacion,
                                 DeclararFueraDeServicioUseCase declararFueraDeServicio,
                                 RegistrarBloqueoUseCase registrarBloqueo, LevantarBloqueoUseCase levantarBloqueo,
                                 FijarLimiteMascotasUseCase fijarLimiteMascotas) {
        this.alojamiento = alojamiento;
        this.usuario = usuario;
        this.actualizarApartamento = actualizarApartamento;
        this.listarPendientes = listarPendientes;
        this.crearApartamento = crearApartamento;
        this.consultarApartamento = consultarApartamento;
        this.listarApartamentos = listarApartamentos;
        this.activarApartamento = activarApartamento;
        this.retirarApartamento = retirarApartamento;
        this.cambiarCapacidad = cambiarCapacidad;
        this.iniciarPreparacion = iniciarPreparacion;
        this.marcarPreparado = marcarPreparado;
        this.marcarPendientePreparacion = marcarPendientePreparacion;
        this.declararFueraDeServicio = declararFueraDeServicio;
        this.registrarBloqueo = registrarBloqueo;
        this.levantarBloqueo = levantarBloqueo;
        this.fijarLimiteMascotas = fijarLimiteMascotas;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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

    /** Lo que falta por preparar, primero lo de la ocupación más cercana (CU-41 · CU-42 · DEC-111). */
    @GetMapping("/pendientes-preparacion")
    public PaginaResponse<ApartamentoResponse> listarPendientesPreparacion(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarPendientes.ejecutar(alojamiento.id(), pagina), ApartamentoResponse::de);
    }

    @PutMapping("/{codigo}")
    public ApartamentoResponse actualizar(@PathVariable String codigo,
                                          @Valid @RequestBody ActualizarApartamentoRequest solicitud) {
        return ApartamentoResponse.de(actualizarApartamento.ejecutar(solicitud.aCommand(codigo)));
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

    /** Sin valor, el apartamento vuelve al límite de mascotas del alojamiento (DEC-93). */
    @PutMapping("/{codigo}/limite-mascotas")
    public ApartamentoResponse fijarLimiteMascotas(@PathVariable String codigo,
                                                   @Valid @RequestBody LimiteMascotasRequest solicitud) {
        return ApartamentoResponse.de(fijarLimiteMascotas.ejecutar(solicitud.aCommand(codigo)));
    }

    /**
     * El personal de servicio solo mueve el apartamento por la ruta de preparación; declararlo fuera de servicio,
     * o devolverlo de ahí, es del administrador (EOPE-04 · DEC-22).
     */
    @PutMapping("/{codigo}/iniciar-preparacion")
    public ApartamentoResponse iniciarPreparacion(@PathVariable String codigo) {
        exigirPermiso(codigo, "EN_PREPARACION");
        return ApartamentoResponse.de(iniciarPreparacion.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/marcar-preparado")
    public ApartamentoResponse marcarPreparado(@PathVariable String codigo) {
        exigirPermiso(codigo, "PREPARADO");
        return ApartamentoResponse.de(marcarPreparado.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/marcar-pendiente-preparacion")
    public ApartamentoResponse marcarPendientePreparacion(@PathVariable String codigo) {
        exigirPermiso(codigo, "PENDIENTE_PREPARACION");
        return ApartamentoResponse.de(marcarPendientePreparacion.ejecutar(codigo));
    }

    @PutMapping("/{codigo}/declarar-fuera-de-servicio")
    public ApartamentoResponse declararFueraDeServicio(@PathVariable String codigo) {
        exigirPermiso(codigo, "FUERA_DE_SERVICIO");
        return ApartamentoResponse.de(declararFueraDeServicio.ejecutar(codigo));
    }

    private void exigirPermiso(String codigo, String destino) {
        String actual = consultarApartamento.ejecutar(codigo).estadoOperativo();
        if (!RUTA_DE_PREPARACION.contains(actual) || !RUTA_DE_PREPARACION.contains(destino)) {
            usuario.exigirAdministrador("sacar un apartamento de la ruta de preparación o devolverlo a ella");
        }
    }

    @PostMapping("/{codigo}/bloqueos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ApartamentoResponse> registrarBloqueo(@PathVariable String codigo,
                                                @Valid @RequestBody RegistrarBloqueoRequest solicitud) {
        return ResponseEntity.created(URI.create("/api/apartamentos/" + codigo))
                .body(ApartamentoResponse.de(registrarBloqueo.ejecutar(solicitud.aCommand(codigo))));
    }

    @PutMapping("/{codigo}/bloqueos/{id}/levantar")
    public ApartamentoResponse levantarBloqueo(@PathVariable String codigo, @PathVariable String id) {
        return ApartamentoResponse.de(levantarBloqueo.ejecutar(new LevantarBloqueoCommand(codigo, id)));
    }
}
