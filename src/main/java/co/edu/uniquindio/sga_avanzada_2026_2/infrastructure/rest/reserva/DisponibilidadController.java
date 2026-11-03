package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.FiltroBusqueda;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarCalendarioDisponibilidadUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarApartamentosUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarApartamentosCommand;
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
    private final BuscarApartamentosUseCase buscarApartamentos;
    private final ConsultarCalendarioDisponibilidadUseCase consultarCalendario;
    private final CotizarEstanciaUseCase cotizarEstancia;

    public DisponibilidadController(AlojamientoActual alojamiento, BuscarApartamentosUseCase buscarApartamentos,
                                    ConsultarCalendarioDisponibilidadUseCase consultarCalendario,
                                    CotizarEstanciaUseCase cotizarEstancia) {
        this.alojamiento = alojamiento;
        this.buscarApartamentos = buscarApartamentos;
        this.consultarCalendario = consultarCalendario;
        this.cotizarEstancia = cotizarEstancia;
    }

    /**
     * Tarjetas de los apartamentos libres, de 10 en 10 (CU-07 · sección 7.5). Todos los filtros son opcionales.
     *
     * @param salida           exclusiva: esa noche no se ocupa (EST-01)
     * @param ocupantes        tamaño del grupo; todos cuentan para la capacidad (RN-02)
     * @param fechasNacimiento una por ocupante; con ellas el valor estimado cuenta solo a los facturables (RN-06)
     * @param capacidadMinima  el apartamento admite al menos estas personas
     * @param precioMinimo     valor estimado de la estancia, en COP
     * @param precioMaximo     valor estimado de la estancia, en COP
     * @param caracteristicas  debe tenerlas todas
     * @param servicios        el alojamiento debe ofrecerlos todos
     */
    @GetMapping("/api/apartamentos/disponibles")
    public PaginaResponse<ApartamentoEncontradoResponse> buscar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam @Min(value = 1, message = "debe ser al menos 1") int ocupantes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) List<LocalDate> fechasNacimiento,
            @RequestParam(required = false) @Min(value = 1, message = "debe ser al menos 1") Integer capacidadMinima,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "no puede ser negativo") BigDecimal precioMinimo,
            @RequestParam(required = false) @DecimalMin(value = "0", message = "no puede ser negativo") BigDecimal precioMaximo,
            @RequestParam(required = false) List<String> caracteristicas,
            @RequestParam(required = false) List<String> servicios,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(buscarApartamentos.ejecutar(new BuscarApartamentosCommand(alojamiento.id(), entrada,
                salida, ocupantes, Optional.ofNullable(fechasNacimiento).orElse(List.of()),
                new FiltroBusqueda(capacidadMinima, precioMinimo, precioMaximo, caracteristicas, servicios), pagina)),
                ApartamentoEncontradoResponse::de);
    }

    /**
     * Qué noches se pueden vender, para el calendario del detalle (CU-08). Es público y no dice quién reservó.
     *
     * @param hasta exclusiva (EST-01)
     */
    @GetMapping("/api/apartamentos/{codigo}/calendario")
    public List<NocheCalendarioResponse> calendario(
            @PathVariable String codigo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return consultarCalendario.ejecutar(codigo, desde, hasta).stream().map(NocheCalendarioResponse::de).toList();
    }

    /** Es un cálculo, no crea ningún recurso: responde 200. */
    @PostMapping("/api/cotizaciones")
    public CotizacionResponse cotizar(@Valid @RequestBody CotizarEstanciaRequest solicitud) {
        return CotizacionResponse.de(cotizarEstancia.ejecutar(solicitud.aCommand()));
    }
}
