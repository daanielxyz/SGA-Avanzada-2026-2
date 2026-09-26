package co.edu.uniquindio.sga_avanzada_2026_2.domain.repository;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.IdentificadorApartamento;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository {

    List<Reserva> buscarActivasByApartamento(IdentificadorApartamento apartamento);
    public Optional<Reserva> obtenerPorCodigo(CodigoReserva codigo);
    public  void guardar(Reserva reserva);
}
