package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.constraints.Min;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarHistorialTarifasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarTarifasUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaUseCase;
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

    private final ConsultarTarifasUseCase consultarTarifas;
    private final ConsultarHistorialTarifasUseCase consultarHistorial;
    private final DefinirTarifaUseCase definirTarifa;

    public TarifaController(ConsultarHistorialTarifasUseCase consultarHistorial,
                            ConsultarTarifasUseCase consultarTarifas, DefinirTarifaUseCase definirTarifa) {
        this.consultarTarifas = consultarTarifas;
        this.consultarHistorial = consultarHistorial;
        this.definirTarifa = definirTarifa;
    }

    /** Las tarifas vigentes hoy, de 10 en 10 (DEC-18). */
    @GetMapping
    public PaginaResponse<TarifaResponse> consultar(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(Pagina.de(consultarTarifas.ejecutar(codigo), pagina), TarifaResponse::de);
    }

    /** Todas las versiones, de la más reciente a la más antigua (TAR-05). */
    @GetMapping("/historial")
    public PaginaResponse<TarifaResponse> consultarHistorial(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(consultarHistorial.ejecutar(codigo, pagina), TarifaResponse::de);
    }

    /** Crea la tarifa de esa temporada o publica su versión siguiente (TAR-05). */
    @PutMapping("/{temporada}")
    public TarifaResponse definir(@PathVariable String codigo, @PathVariable String temporada,
                                  @Valid @RequestBody DefinirTarifaRequest solicitud) {
        return TarifaResponse.de(definirTarifa.ejecutar(solicitud.aCommand(codigo, temporada)));
    }
}
