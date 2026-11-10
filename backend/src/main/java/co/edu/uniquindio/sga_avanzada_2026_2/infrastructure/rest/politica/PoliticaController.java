package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.politica;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Min;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.ListarVersionesPoliticaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.ConsultarPoliticaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.ConsultarPoliticaVigenteUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPoliticaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints de la política de cancelación del alojamiento. Las versiones no se editan: publicar crea la siguiente
 * (DEC-41). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/politicas")
public class PoliticaController {

    private final AlojamientoActual alojamiento;
    private final PublicarPoliticaUseCase publicarPolitica;
    private final ConsultarPoliticaVigenteUseCase consultarVigente;
    private final ListarVersionesPoliticaUseCase listarVersiones;
    private final ConsultarPoliticaUseCase consultarPolitica;

    public PoliticaController(AlojamientoActual alojamiento, PublicarPoliticaUseCase publicarPolitica,
                              ConsultarPoliticaVigenteUseCase consultarVigente,
                              ListarVersionesPoliticaUseCase listarVersiones,
                              ConsultarPoliticaUseCase consultarPolitica) {
        this.alojamiento = alojamiento;
        this.publicarPolitica = publicarPolitica;
        this.consultarVigente = consultarVigente;
        this.listarVersiones = listarVersiones;
        this.consultarPolitica = consultarPolitica;
    }

    /** La versión publicada pasa a ser la vigente, así que esa es su ubicación. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PoliticaResponse> publicar(@Valid @RequestBody PublicarPoliticaRequest solicitud) {
        PoliticaResponse publicada = PoliticaResponse.de(
                publicarPolitica.ejecutar(solicitud.aCommand(alojamiento.id())));
        return ResponseEntity.created(URI.create("/api/politicas/vigente")).body(publicada);
    }

    /** Todas las versiones, de la más alta a la más baja (POL-04). */
    @GetMapping
    public PaginaResponse<PoliticaResponse> listarVersiones(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarVersiones.ejecutar(alojamiento.id(), pagina), PoliticaResponse::de);
    }

    @GetMapping("/vigente")
    public PoliticaResponse consultarVigente() {
        return PoliticaResponse.de(consultarVigente.ejecutar(alojamiento.id()));
    }

    /** La versión congelada en una reserva, con el {@code politicaId} de su detalle (RN-13 · DEC-104). */
    @GetMapping("/{id}")
    public PoliticaResponse consultarVersion(@PathVariable String id) {
        return PoliticaResponse.de(consultarPolitica.ejecutar(alojamiento.id(), id));
    }
}
