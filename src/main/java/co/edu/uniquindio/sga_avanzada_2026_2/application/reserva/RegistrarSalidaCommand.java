package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Entrada de {@link RegistrarSalida}.
 *
 * @param autor              usuario que registra la salida (SAL-05) y, si hay motivo, quien autoriza; en B3 sale del
 *                           usuario autenticado (DEC-61)
 * @param motivoAutorizacion solo para salir con saldo distinto de cero (CU-38 · RN-17); vacío en la salida normal
 */
public record RegistrarSalidaCommand(String codigo, String autor, String motivoAutorizacion) {

    /**
     * La autorización de cierre, solo si llegó un motivo (RN-17). Que el autor sea administrador lo controla la
     * seguridad (DEC-22 · DEC-61).
     */
    public Optional<AutorizacionCierre> autorizacion(LocalDateTime ahora) {
        return Optional.ofNullable(motivoAutorizacion)
                .filter(motivo -> !motivo.isBlank())
                .map(motivo -> new AutorizacionCierre(new UsuarioId(autor), motivo, ahora));
    }
}
