package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ModoPagoEnLinea;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Ubicacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * El alojamiento del despliegue para las pruebas REST: ALO-1 como lo dejaría el arranque (DEC-53), con el Desayuno
 * a 25.000, EFECTIVO y TARJETA, la política de dos tramos y la temporada base. Sus ids terminan en 900 para no
 * chocar con los que entregan las secuencias, que no se devuelven al deshacer la transacción de cada prueba.
 */
public final class AlojamientoDePrueba {

    public static final AlojamientoId ALO = new AlojamientoId("ALO-1");
    public static final String DESAYUNO = "SRV-900";
    public static final String TEMPORADA_BASE = "TEM-900";
    public static final String POLITICA = "POL-900";

    private AlojamientoDePrueba() {
    }

    public static void guardar(AlojamientoRepository alojamientos, CalendarioTemporadasRepository calendarios,
                               PoliticaCancelacionRepository politicas) {
        alojamientos.guardar(new Alojamiento(ALO, "Puerta al Sol", "Frente al mar", "Santa Marta", "Calle 1 # 2-3",
                new Ubicacion(11.24, -74.21), null,
                new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                        Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), 2, 1, 2, 2, 2,
                        ModoPagoEnLinea.ANTICIPO, Duration.ofMinutes(30)),
                List.of(new ServicioAdicional(new ServicioAdicionalId(DESAYUNO), "Desayuno", true,
                        Dinero.de(25_000), true)),
                List.of(new MedioPago("EFECTIVO"), new MedioPago("TARJETA"))));
        calendarios.guardar(CalendarioTemporadas.crear(ALO, new TemporadaId(TEMPORADA_BASE), "Temporada base"));
        politicas.guardar(PoliticaCancelacion.crear(new PoliticaId(POLITICA), ALO,
                List.of(new TramoCancelacion(48, delTotal(0)), new TramoCancelacion(0, delTotal(50))), delTotal(100),
                2));
    }

    /** Hoy en Colombia, como lo ve el reloj de la aplicación (EST-06). */
    public static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    }

    /**
     * Un apartamento listo para vender y entregar: activo, PREPARADO y con tarifa de 100.000 por ocupante en la
     * temporada base.
     */
    public static void guardarApartamento(ApartamentoRepository apartamentos, TarifaRepository tarifas,
                                          String codigo, int capacidad) {
        ApartamentoId id = new ApartamentoId(codigo);
        apartamentos.guardar(new Apartamento(id, ALO, "Apartamento " + codigo, null, new Capacidad(capacidad),
                new Dormitorio(2), EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)),
                List.of(new Caracteristica("Balcón")), List.of(), true));
        tarifas.guardar(Tarifa.crear(new TarifaId("TAR-900-" + codigo), id, new TemporadaId(TEMPORADA_BASE),
                Dinero.de(100_000), hoy()));
    }

    private static Penalizacion delTotal(int porcentaje) {
        return Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(porcentaje));
    }
}
