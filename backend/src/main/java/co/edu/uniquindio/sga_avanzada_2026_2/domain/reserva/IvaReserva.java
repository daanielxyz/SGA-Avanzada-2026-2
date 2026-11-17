package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * VO: el IVA congelado en una reserva desde que se crea (DEC-134 · DEC-141). El valor de las noches sale de las
 * tarifas; el IVA se le suma o ya va dentro, y no se cobra si la reserva está exenta porque decide un pasaporte.
 *
 * @param porcentaje el que cobraba el alojamiento al crearla; 0 si no lo cobraba (y entonces nunca lleva IVA)
 * @param exento     si quien decide trae pasaporte (residente en otro país); se revisa en la llegada
 * @param incluido   si las tarifas ya traían el IVA al crearla (DEC-141): el exento paga entonces sin él
 */
public record IvaReserva(Porcentaje porcentaje, boolean exento, boolean incluido) {

    /** Sin IVA: las reservas de antes de DEC-134, las de un alojamiento que no lo cobra y las de canales externos. */
    public static final IvaReserva NINGUNO = new IvaReserva(new Porcentaje(0), false);

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    // DEC-134
    public IvaReserva {
        if (porcentaje == null) {
            throw new ReglaDominioException("El IVA de la reserva requiere su porcentaje");
        }
    }

    /** El IVA que se suma a las noches, como antes de DEC-141. */
    public IvaReserva(Porcentaje porcentaje, boolean exento) {
        this(porcentaje, exento, false);
    }

    /**
     * El IVA de una reserva nueva o modificada con la configuración del alojamiento de ese momento.
     *
     * @param documento el que decide la exención: el del huésped principal o, si aún no lo tiene, el del titular
     */
    // DEC-134 · DEC-141
    public static IvaReserva para(ImpuestoAlojamiento impuesto, Documento documento) {
        if (!impuesto.cobraIva()) {
            return NINGUNO;
        }
        return new IvaReserva(impuesto.porcentaje(), esPasaporte(documento), impuesto.incluidoEnTarifa());
    }

    /** Lo que muestra una cotización, antes de saber quién llega: con IVA si el alojamiento lo cobra. */
    public static IvaReserva paraCotizar(ImpuestoAlojamiento impuesto) {
        return impuesto.cobraIva()
                ? new IvaReserva(impuesto.porcentaje(), false, impuesto.incluidoEnTarifa())
                : NINGUNO;
    }

    /**
     * La misma tasa congelada, con la exención según el documento de quien llega (DEC-134). Una reserva creada sin
     * IVA sigue sin IVA, aunque el alojamiento lo cobre ahora.
     */
    public IvaReserva revisadoCon(Documento documento) {
        return cobra() ? new IvaReserva(porcentaje, esPasaporte(documento), incluido) : this;
    }

    /**
     * El IVA que se cobra sobre el valor de las noches, redondeado al peso; cero si no se cobra o está exenta. Si ya
     * venía en las tarifas, es el que traen dentro (DEC-141).
     */
    public Dinero sobre(Dinero valorNoches) {
        if (!cobra() || exento) {
            return Dinero.CERO;
        }
        return incluido ? incluidoEn(valorNoches) : valorNoches.porcentaje(porcentaje);
    }

    /**
     * El IVA que traen dentro las tarifas (DEC-141): $ 119.000 al 19 % traen $ 19.000. Se calcula como la diferencia
     * con la base redondeada, para que base más IVA den exacto el precio que escribió el hotel. Cero si las tarifas
     * no lo incluyen.
     */
    public Dinero incluidoEn(Dinero valorNoches) {
        if (!cobra() || !incluido) {
            return Dinero.CERO;
        }
        BigDecimal base = valorNoches.monto().multiply(CIEN)
                .divide(CIEN.add(BigDecimal.valueOf(porcentaje.valor())), 0, RoundingMode.HALF_UP);
        return valorNoches.restar(new Dinero(base));
    }

    /** Si el alojamiento lo cobraba al crear la reserva. */
    public boolean cobra() {
        return porcentaje.valor() > 0;
    }

    private static boolean esPasaporte(Documento documento) {
        return documento != null && documento.tipo() == TipoDocumento.PASAPORTE;
    }
}
