package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;

/**
 * VO: lo que el TRA pide de cada huésped (Resolución 409 de 2022, DEC-130). El tipo de documento va como lo escribe
 * el Ministerio («C.C», «Pasaporte»).
 *
 * @param apellidos el primero y el segundo, juntos
 */
public record HuespedTra(OcupanteId ocupante, String tipoDocumento, String numeroDocumento, String nombres,
                         String apellidos, String ciudadResidencia, String ciudadProcedencia) {

    // DEC-130
    public HuespedTra {
        if (ocupante == null || vacio(tipoDocumento) || vacio(numeroDocumento) || vacio(nombres) || vacio(apellidos)
                || vacio(ciudadResidencia) || vacio(ciudadProcedencia)) {
            throw new ReglaDominioException("Faltan datos del TRA del huésped");
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
