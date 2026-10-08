package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.compartido;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link GeneradorCodigos} con una secuencia de la BD por serie ({@code seq_<serie>}, V8):
 * la BD garantiza que no se repita entre transacciones concurrentes (DEC-52).
 */
@Component
public class GeneradorCodigosSecuencias implements GeneradorCodigos {

    private final EntityManager em;

    public GeneradorCodigosSecuencias(EntityManager em) {
        this.em = em;
    }

    @Override
    public String siguiente(SerieCodigo serie) {
        String secuencia = "seq_" + serie.name().toLowerCase();
        Number numero = (Number) em.createNativeQuery("select nextval('" + secuencia + "')").getSingleResult();
        return serie.prefijo() + "-" + numero.longValue();
    }
}
