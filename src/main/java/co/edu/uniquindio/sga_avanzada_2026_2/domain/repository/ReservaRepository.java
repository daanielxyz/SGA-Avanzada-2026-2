package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.IdentificadorApartamento;

import java.util.List;

public interface ReservaRepository {

    List<Reserva> buscarActivasByApartamento(IdentificadorApartamento apartamento);
}
