package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * VO: el IVA de la estancia que cobra el alojamiento (DEC-133 · DEC-141). Las tarifas son el precio que escribe el
 * hotel: se le suma el IVA o ya lo incluye, según {@code incluidoEnTarifa}.
 *
 * @param cobraIva         si el alojamiento lo cobra; viene apagado
 * @param porcentaje       el del IVA, 19 en Colombia
 * @param incluidoEnTarifa si la tarifa ya trae el IVA ($ 119.000 = $ 100.000 + $ 19.000); si no, se le suma
 */
public record ImpuestoAlojamiento(boolean cobraIva, Porcentaje porcentaje, boolean incluidoEnTarifa) {

    /** El de un alojamiento nuevo: apagado, con el 19 % listo para encenderlo y sumado a la tarifa (DEC-133). */
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

    /** El IVA que se suma a la tarifa, como antes de DEC-141. */
    public ImpuestoAlojamiento(boolean cobraIva, Porcentaje porcentaje) {
        this(cobraIva, porcentaje, false);
    }
}
