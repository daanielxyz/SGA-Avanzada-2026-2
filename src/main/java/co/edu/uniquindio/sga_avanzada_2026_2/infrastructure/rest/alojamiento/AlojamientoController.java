package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.AgregarServicioAdicionalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarParametrosAlojamientoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarUbicacionAlojamientoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarValorServicioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.ConsultarAlojamientoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.DesactivarServicioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.DesactivarServicioCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.DeshabilitarMedioPagoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.HabilitarMedioPagoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.MedioPagoCommand;
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
 * Endpoints del alojamiento de este despliegue (ALO-01): no lleva id en la URL (DEC-64). Solo traduce HTTP ⇄ caso de
 * uso; los errores los responde el manejador global (DEC-33).
 */
@RestController
@RequestMapping("/api/alojamiento")
public class AlojamientoController {

    private final AlojamientoActual alojamiento;
    private final ConsultarAlojamientoUseCase consultarAlojamiento;
    private final CambiarParametrosAlojamientoUseCase cambiarParametros;
    private final CambiarUbicacionAlojamientoUseCase cambiarUbicacion;
    private final HabilitarMedioPagoUseCase habilitarMedioPago;
    private final DeshabilitarMedioPagoUseCase deshabilitarMedioPago;
    private final AgregarServicioAdicionalUseCase agregarServicio;
    private final CambiarValorServicioUseCase cambiarValorServicio;
    private final DesactivarServicioUseCase desactivarServicio;

    public AlojamientoController(AlojamientoActual alojamiento, ConsultarAlojamientoUseCase consultarAlojamiento,
                                 CambiarParametrosAlojamientoUseCase cambiarParametros,
                                 CambiarUbicacionAlojamientoUseCase cambiarUbicacion,
                                 HabilitarMedioPagoUseCase habilitarMedioPago,
                                 DeshabilitarMedioPagoUseCase deshabilitarMedioPago,
                                 AgregarServicioAdicionalUseCase agregarServicio,
                                 CambiarValorServicioUseCase cambiarValorServicio,
                                 DesactivarServicioUseCase desactivarServicio) {
        this.alojamiento = alojamiento;
        this.consultarAlojamiento = consultarAlojamiento;
        this.cambiarParametros = cambiarParametros;
        this.cambiarUbicacion = cambiarUbicacion;
        this.habilitarMedioPago = habilitarMedioPago;
        this.deshabilitarMedioPago = deshabilitarMedioPago;
        this.agregarServicio = agregarServicio;
        this.cambiarValorServicio = cambiarValorServicio;
        this.desactivarServicio = desactivarServicio;
    }

    @GetMapping
    public AlojamientoResponse consultar() {
        return AlojamientoResponse.de(consultarAlojamiento.ejecutar(alojamiento.id()));
    }

    @PutMapping("/parametros")
    public AlojamientoResponse cambiarParametros(@Valid @RequestBody ParametrosRequest solicitud) {
        return AlojamientoResponse.de(cambiarParametros.ejecutar(solicitud.aCommand(alojamiento.id())));
    }

    @PutMapping("/ubicacion")
    public AlojamientoResponse cambiarUbicacion(@Valid @RequestBody UbicacionRequest solicitud) {
        return AlojamientoResponse.de(cambiarUbicacion.ejecutar(solicitud.aCommand(alojamiento.id())));
    }

    @PutMapping("/medios-pago/{medio}/habilitar")
    public AlojamientoResponse habilitarMedioPago(@PathVariable String medio) {
        return AlojamientoResponse.de(habilitarMedioPago.ejecutar(new MedioPagoCommand(alojamiento.id(), medio)));
    }

    @PutMapping("/medios-pago/{medio}/deshabilitar")
    public AlojamientoResponse deshabilitarMedioPago(@PathVariable String medio) {
        return AlojamientoResponse.de(deshabilitarMedioPago.ejecutar(new MedioPagoCommand(alojamiento.id(), medio)));
    }

    @PostMapping("/servicios")
    @ResponseStatus(HttpStatus.CREATED)
    public AlojamientoResponse agregarServicio(@Valid @RequestBody ServicioAdicionalRequest solicitud) {
        return AlojamientoResponse.de(agregarServicio.ejecutar(solicitud.aCommand(alojamiento.id())));
    }

    @PutMapping("/servicios/{id}/valor")
    public AlojamientoResponse cambiarValorServicio(@PathVariable String id,
                                                    @Valid @RequestBody ValorServicioRequest solicitud) {
        return AlojamientoResponse.de(cambiarValorServicio.ejecutar(solicitud.aCommand(alojamiento.id(), id)));
    }

    @PutMapping("/servicios/{id}/desactivar")
    public AlojamientoResponse desactivarServicio(@PathVariable String id) {
        return AlojamientoResponse.de(
                desactivarServicio.ejecutar(new DesactivarServicioCommand(alojamiento.id(), id)));
    }
}
