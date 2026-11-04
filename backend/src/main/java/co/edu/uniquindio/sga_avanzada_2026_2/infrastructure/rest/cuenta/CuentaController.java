package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.cuenta;

import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ListarReservasDeTitularUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ActualizarCuentaTitularUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.titular.ConsultarTitularUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.UsuarioActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.reserva.ReservaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La cuenta del huésped y sus reservas (DEC-68 · USU-04). El titular siempre es el del token, nunca uno de la URL.
 */
@RestController
public class CuentaController {

    private final UsuarioActual usuario;
    private final ConsultarTitularUseCase consultarTitular;
    private final ActualizarCuentaTitularUseCase actualizarCuenta;
    private final ListarReservasDeTitularUseCase listarReservas;

    public CuentaController(UsuarioActual usuario, ConsultarTitularUseCase consultarTitular,
                            ActualizarCuentaTitularUseCase actualizarCuenta,
                            ListarReservasDeTitularUseCase listarReservas) {
        this.usuario = usuario;
        this.consultarTitular = consultarTitular;
        this.actualizarCuenta = actualizarCuenta;
        this.listarReservas = listarReservas;
    }

    @GetMapping("/api/mi-cuenta")
    public CuentaResponse consultar() {
        return CuentaResponse.de(consultarTitular.ejecutar(usuario.id()));
    }

    @PutMapping("/api/mi-cuenta")
    public CuentaResponse actualizar(@Valid @RequestBody ActualizarCuentaRequest solicitud) {
        return CuentaResponse.de(actualizarCuenta.ejecutar(solicitud.aCommand(usuario.id())));
    }

    @GetMapping("/api/mis-reservas")
    public PaginaResponse<ReservaResponse> misReservas(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(listarReservas.ejecutar(usuario.id(), pagina), ReservaResponse::de);
    }
}
