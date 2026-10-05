package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementaciones falsas de los puertos para probar casos de uso sin Spring ni base de datos.
 */
final class RepositoriosEnMemoria {

    private RepositoriosEnMemoria() {
    }

    static final class Apartamentos implements ApartamentoRepository {

        final Map<ApartamentoId, Apartamento> guardados = new HashMap<>();

        @Override
        public void guardar(Apartamento apartamento) {
            guardados.put(apartamento.codigo(), apartamento);
        }

        @Override
        public Optional<Apartamento> buscarPorCodigo(ApartamentoId codigo) {
            return Optional.ofNullable(guardados.get(codigo));
        }
    }

    static final class Alojamientos implements AlojamientoRepository {

        final Map<AlojamientoId, Alojamiento> guardados = new HashMap<>();

        @Override
        public void guardar(Alojamiento alojamiento) {
            guardados.put(alojamiento.id(), alojamiento);
        }

        @Override
        public Optional<Alojamiento> buscarPorId(AlojamientoId id) {
            return Optional.ofNullable(guardados.get(id));
        }
    }

    static Alojamiento puertaAlSol(String id) {
        return new Alojamiento(new AlojamientoId(id), "Puerta al Sol", "Apartamentos frente al mar", "Santa Marta",
                "Calle 1 # 2-3", new Ubicacion(11.24, -74.21), null,
                new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                        Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, 2),
                List.of(new ServicioAdicional(new ServicioAdicionalId("SRV-1"), "Desayuno", true, Dinero.de(25_000), true)),
                List.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA")));
    }
}
