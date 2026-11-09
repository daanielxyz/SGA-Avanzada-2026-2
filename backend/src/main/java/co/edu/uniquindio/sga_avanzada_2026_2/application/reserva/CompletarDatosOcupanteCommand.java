package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Entrada de {@link CompletarDatosOcupanteUseCase}: los datos del TRA de un ocupante (BL-07). El que llega nulo se
 * conserva como estaba.
 *
 * @param tipoDocumento nombre de {@code TipoDocumento}; junto con {@code numeroDocumento}
 * @param genero        nombre de {@code Genero}
 * @param motivoViaje   nombre de {@code MotivoViaje}
 */
public record CompletarDatosOcupanteCommand(String codigo, String ocupanteId, String nombres, String primerApellido,
                                            String segundoApellido, String tipoDocumento, String numeroDocumento,
                                            String nacionalidad, String genero, String paisResidencia,
                                            String ciudadResidencia, String ciudadProcedencia, String ciudadDestino,
                                            String motivoViaje) {
}
