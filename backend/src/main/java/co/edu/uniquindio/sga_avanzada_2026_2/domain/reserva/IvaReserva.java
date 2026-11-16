package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;

/**
 * VO: el IVA congelado en una reserva desde que se crea (DEC-134). Las tarifas son la base sin IVA; el IVA es ese
 * porcentaje sobre el valor de las noches, salvo que la reserva esté exenta porque decide un pasaporte.
 *
 * @param porcentaje el que cobraba el alojamiento al crearla; 0 si no lo cobraba (y entonces nunca lleva IVA)
 * @param exento     si quien decide trae pasaporte (residente en otro país); se revisa en la llegada
 */
public record IvaReserva(Porcentaje porcentaje, boolean exento) {

    /** Sin IVA: las reservas de antes de DEC-134, las de un alojamiento que no lo cobra y las de canales externos. */
    public static final IvaReserva NINGUNO = new IvaReserva(new Porcentaje(0), false);

    // DEC-134
    public IvaReserva {
        if (porcentaje == null) {
            throw new ReglaDominioException("El IVA de la reserva requiere su porcentaje");
        }
    }

    /**
     * El IVA de una reserva nueva o modificada con la configuración del alojamiento de ese momento.
     *
     * @param documento el que decide la exención: el del huésped principal o, si aún no lo tiene, el del titular
     */
    // DEC-134
    public static IvaReserva para(ImpuestoAlojamiento impuesto, Documento documento) {
        if (!impuesto.cobraIva()) {
            return NINGUNO;
        }
        return new IvaReserva(impuesto.porcentaje(), esPasaporte(documento));
    }

    /** Lo que muestra una cotización, antes de saber quién llega: con IVA si el alojamiento lo cobra. */
    public static IvaReserva paraCotizar(ImpuestoAlojamiento impuesto) {
        return impuesto.cobraIva() ? new IvaReserva(impuesto.porcentaje(), false) : NINGUNO;
    }

    /**
     * La misma tasa congelada, con la exención según el documento de quien llega (DEC-134). Una reserva creada sin
     * IVA sigue sin IVA, aunque el alojamiento lo cobre ahora.
     */
    public IvaReserva revisadoCon(Documento documento) {
        return cobra() ? new IvaReserva(porcentaje, esPasaporte(documento)) : this;
    }

    /** El IVA sobre el valor de las noches, redondeado al peso; cero si no se cobra o está exenta. */
    public Dinero sobre(Dinero valorNoches) {
        return cobra() && !exento ? valorNoches.porcentaje(porcentaje) : Dinero.CERO;
    }

    /** Si el alojamiento lo cobraba al crear la reserva. */
    public boolean cobra() {
        return porcentaje.valor() > 0;
    }

    private static boolean esPasaporte(Documento documento) {
        return documento != null && documento.tipo() == TipoDocumento.PASAPORTE;
    }
}
