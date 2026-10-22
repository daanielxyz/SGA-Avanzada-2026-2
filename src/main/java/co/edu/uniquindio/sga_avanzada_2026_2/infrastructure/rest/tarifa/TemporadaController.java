package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.tarifa;

import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CambiarFechasTemporadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarCalendarioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DesactivarTemporadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DesactivarTemporadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints del calendario de temporadas del alojamiento (CU-33). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/temporadas")
public class TemporadaController {

    private final AlojamientoActual alojamiento;
    private final ConsultarCalendarioUseCase consultarCalendario;
    private final AgregarTemporadaUseCase agregarTemporada;
    private final CambiarFechasTemporadaUseCase cambiarFechas;
    private final DesactivarTemporadaUseCase desactivarTemporada;

    public TemporadaController(AlojamientoActual alojamiento, ConsultarCalendarioUseCase consultarCalendario,
                               AgregarTemporadaUseCase agregarTemporada, CambiarFechasTemporadaUseCase cambiarFechas,
                               DesactivarTemporadaUseCase desactivarTemporada) {
        this.alojamiento = alojamiento;
        this.consultarCalendario = consultarCalendario;
        this.agregarTemporada = agregarTemporada;
        this.cambiarFechas = cambiarFechas;
        this.desactivarTemporada = desactivarTemporada;
    }

    @GetMapping
    public CalendarioResponse consultar() {
        return CalendarioResponse.de(consultarCalendario.ejecutar(alojamiento.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaCreadaResponse agregar(@Valid @RequestBody AgregarTemporadaRequest solicitud) {
        return TemporadaCreadaResponse.de(agregarTemporada.ejecutar(solicitud.aCommand(alojamiento.id())));
    }

    @PutMapping("/{id}/fechas")
    public CalendarioResponse cambiarFechas(@PathVariable String id,
                                            @Valid @RequestBody CambiarFechasTemporadaRequest solicitud) {
        return CalendarioResponse.de(cambiarFechas.ejecutar(solicitud.aCommand(alojamiento.id(), id)));
    }

    @PutMapping("/{id}/desactivar")
    public CalendarioResponse desactivar(@PathVariable String id) {
        return CalendarioResponse.de(
                desactivarTemporada.ejecutar(new DesactivarTemporadaCommand(alojamiento.id(), id)));
    }
}
