package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;

/** Completa los datos del TRA de todo el grupo, para las pruebas que llegan hasta la salida (BL-07). */
public final class DatosTraDePrueba {

    public static final DatosViaje VIAJE = new DatosViaje(Genero.FEMENINO, "Colombia", "Pereira", "Pereira",
            "Salento", MotivoViaje.TURISMO_VACACIONES);

    private DatosTraDePrueba() {
    }

    public static void completar(Reserva reserva) {
        int i = 0;
        for (Ocupante o : reserva.ocupantes()) {
            i++;
            Documento documento = o.documento() != null ? null : new Documento(TipoDocumento.CC, "7700" + i);
            reserva.completarDatosOcupante(o.id(), o.nombres().split(" ")[0], "Ríos", null, documento, "Colombiana",
                    VIAJE);
        }
    }
}
