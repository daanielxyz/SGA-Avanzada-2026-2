package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarTarifas;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifa;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de las tarifas de un apartamento, una por temporada (CU-34). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/apartamentos/{codigo}/tarifas")
public class TarifaController {

    private final ConsultarTarifas consultarTarifas;
    private final DefinirTarifa definirTarifa;

    public TarifaController(ConsultarTarifas consultarTarifas, DefinirTarifa definirTarifa) {
        this.consultarTarifas = consultarTarifas;
        this.definirTarifa = definirTarifa;
    }

    /** Las tarifas vigentes hoy. */
    @GetMapping
    public List<TarifaResponse> consultar(@PathVariable String codigo) {
        return consultarTarifas.ejecutar(codigo).stream().map(TarifaResponse::de).toList();
    }

    /** Crea la tarifa de esa temporada o publica su versión siguiente (TAR-05). */
    @PutMapping("/{temporada}")
    public TarifaResponse definir(@PathVariable String codigo, @PathVariable String temporada,
                                  @Valid @RequestBody DefinirTarifaRequest solicitud) {
        return TarifaResponse.de(definirTarifa.ejecutar(solicitud.aCommand(codigo, temporada)));
    }
}
