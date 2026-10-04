package co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;

/**
 * Id tipado: código de negocio del apartamento.
 */
public record ApartamentoId(String valor) {

    public ApartamentoId {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDominioException("ApartamentoId es obligatorio");
        }
        if (!valor.matches("APT-\\d+")) {
            throw new ReglaDominioException("ApartamentoId con formato inválido (ej. APT-101): " + valor);
        }
    }
}
