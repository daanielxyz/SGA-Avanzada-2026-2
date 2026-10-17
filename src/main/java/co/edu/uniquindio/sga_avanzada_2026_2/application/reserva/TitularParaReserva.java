package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;

/**
 * El titular de una reserva nueva, como lo deja {@link CargadorReserva#titularPara} (DEC-69).
 *
 * @param cuentaNueva    con esta reserva estrena cuenta: ahora tiene correo y antes no existía o no lo tenía
 * @param correoDistinto ya tenía cuenta y llegó otro correo, que no reemplaza al de su cuenta
 */
public record TitularParaReserva(Titular titular, boolean cuentaNueva, boolean correoDistinto) {
}
