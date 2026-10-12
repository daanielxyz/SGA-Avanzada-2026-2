package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.politica;

import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.ConsultarPoliticaVigente;
import co.edu.uniquindio.sga_avanzada_2026_2.application.politica.PublicarPolitica;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final PublicarPolitica publicarPolitica;
    private final ConsultarPoliticaVigente consultarVigente;

    public PoliticaController(AlojamientoActual alojamiento, PublicarPolitica publicarPolitica,
                              ConsultarPoliticaVigente consultarVigente) {
        this.alojamiento = alojamiento;
        this.publicarPolitica = publicarPolitica;
        this.consultarVigente = consultarVigente;
    }

    /** La versión publicada pasa a ser la vigente, así que esa es su ubicación. */
    @PostMapping
    public ResponseEntity<PoliticaResponse> publicar(@Valid @RequestBody PublicarPoliticaRequest solicitud) {
        PoliticaResponse publicada = PoliticaResponse.de(
                publicarPolitica.ejecutar(solicitud.aCommand(alojamiento.id())));
        return ResponseEntity.created(URI.create("/api/politicas/vigente")).body(publicada);
    }

    @GetMapping("/vigente")
    public PoliticaResponse consultarVigente() {
        return PoliticaResponse.de(consultarVigente.ejecutar(alojamiento.id()));
    }
}
