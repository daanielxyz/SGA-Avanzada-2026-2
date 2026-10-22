package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CorregirRegistroUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarSalidaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarSalidaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import jakarta.validation.Valid;
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

    public EstanciaController(UsuarioActual usuario, RegistrarLlegadaUseCase registrarLlegada,
                              CorregirRegistroUseCase corregirRegistro, RegistrarSalidaUseCase registrarSalida) {
        this.usuario = usuario;
        this.registrarLlegada = registrarLlegada;
        this.corregirRegistro = corregirRegistro;
        this.registrarSalida = registrarSalida;
    }

    @PutMapping("/registrar-llegada")
    public EstanciaResponse registrarLlegada(@PathVariable String codigo) {
        return EstanciaResponse.de(registrarLlegada.ejecutar(new RegistrarLlegadaCommand(codigo, usuario.id())));
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
