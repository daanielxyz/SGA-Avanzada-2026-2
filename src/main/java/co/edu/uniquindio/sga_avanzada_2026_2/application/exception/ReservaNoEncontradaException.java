package co.edu.uniquindio.sga_avanzada_2026_2.application.exception;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CodigoReserva;

public class ReservaNoEncontradaException extends RuntimeException {
    public ReservaNoEncontradaException(CodigoReserva codigo) {
        super("La resrrva con el "+codigo+" no se fue encontrada");
    }


}
