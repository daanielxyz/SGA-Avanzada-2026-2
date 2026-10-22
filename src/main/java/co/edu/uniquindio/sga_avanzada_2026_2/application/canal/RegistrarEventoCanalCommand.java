package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

/**
 * Entrada de {@link RegistrarEventoCanalUseCase} (BIT-01).
 *
 * @param operacion nombre de {@code OperacionCanal}
 * @param sentido   ENTRANTE o SALIENTE
 * @param cargaUtil el mensaje, ya sin credenciales (BIT-04)
 * @param resultado nombre de {@code ResultadoEvento}
 * @param detalle   opcional: motivo del rechazo o del error
 */
public record RegistrarEventoCanalCommand(String canalId, String operacion, String sentido, String cargaUtil,
                                          String resultado, String detalle) {
}
