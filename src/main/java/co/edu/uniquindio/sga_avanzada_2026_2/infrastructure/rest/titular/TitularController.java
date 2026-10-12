package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ActualizarTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ConsultarTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.RegistrarTitular;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.TitularResult;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Endpoints de titulares. Al crear una reserva el titular se registra o se reutiliza solo (DEC-58); estos sirven
 * para registrarlo antes o corregir sus datos. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/titulares")
public class TitularController {

    private final AlojamientoActual alojamiento;
    private final RegistrarTitular registrarTitular;
    private final ConsultarTitular consultarTitular;
    private final ActualizarTitular actualizarTitular;

    public TitularController(AlojamientoActual alojamiento, RegistrarTitular registrarTitular,
                             ConsultarTitular consultarTitular, ActualizarTitular actualizarTitular) {
        this.alojamiento = alojamiento;
        this.registrarTitular = registrarTitular;
        this.consultarTitular = consultarTitular;
        this.actualizarTitular = actualizarTitular;
    }

    @PostMapping
    public ResponseEntity<TitularResponse> registrar(@Valid @RequestBody RegistrarTitularRequest solicitud) {
        TitularResult registrado = registrarTitular.ejecutar(solicitud.aCommand(alojamiento.id()));
        return ResponseEntity.created(URI.create("/api/titulares/" + registrado.id()))
                .body(TitularResponse.de(registrado));
    }

    @GetMapping("/{id}")
    public TitularResponse consultar(@PathVariable String id) {
        return TitularResponse.de(consultarTitular.ejecutar(id));
    }

    @PutMapping("/{id}")
    public TitularResponse actualizar(@PathVariable String id,
                                      @Valid @RequestBody ActualizarTitularRequest solicitud) {
        return TitularResponse.de(actualizarTitular.ejecutar(solicitud.aCommand(id)));
    }
}
