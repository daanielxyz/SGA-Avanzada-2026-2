package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Bloqueo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Datos comunes de las pruebas de los servicios que cruzan Apartamento y Reservas.
 */
final class ReservasDePrueba {

    static final ApartamentoId APT = new ApartamentoId("APT-101");

    private ReservasDePrueba() {
    }

    static LocalDate dia(int d) {
        return LocalDate.of(2026, 12, d);
    }

    static Apartamento apartamento(boolean activo, List<Bloqueo> bloqueos) {
        return new Apartamento(APT, new AlojamientoId("ALO-1"), "Apto 101", null, new Capacidad(4),
                new Dormitorio(2), EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt-101.jpg", true)),
                List.of(new Caracteristica("Balcón")), bloqueos, activo);
    }

    static Apartamento apartamentoActivo() {
        return apartamento(true, List.of());
    }

    /** Entrada 15:00, salida 11:00: ventana de 4 horas para preparar el apartamento el mismo día. */
    static ParametrosAlojamiento parametros(int horasPreparacion) {
        return new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0),
                Duration.ofHours(horasPreparacion), Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30),
                2, 1, 2, 2);
    }

    static Reserva reserva(String codigo, ApartamentoId apartamento, int entrada, int salida, EstadoReserva estado) {
        Estancia estancia = new Estancia(dia(entrada), dia(salida));
        List<LineaCotizacion> desglose = estancia.noches().stream()
                .map(n -> new LineaCotizacion(n, new TemporadaId("TEM-BASE"), Dinero.de(100_000), 1,
                        Dinero.de(100_000)))
                .toList();
        return new Reserva(new ReservaId(codigo), apartamento, new TitularId("TIT-1"), estancia, estado,
                CanalOrigen.DIRECTO, null, null,
                List.of(new Ocupante(new OcupanteId("OCU-1"), "Ana", LocalDate.of(1990, 1, 1), null, null)),
                null, null, null, Dinero.de(100_000L * desglose.size()), desglose, new PoliticaId("POL-1"),
                dia(1).atStartOfDay());
    }

    static Reserva confirmada(String codigo, int entrada, int salida) {
        return reserva(codigo, APT, entrada, salida, EstadoReserva.CONFIRMADA);
    }
}
