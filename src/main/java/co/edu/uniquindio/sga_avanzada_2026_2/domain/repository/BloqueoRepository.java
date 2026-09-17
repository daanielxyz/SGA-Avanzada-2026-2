package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.IdentificadorApartamento;

import java.util.List;

public interface BloqueoRepository  {

    List<Bloqueo> buscarVigentesByApartamento(IdentificadorApartamento apartamento);
}
