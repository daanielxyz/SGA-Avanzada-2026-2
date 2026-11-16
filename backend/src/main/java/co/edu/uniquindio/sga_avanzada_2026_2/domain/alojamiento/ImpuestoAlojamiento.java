package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: el IVA que el alojamiento le suma a la estancia (DEC-133). Las tarifas son la base, sin IVA; si se cobra, al
 * registrar la llegada se carga el porcentaje sobre el valor de las noches.
 *
 * @param cobraIva   si el alojamiento lo cobra; viene apagado
 * @param porcentaje el del IVA, 19 en Colombia
 */
public record ImpuestoAlojamiento(boolean cobraIva, Porcentaje porcentaje) {

    /** El de un alojamiento nuevo: apagado, con el 19 % listo para encenderlo (DEC-133). */
    public static final ImpuestoAlojamiento INICIAL = new ImpuestoAlojamiento(false, new Porcentaje(19));

    // DEC-133
    public ImpuestoAlojamiento {
        if (porcentaje == null) {
            throw new ReglaDominioException("El IVA del alojamiento requiere su porcentaje");
        }
        if (cobraIva && porcentaje.valor() == 0) {
            throw new ReglaDominioException("Si se cobra el IVA, su porcentaje debe ser mayor que 0");
        }
    }
}
