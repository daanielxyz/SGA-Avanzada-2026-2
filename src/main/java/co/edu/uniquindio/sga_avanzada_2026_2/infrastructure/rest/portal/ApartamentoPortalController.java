package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ConsultarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el visitante del portal ve de los apartamentos, sin sesión (CU-07 · CU-08): los libres para sus fechas y la
 * ficha de cada uno, solo con los datos públicos ({@link ApartamentoPublicoResponse}). Reusa los casos de uso del
 * personal sin cambiarlos. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/portal/apartamentos")
public class ApartamentoPortalController {

    private final AlojamientoActual alojamiento;
    private final BuscarDisponiblesUseCase buscarDisponibles;
    private final ConsultarApartamentoUseCase consultarApartamento;

    public ApartamentoPortalController(AlojamientoActual alojamiento, BuscarDisponiblesUseCase buscarDisponibles,
                                       ConsultarApartamentoUseCase consultarApartamento) {
        this.alojamiento = alojamiento;
        this.buscarDisponibles = buscarDisponibles;
        this.consultarApartamento = consultarApartamento;
    }

    /**
     * El precio de cada uno se pide aparte, con {@code POST /api/cotizaciones}.
     *
     * @param salida    exclusiva: esa noche no se ocupa (EST-01)
     * @param ocupantes tamaño del grupo; todos cuentan para la capacidad (RN-02)
     */
    @GetMapping("/disponibles")
    public List<ApartamentoPublicoResponse> buscar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam @Min(value = 1, message = "debe ser al menos 1") int ocupantes) {
        return buscarDisponibles.ejecutar(new BuscarDisponiblesCommand(alojamiento.id(), entrada, salida, ocupantes))
                .stream()
                .map(ApartamentoDisponibleResult::codigo)
                .map(consultarApartamento::ejecutar)
                .map(ApartamentoPublicoResponse::de)
                .toList();
    }

    /** APA-16: un apartamento retirado no existe para el público, igual que en la búsqueda: 404. */
    @GetMapping("/{codigo}")
    public ApartamentoPublicoResponse consultar(@PathVariable String codigo) {
        ApartamentoResult apartamento = consultarApartamento.ejecutar(codigo);
        if (!apartamento.activo()) {
            throw new RecursoNoEncontradoException("el apartamento", codigo);
        }
        return ApartamentoPublicoResponse.de(apartamento);
    }
}
