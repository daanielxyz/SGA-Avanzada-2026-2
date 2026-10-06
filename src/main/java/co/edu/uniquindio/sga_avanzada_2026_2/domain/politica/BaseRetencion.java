package co.edu.uniquindio.sga_avanzada_2026_2.domain.politica;

/**
 * Valor sobre el que se calcula una penalización; lo elige el administrador de cada alojamiento (DEC-41).
 */
public enum BaseRetencion {
    VALOR_TOTAL,      // valor congelado de la estancia
    PAGADO,           // pagos netos del folio (abonos − reversos)
    ANTICIPO_EXIGIDO  // valor total × porcentaje de anticipo del alojamiento
}
