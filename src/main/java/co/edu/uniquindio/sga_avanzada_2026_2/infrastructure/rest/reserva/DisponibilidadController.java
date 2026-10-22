package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints previos a reservar: qué apartamentos hay libres y cuánto cuesta la estancia (CU-07 · CU-09). No guardan
 * nada. Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
public class DisponibilidadController {

    private final AlojamientoActual alojamiento;
    private final BuscarDisponiblesUseCase buscarDisponibles;
    private final CotizarEstanciaUseCase cotizarEstancia;

    public DisponibilidadController(AlojamientoActual alojamiento, BuscarDisponiblesUseCase buscarDisponibles,
                                    CotizarEstanciaUseCase cotizarEstancia) {
        this.alojamiento = alojamiento;
        this.buscarDisponibles = buscarDisponibles;
        this.cotizarEstancia = cotizarEstancia;
    }

    /**
     * @param salida    exclusiva: esa noche no se ocupa (EST-01)
     * @param ocupantes tamaño del grupo; todos cuentan para la capacidad (RN-02)
     */
    @GetMapping("/api/apartamentos/disponibles")
    public List<ApartamentoDisponibleResponse> buscar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam @Min(value = 1, message = "debe ser al menos 1") int ocupantes) {
        return buscarDisponibles.ejecutar(new BuscarDisponiblesCommand(alojamiento.id(), entrada, salida, ocupantes))
                .stream()
                .map(ApartamentoDisponibleResponse::de)
                .toList();
    }

    /** Es un cálculo, no crea ningún recurso: responde 200. */
    @PostMapping("/api/cotizaciones")
    public CotizacionResponse cotizar(@Valid @RequestBody CotizarEstanciaRequest solicitud) {
        return CotizacionResponse.de(cotizarEstancia.ejecutar(solicitud.aCommand()));
    }
}
