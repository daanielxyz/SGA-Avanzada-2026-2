package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistence.inmemory;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.entity.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.repository.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.CodigoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.valueobject.IdentificadorApartamento;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class ReservaRepositoryInMemory implements ReservaRepository {

    private final HashMap<CodigoReserva,Reserva> reservas= new HashMap<>();

    @Override
    public List<Reserva> buscarActivasByApartamento(IdentificadorApartamento apartamento) {

        return reservas.values().stream()
                .filter(reserva -> reserva.getApartamento().equals(apartamento))
                .filter(reserva -> reserva.getEstado().estaActiva()).toList();
    }

    @Override
    public Optional<Reserva> obtenerPorCodigo(CodigoReserva codigo) {
        return Optional.ofNullable(reservas.get(codigo));
    }

    @Override
    public  void guardar(Reserva reserva) {
        reservas.put(reserva.getCodigo(),reserva);
    }
}
