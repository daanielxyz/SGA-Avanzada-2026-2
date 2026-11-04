package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

/**
 * Una reserva con el nombre de su titular, para que recepción la identifique sin abrir otra pantalla (CU-25 · CU-49).
 *
 * @param titularNombre el nombre vigente del titular; la reserva solo guarda su id (referencia entre agregados)
 */
public record ReservaConTitularResult(ReservaResult reserva, String titularNombre) {
}
