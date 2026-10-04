package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;

/**
 * Calculado (no se guarda): resultado de verificar la disponibilidad de un apartamento.
 */
public record Disponibilidad(ApartamentoId apartamentoId, Estancia estancia, boolean disponible, String motivo) {
}
