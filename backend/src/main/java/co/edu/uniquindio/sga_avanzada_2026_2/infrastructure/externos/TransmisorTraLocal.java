package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.HuespedTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TransmisorTra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link TransmisorTra} sin salir a internet (DEC-130): es el de por defecto y el de la demo publicada, para no mandar
 * huéspedes inventados a un sistema del Gobierno. Deja en el log solo la reserva y cuántos son, nunca sus datos.
 */
@Component
public class TransmisorTraLocal implements TransmisorTra {

    private static final Logger LOG = LoggerFactory.getLogger(TransmisorTraLocal.class);

    private final AtomicLong codigos = new AtomicLong();

    // DEC-130
    @Override
    public String enviarPrincipal(ReporteTra reporte) {
        String codigo = "LOCAL-" + codigos.incrementAndGet();
        LOG.info("TRA (local, no sale a internet): reserva {}, huésped principal y {} acompañantes → {}",
                reporte.reserva().valor(), reporte.numeroAcompanantes(), codigo);
        return codigo;
    }

    @Override
    public void enviarAcompanante(ReporteTra reporte, HuespedTra acompanante, String codigoPrincipal) {
        LOG.info("TRA (local, no sale a internet): reserva {}, acompañante de {}", reporte.reserva().valor(),
                codigoPrincipal);
    }
}
