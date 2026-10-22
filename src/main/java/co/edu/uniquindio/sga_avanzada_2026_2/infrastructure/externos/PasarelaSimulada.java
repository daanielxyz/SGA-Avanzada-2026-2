package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.PasarelaPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación local de la {@link PasarelaPago} (DEC-69): no cobra nada. Aprueba todo token salvo
 * {@value #TOKEN_RECHAZADO}, que sirve para probar un rechazo.
 */
// TODO(MVP2): adaptador de la pasarela real (Wompi, DEC-74) tras este mismo puerto; este queda como alternativa local.
@Component
public class PasarelaSimulada implements PasarelaPago {

    public static final String TOKEN_RECHAZADO = "tok_rechazado";

    private static final Logger LOG = LoggerFactory.getLogger(PasarelaSimulada.class);

    @Override
    public CobroEnLinea cobrar(String codigoReserva, Dinero monto, String tokenPago) {
        if (TOKEN_RECHAZADO.equals(tokenPago)) {
            LOG.info("Pasarela simulada: rechaza el cobro de {} para {}", monto.monto(), codigoReserva);
            return new CobroEnLinea(false, null, "fondos insuficientes (simulado)");
        }
        String referencia = "SIM-" + UUID.randomUUID();
        LOG.info("Pasarela simulada: aprueba el cobro de {} para {} con la referencia {}", monto.monto(),
                codigoReserva, referencia);
        return new CobroEnLinea(true, referencia, null);
    }
}
