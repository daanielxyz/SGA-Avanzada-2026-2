package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CargarServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CerrarFolio;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.CerrarFolioCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.ConsultarFolio;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarDevolucion;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarPago;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RevertirCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RevertirPago;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RevertirPagoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
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

import java.util.Optional;

/**
 * Endpoints del folio de una reserva: se llega a él por el código de la reserva, no por su id (FOL-02 · DEC-61).
 * Cada {@code POST} agrega un movimiento; ninguno se edita ni se borra (RN-16). Solo traduce HTTP ⇄ caso de uso
 * (DEC-33).
 */
@RestController
@RequestMapping("/api/reservas/{codigo}/folio")
public class FolioController {

    private final UsuarioActual usuario;
    private final ConsultarReserva consultarReserva;
    private final ConsultarFolio consultarFolio;
    private final RegistrarPago registrarPago;
    private final RegistrarDevolucion registrarDevolucion;
    private final RevertirPago revertirPago;
    private final RevertirCargo revertirCargo;
    private final CargarServicioAdicional cargarServicio;
    private final CerrarFolio cerrarFolio;

    public FolioController(UsuarioActual usuario, ConsultarReserva consultarReserva, ConsultarFolio consultarFolio,
                           RegistrarPago registrarPago,
                           RegistrarDevolucion registrarDevolucion, RevertirPago revertirPago,
                           RevertirCargo revertirCargo, CargarServicioAdicional cargarServicio,
                           CerrarFolio cerrarFolio) {
        this.usuario = usuario;
        this.consultarReserva = consultarReserva;
        this.consultarFolio = consultarFolio;
        this.registrarPago = registrarPago;
        this.registrarDevolucion = registrarDevolucion;
        this.revertirPago = revertirPago;
        this.revertirCargo = revertirCargo;
        this.cargarServicio = cargarServicio;
        this.cerrarFolio = cerrarFolio;
    }

    /** El huésped solo ve el folio de sus reservas (USU-04). */
    @GetMapping
    public FolioResponse consultar(@PathVariable String codigo) {
        usuario.exigirAccesoA(consultarReserva.ejecutar(codigo).titularId());
        return FolioResponse.de(consultarFolio.ejecutar(codigo));
    }

    @PostMapping("/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public FolioResponse registrarPago(@PathVariable String codigo,
                                       @Valid @RequestBody RegistrarPagoRequest solicitud) {
        return FolioResponse.de(registrarPago.ejecutar(solicitud.aCommand(codigo)));
    }

    @PostMapping("/devoluciones")
    @ResponseStatus(HttpStatus.CREATED)
    public FolioResponse registrarDevolucion(@PathVariable String codigo,
                                             @Valid @RequestBody RegistrarPagoRequest solicitud) {
        return FolioResponse.de(registrarDevolucion.ejecutar(solicitud.aCommand(codigo)));
    }

    @PostMapping("/pagos/{id}/revertir")
    @ResponseStatus(HttpStatus.CREATED)
    public FolioResponse revertirPago(@PathVariable String codigo, @PathVariable String id) {
        return FolioResponse.de(revertirPago.ejecutar(new RevertirPagoCommand(codigo, id)));
    }

    @PostMapping("/cargos/{id}/revertir")
    @ResponseStatus(HttpStatus.CREATED)
    public FolioResponse revertirCargo(@PathVariable String codigo, @PathVariable String id,
                                       @Valid @RequestBody RevertirCargoRequest solicitud) {
        return FolioResponse.de(revertirCargo.ejecutar(solicitud.aCommand(codigo, id)));
    }

    @PostMapping("/servicios")
    @ResponseStatus(HttpStatus.CREATED)
    public FolioResponse cargarServicio(@PathVariable String codigo,
                                        @Valid @RequestBody CargarServicioRequest solicitud) {
        return FolioResponse.de(cargarServicio.ejecutar(solicitud.aCommand(codigo)));
    }

    /**
     * Sin cuerpo cierra con saldo en cero; con motivo de autorización, con saldo pendiente o a favor (RN-17), y eso
     * solo lo puede el administrador (AUTC-01).
     */
    @PutMapping("/cerrar")
    public FolioResponse cerrar(@PathVariable String codigo,
                                @Valid @RequestBody(required = false) CerrarFolioRequest solicitud) {
        String motivo = Optional.ofNullable(solicitud).map(CerrarFolioRequest::motivoAutorizacion).orElse(null);
        if (motivo != null) {
            usuario.exigirAdministrador("autorizar el cierre de un folio con saldo");
        }
        return FolioResponse.de(cerrarFolio.ejecutar(new CerrarFolioCommand(codigo, usuario.id(), motivo)));
    }
}
