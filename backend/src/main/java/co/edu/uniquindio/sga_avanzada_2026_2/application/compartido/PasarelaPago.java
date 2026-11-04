package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;

/**
 * Puerto de salida hacia la pasarela de pagos (DEC-69). El sistema nunca recibe datos de la tarjeta: el cliente los
 * da en la pasarela, que le entrega un token, y con ese token se cobra.
 */
public interface PasarelaPago {

    /**
     * Cobra el monto. Un rechazo no es un error: vuelve como un cobro no aprobado, con su motivo.
     *
     * @param tokenPago lo que entregó la pasarela al cliente
     */
    CobroEnLinea cobrar(String codigoReserva, Dinero monto, String tokenPago);

    /**
     * @param referencia identificador del cobro en la pasarela; nulo si se rechazó
     * @param motivo     por qué se rechazó; nulo si se aprobó
     */
    record CobroEnLinea(boolean aprobado, String referencia, String motivo) {
    }
}
