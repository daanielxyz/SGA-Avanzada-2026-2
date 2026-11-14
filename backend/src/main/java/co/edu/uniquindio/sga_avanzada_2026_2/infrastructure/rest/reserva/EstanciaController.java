package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CambiarTitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CambiarTitularUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CompletarDatosOcupanteUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarEstanciaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConsultarReservaConTitularUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirDocumentoTitularUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirDocumentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.DesignarHuespedPrincipalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirRegistroUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarSalidaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Endpoints de la llegada, su corrección y la salida (CU-21 · CU-22 · CU-38). El autor es el usuario que hace la
 * petición, nunca un dato del cuerpo (DEC-61). Solo traduce HTTP ⇄ caso de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/reservas/{codigo}")
public class EstanciaController {

    private final UsuarioActual usuario;
    private final RegistrarLlegadaUseCase registrarLlegada;
    private final CorregirRegistroUseCase corregirRegistro;
    private final RegistrarSalidaUseCase registrarSalida;
    private final CompletarDatosOcupanteUseCase completarDatosOcupante;
    private final CambiarTitularUseCase cambiarTitular;
    private final DesignarHuespedPrincipalUseCase designarHuespedPrincipal;
    private final ConsultarEstanciaUseCase consultarEstancia;
    private final CorregirDocumentoUseCase corregirDocumento;
    private final CorregirDocumentoTitularUseCase corregirDocumentoTitular;
    private final ConsultarReservaConTitularUseCase consultarDetalle;

    public EstanciaController(UsuarioActual usuario, RegistrarLlegadaUseCase registrarLlegada,
                              CorregirRegistroUseCase corregirRegistro, RegistrarSalidaUseCase registrarSalida,
                              CompletarDatosOcupanteUseCase completarDatosOcupante,
                              CambiarTitularUseCase cambiarTitular,
                              DesignarHuespedPrincipalUseCase designarHuespedPrincipal,
                              ConsultarEstanciaUseCase consultarEstancia,
                              CorregirDocumentoUseCase corregirDocumento,
                              CorregirDocumentoTitularUseCase corregirDocumentoTitular,
                              ConsultarReservaConTitularUseCase consultarDetalle) {
        this.usuario = usuario;
        this.registrarLlegada = registrarLlegada;
        this.corregirRegistro = corregirRegistro;
        this.registrarSalida = registrarSalida;
        this.completarDatosOcupante = completarDatosOcupante;
        this.cambiarTitular = cambiarTitular;
        this.designarHuespedPrincipal = designarHuespedPrincipal;
        this.consultarEstancia = consultarEstancia;
        this.corregirDocumento = corregirDocumento;
        this.corregirDocumentoTitular = corregirDocumentoTitular;
        this.consultarDetalle = consultarDetalle;
    }

    /**
     * Corrige el documento mal escrito de un ocupante sin perder sus datos del TRA (#14 · DEC-120); si es el titular
     * que se aloja, se corrige también el titular. Responde el detalle de la reserva, con el historial de
     * correcciones; recepción y administración, por la regla de {@code /api/reservas/**}.
     */
    @PutMapping("/ocupantes/{ocupanteId}/documento")
    public ReservaResponse corregirDocumento(@PathVariable String codigo, @PathVariable String ocupanteId,
                                             @Valid @RequestBody CorregirDocumentoRequest solicitud) {
        corregirDocumento.ejecutar(solicitud.aCommand(codigo, ocupanteId, usuario.id()));
        return ReservaResponse.de(consultarDetalle.ejecutar(codigo));
    }

    /**
     * Corrige el documento mal escrito del titular de la reserva, también si no se aloja (#14 · DEC-120). Sigue
     * siendo el mismo titular, con sus reservas y su cuenta; si se aloja, su ocupante se corrige con él. Responde el
     * detalle de la reserva, como {@link #corregirDocumento}.
     */
    @PutMapping("/titular/documento")
    public ReservaResponse corregirDocumentoTitular(@PathVariable String codigo,
                                                    @Valid @RequestBody CorregirDocumentoRequest solicitud) {
        corregirDocumentoTitular.ejecutar(solicitud.aCommandDelTitular(codigo, usuario.id()));
        return ReservaResponse.de(consultarDetalle.ejecutar(codigo));
    }

