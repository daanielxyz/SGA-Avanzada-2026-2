package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

/**
 * Puerto hacia el Ministerio (DEC-130). El huésped principal va primero y devuelve el código con el que se envía cada
 * acompañante. Con la API real o en modo local, sin salir a internet.
 */
public interface TransmisorTra {

    /**
     * @return el código que el Ministerio le dio al huésped principal
     * @throws TraNoDisponibleException si no se pudo enviar
     */
    String enviarPrincipal(ReporteTra reporte);

    /**
     * @param codigoPrincipal el que devolvió {@link #enviarPrincipal}
     * @throws TraNoDisponibleException si no se pudo enviar
     */
    void enviarAcompanante(ReporteTra reporte, HuespedTra acompanante, String codigoPrincipal);
}
