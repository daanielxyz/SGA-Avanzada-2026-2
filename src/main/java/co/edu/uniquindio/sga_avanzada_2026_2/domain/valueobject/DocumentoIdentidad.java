package co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.exception.ReglaDominioException;

public record DocumentoIdentidad(String numero) {
    public DocumentoIdentidad{
        if(numero == null || numero.isEmpty() || numero.isBlank()){
            throw new ReglaDominioException("El documento de identidad es obligatorio");
        }
        numero = numero.trim();
    }
}
