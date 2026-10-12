package co.edu.uniquindio.sga_avanzada_2026_2.application;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarPago;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.RegistrarPagoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ConfirmarReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CrearReservaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegada;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.IndicarHoraLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegada;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.RegistrarLlegadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.EstadoOperativo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.RegistroLlegadaDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Tarifa;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Escenario común de las pruebas del folio, la llegada y la salida: el APT-101 de Puerta al Sol y una reserva de dos
 * adultos del 10 al 13 de octubre de 2026 por 600.000, creada el 1 de octubre. El anticipo exigido es el 30 %
 * (180.000). Cada paso usa los casos de uso reales con repositorios en memoria.
 */
public final class EscenarioEstancia {

    public static final String RECEPCION = "USR-recepcion";
    public static final String ADMIN = "USR-admin";
    public static final LocalDate ENTRADA = LocalDate.of(2026, 10, 10);
    public static final Clock AL_CREAR = RepositoriosEnMemoria.RELOJ;
    public static final Clock AL_LLEGAR = relojEn(ENTRADA.atTime(15, 30));
    public static final Clock ALOJADO = relojEn(ENTRADA.plusDays(1).atTime(9, 0));
    public static final Clock AL_SALIR = relojEn(ENTRADA.plusDays(3).atTime(10, 0));

    private static final AlojamientoId ALO = new AlojamientoId("ALO-1");
    private static final ApartamentoId APT_101 = new ApartamentoId("APT-101");

    public final RepositoriosEnMemoria.Reservas reservas = new RepositoriosEnMemoria.Reservas();
    public final RepositoriosEnMemoria.Folios folios = new RepositoriosEnMemoria.Folios();
    public final RepositoriosEnMemoria.Titulares titulares = new RepositoriosEnMemoria.Titulares();
    public final RepositoriosEnMemoria.Apartamentos apartamentos = new RepositoriosEnMemoria.Apartamentos();
    public final RepositoriosEnMemoria.Alojamientos alojamientos = new RepositoriosEnMemoria.Alojamientos();
    public final RepositoriosEnMemoria.Calendarios calendarios = new RepositoriosEnMemoria.Calendarios();
    public final RepositoriosEnMemoria.Tarifas tarifas = new RepositoriosEnMemoria.Tarifas();
    public final RepositoriosEnMemoria.Politicas politicas = new RepositoriosEnMemoria.Politicas();
    public final RepositoriosEnMemoria.Canales canales = new RepositoriosEnMemoria.Canales();
    public final RepositoriosEnMemoria.Conflictos conflictos = new RepositoriosEnMemoria.Conflictos();
    public final RepositoriosEnMemoria.Eventos eventos = new RepositoriosEnMemoria.Eventos();
    public final RepositoriosEnMemoria.Novedades novedades = new RepositoriosEnMemoria.Novedades();
    public final RepositoriosEnMemoria.Codigos codigos = new RepositoriosEnMemoria.Codigos();
    public final CargadorReserva cargador = new CargadorReserva(reservas, folios, apartamentos, alojamientos,
            calendarios, titulares, codigos);

    public EscenarioEstancia() {
        alojamientos.guardar(RepositoriosEnMemoria.puertaAlSol("ALO-1"));
        calendarios.guardar(CalendarioTemporadas.crear(ALO, new TemporadaId("TEM-1"), "Base"));
        apartamentos.guardar(new Apartamento(APT_101, ALO, "Apto 101", null, new Capacidad(4), new Dormitorio(2),
                EstadoOperativo.PREPARADO, List.of(new Imagen("https://cdn.sga.co/apt.jpg", true)),
                List.of(new Caracteristica("Balcón")), List.of(), true));
        tarifas.guardar(Tarifa.crear(new TarifaId("TAR-1"), APT_101, new TemporadaId("TEM-1"), Dinero.de(100_000),
                LocalDate.now(AL_CREAR)));
        Penalizacion total = Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(100));
        Penalizacion nada = Penalizacion.porcentaje(BaseRetencion.VALOR_TOTAL, new Porcentaje(0));
        politicas.guardar(PoliticaCancelacion.crear(new PoliticaId("POL-1"), ALO,
                List.of(new TramoCancelacion(48, nada), new TramoCancelacion(0, total)), total, 2));
    }

    public static Clock relojEn(LocalDateTime momento) {
        return Clock.fixed(momento.atZone(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota"));
    }

    /** Crea la reserva PENDIENTE con su folio y devuelve su código. */
    public String reservaPendiente() {
        TitularCommand ana = new TitularCommand("Ana Ríos", "CC", "1094", "ana@correo.co", null);
        return new CrearReserva(cargador, reservas, folios, titulares, tarifas, politicas,
                new DisponibilidadDomainService(), new TarificacionDomainService(), codigos, AL_CREAR)
                .ejecutar(new CrearReservaCommand("APT-101", ENTRADA, ENTRADA.plusDays(3), "DIRECTO", ana,
                        List.of(new OcupanteCommand("Ana Ríos", LocalDate.of(1990, 5, 1), "CC", "1094", "Colombiana"),
                                new OcupanteCommand("Luis Ríos", LocalDate.of(1988, 3, 2), null, null, null)),
                        null))
                .codigo();
    }

    /** Reserva CONFIRMADA: indicó su hora de llegada y pagó el anticipo de 180.000 en efectivo. */
    public String reservaConfirmada() {
        String codigo = reservaPendiente();
        new IndicarHoraLlegada(cargador, reservas)
                .ejecutar(new IndicarHoraLlegadaCommand(codigo, LocalTime.of(16, 0)));
        pagar(codigo, 180_000, AL_CREAR);
        new ConfirmarReserva(cargador, reservas).ejecutar(codigo);
        return codigo;
    }

    /** Reserva EN_CURSO: el grupo llegó el 10 de octubre a las 15:30. */
    public String reservaEnCurso() {
        String codigo = reservaConfirmada();
        registrarLlegada(AL_LLEGAR).ejecutar(new RegistrarLlegadaCommand(codigo, RECEPCION));
        return codigo;
    }

    public RegistrarLlegada registrarLlegada(Clock reloj) {
        return new RegistrarLlegada(cargador, reservas, apartamentos, new RegistroLlegadaDomainService(), reloj);
    }

    /** Abona en efectivo con la fecha del reloj. */
    public void pagar(String codigo, long monto, Clock reloj) {
        new RegistrarPago(cargador, folios, reloj).ejecutar(new RegistrarPagoCommand(codigo, "EFECTIVO",
                BigDecimal.valueOf(monto), LocalDate.now(reloj)));
    }

    public Reserva reserva(String codigo) {
        return reservas.buscarPorCodigo(new ReservaId(codigo)).orElseThrow();
    }

    public Folio folio(String codigo) {
        return folios.buscarPorReserva(new ReservaId(codigo)).orElseThrow();
    }

    public Apartamento apartamento() {
        return apartamentos.buscarPorCodigo(APT_101).orElseThrow();
    }
}
