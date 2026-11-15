package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.novedad;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.AvanzarNovedadUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.AvanzarNovedadCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.ConsultarNovedadesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.ListarNovedadesCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.ListarNovedadesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.NovedadResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.novedad.RegistrarNovedadUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de las novedades de los apartamentos (CU-43 · CU-44 · CU-55). El autor de cada paso es el usuario que
 * hace la petición (DEC-61), y cada respuesta trae su nombre (DEC-132). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
public class NovedadController {

    private final UsuarioActual usuario;
    private final RegistrarNovedadUseCase registrarNovedad;
    private final AvanzarNovedadUseCase avanzarNovedad;
    private final ConsultarNovedadesUseCase consultarNovedades;
    private final ListarNovedadesUseCase listarNovedades;
    private final DirectorioPersonal personal;

    public NovedadController(UsuarioActual usuario, RegistrarNovedadUseCase registrarNovedad,
                             AvanzarNovedadUseCase avanzarNovedad, ConsultarNovedadesUseCase consultarNovedades,
                             ListarNovedadesUseCase listarNovedades, DirectorioPersonal personal) {
        this.usuario = usuario;
        this.registrarNovedad = registrarNovedad;
        this.avanzarNovedad = avanzarNovedad;
        this.consultarNovedades = consultarNovedades;
        this.listarNovedades = listarNovedades;
        this.personal = personal;
    }

    @PostMapping("/api/apartamentos/{codigo}/novedades")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<NovedadResponse> registrar(@PathVariable String codigo,
                                     @Valid @RequestBody RegistrarNovedadRequest solicitud) {
        return ResponseEntity.created(URI.create("/api/apartamentos/" + codigo + "/novedades"))
                .body(respuesta(registrarNovedad.ejecutar(solicitud.aCommand(codigo, usuario.id()))));
    }

    /** De la más reciente a la más antigua, incluidas las cerradas (NOV-06). */
    @GetMapping("/api/apartamentos/{codigo}/novedades")
    public PaginaResponse<NovedadResponse> consultar(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(consultarNovedades.ejecutar(codigo, pagina), this::respuesta);
    }

    /**
     * Las novedades de todos los apartamentos, de la más reciente a la más antigua (DEC-132). Estado, gravedad y
     * apartamento admiten varios valores, repitiendo el parámetro o separados por comas, como en Reservas (DEC-119):
     * dentro de uno es «cualquiera de ellos» y entre filtros distintos, «y».
     */
    @GetMapping("/api/novedades")
    public PaginaResponse<NovedadResponse> listar(
            @RequestParam(required = false)
            List<@Pattern(regexp = "ABIERTA|EN_REVISION|CERRADA", message = "debe ser ABIERTA, EN_REVISION o CERRADA")
                    String> estado,
            @RequestParam(required = false)
            List<@Pattern(regexp = "BAJA|MEDIA|ALTA", message = "debe ser BAJA, MEDIA o ALTA") String> gravedad,
            @RequestParam(required = false)
            List<@Pattern(regexp = "APT-\\d+", message = "debe tener el formato APT-101") String> apartamento,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarNovedades.ejecutar(
                new ListarNovedadesCommand(estado, gravedad, apartamento, pagina)), this::respuesta);
    }

    /** Da el siguiente paso: ABIERTA → EN_REVISION → CERRADA. */
    @PutMapping("/api/novedades/{id}/avanzar")
    public NovedadResponse avanzar(@PathVariable String id) {
        return respuesta(avanzarNovedad.ejecutar(new AvanzarNovedadCommand(id, usuario.id())));
    }

    private NovedadResponse respuesta(NovedadResult novedad) {
        return NovedadResponse.de(novedad.conNombres(personal));
    }
}
