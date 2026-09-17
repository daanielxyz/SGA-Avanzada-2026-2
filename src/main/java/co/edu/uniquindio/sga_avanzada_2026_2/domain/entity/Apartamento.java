package co.edu.uniquindio.sga_avanzada_2026_2.domain.entity;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.IdentificadorApartamento;

public class Apartamento {
    private final IdentificadorApartamento identificadorApartamento;

    public Apartamento(IdentificadorApartamento identificadorApartamento) {
        this.identificadorApartamento = identificadorApartamento;
    }


    public boolean admite(int totalOcupantes) {
        return true;

    }

    public boolean estaActivo() {
        return true;

    }

    public IdentificadorApartamento getIdentificacion() {
        return identificadorApartamento;
    }
}