    /**
     * La llegada registrada (la vigente y las anuladas por una corrección, con su autor y el motivo sin pago), la
     * salida y el saldo (#13 · DEC-114). Recepción y administración, por la regla de {@code /api/reservas/**}: el
     * huésped y el personal de servicio reciben 403 (DEC-99).
     */
    @GetMapping("/estancia")
    public EstanciaResponse consultarEstancia(@PathVariable String codigo) {
        return EstanciaResponse.de(consultarEstancia.ejecutar(codigo));
    }

    /** Otro ocupante pasa a ser el huésped principal del TRA; debe ser mayor de edad (DEC-97). */
    @PutMapping("/ocupantes/{ocupanteId}/principal")
    public ReservaResponse designarHuespedPrincipal(@PathVariable String codigo, @PathVariable String ocupanteId) {
        return ReservaResponse.de(designarHuespedPrincipal.ejecutar(codigo, ocupanteId));
    }

    /**
     * Quien reservó no debe ser el titular (TIT-01 · DEC-97): el titular nuevo responde por la reserva y su folio, y
     * no tiene que alojarse. Con un documento que ya existe se usa ese titular.
     */
    @PutMapping("/cambiar-titular")
    public ReservaResponse cambiarTitular(@PathVariable String codigo,
                                          @Valid @RequestBody TitularReservaRequest solicitud) {
        return ReservaResponse.de(cambiarTitular.ejecutar(new CambiarTitularCommand(codigo, solicitud.aCommand())));
    }

    /**
     * Los datos del TRA de un ocupante, en la llegada o después (BL-07). Lo que no llega se conserva; mientras a
     * alguien le falte algo, la salida responde 409 {@code DATOS_TRA_INCOMPLETOS}.
     */
    @PutMapping("/ocupantes/{ocupanteId}/datos-tra")
    public ReservaResponse completarDatosOcupante(@PathVariable String codigo, @PathVariable String ocupanteId,
                                                  @Valid @RequestBody DatosOcupanteRequest solicitud) {
        return ReservaResponse.de(completarDatosOcupante.ejecutar(solicitud.aCommand(codigo, ocupanteId)));
    }

    /**
     * Sin cuerpo exige la estancia pagada; con motivo, entra sin el pago completo y queda registrado (BL-22). Eso solo
     * lo puede el administrador, como la salida con saldo (AUTC-01 · DEC-112).
     */
    @PutMapping("/registrar-llegada")
    public EstanciaResponse registrarLlegada(@PathVariable String codigo,
                                             @Valid @RequestBody(required = false) RegistrarLlegadaRequest solicitud) {
        String motivo = Optional.ofNullable(solicitud).map(RegistrarLlegadaRequest::motivoSinPago).orElse(null);
        if (motivo != null) {
            usuario.exigirAdministrador("autorizar una llegada sin la estancia pagada");
        }
        return EstanciaResponse.de(
                registrarLlegada.ejecutar(new RegistrarLlegadaCommand(codigo, usuario.id(), motivo)));
    }

    @PutMapping("/corregir-registro")
    public EstanciaResponse corregirRegistro(@PathVariable String codigo,
                                             @Valid @RequestBody CorregirRegistroRequest solicitud) {
        return EstanciaResponse.de(corregirRegistro.ejecutar(solicitud.aCommand(codigo, usuario.id())));
    }

    /**
     * Sin cuerpo es la salida normal; con motivo de autorización, la salida con saldo pendiente (RN-17), y eso solo
     * lo puede el administrador (AUTC-01).
     */
    @PutMapping("/registrar-salida")
    public EstanciaResponse registrarSalida(@PathVariable String codigo,
                                            @Valid @RequestBody(required = false) RegistrarSalidaRequest solicitud) {
        String motivo = Optional.ofNullable(solicitud).map(RegistrarSalidaRequest::motivoAutorizacion).orElse(null);
        if (motivo != null) {
            usuario.exigirAdministrador("autorizar una salida con saldo pendiente");
        }
        return EstanciaResponse.de(
                registrarSalida.ejecutar(new RegistrarSalidaCommand(codigo, usuario.id(), motivo)));
    }
}
