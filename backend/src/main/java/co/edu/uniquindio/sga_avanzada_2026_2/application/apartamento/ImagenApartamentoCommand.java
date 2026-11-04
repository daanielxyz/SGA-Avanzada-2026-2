package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

/**
 * Entrada de los casos de uso de las imágenes de un apartamento (CU-31): la imagen se identifica por su url (IMG-04).
 */
public record ImagenApartamentoCommand(String codigo, String url) {
}
