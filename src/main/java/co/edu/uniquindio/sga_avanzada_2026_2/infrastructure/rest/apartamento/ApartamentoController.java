package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ConsultarApartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamento;
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
 * Endpoints de apartamentos. Solo traduce HTTP ⇄ caso de uso; los errores los responde el manejador global
 * (DEC-33).
 */
@RestController
@RequestMapping("/api/apartamentos")
public class ApartamentoController {

    private final CrearApartamento crearApartamento;
    private final ConsultarApartamento consultarApartamento;

    public ApartamentoController(CrearApartamento crearApartamento, ConsultarApartamento consultarApartamento) {
        this.crearApartamento = crearApartamento;
        this.consultarApartamento = consultarApartamento;
    }

    @PostMapping
    public ResponseEntity<ApartamentoResponse> crear(@Valid @RequestBody CrearApartamentoRequest solicitud) {
        ApartamentoResult creado = crearApartamento.ejecutar(solicitud.aCommand());
        return ResponseEntity.created(URI.create("/api/apartamentos/" + creado.codigo()))
                .body(ApartamentoResponse.de(creado));
    }

    @GetMapping("/{codigo}")
    public ApartamentoResponse consultar(@PathVariable String codigo) {
        return ApartamentoResponse.de(consultarApartamento.ejecutar(codigo));
    }
}
