package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.DeclararFueraDeServicioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.MarcarPreparadoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.IniciarPreparacionUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActivarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoCommand.ImagenCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RegistrarBloqueoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.RegistrarBloqueoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.CanalResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.canal.RegistrarCanalUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CalendarioResult.TemporadaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarCalendarioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.CobroParqueadero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DatosViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.EstadoReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Registro;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Salida;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.SalidaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Rol;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.Usuario;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Completa los datos de prueba de la sección 12.3 del proyecto sobre los de {@link CargadorDatosDemo}: los 10
 * apartamentos con sus tarifas, un usuario por rol, reservas en todos los estados y por los tres canales, un bloqueo
 * vigente y un apartamento fuera de servicio. Las fechas se calculan desde hoy para que sirvan en cualquier arranque.
 * Además deja servicios con precio, una reserva confirmada con un carro y una mascota, y un consumo de dos desayunos en
 * la reserva en curso, para probar el folio y los extras (BL-12 · BL-23).
 * <p>
 * Las reservas se arman con el constructor de reconstrucción del dominio, que valida las invariantes pero no las reglas
 * de creación (DEC-06), porque RN-04 no deja crear una reserva pasada y el historial necesita alguna. Si ya existe
 * {@link #ULTIMO}, no hace nada; si algo falla, no deja nada a medias y el backend arranca igual.
 */
@Component
@Order(3) // después de CargadorDatosDemo: usa sus temporadas
@ConditionalOnProperty(name = "sga.datos-demo", havingValue = "true")
public class CargadorEscenarioDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargadorEscenarioDemo.class);

    static final String ULTIMO = "APT-110";
    static final String CORREO_ADMINISTRADOR = "admin.demo@example.com";
    static final String CORREO_RECEPCION = "recepcion.demo@example.com";
    static final String CORREO_SERVICIO = "servicio.demo@example.com";
    static final String CORREO_HUESPED_ANA = "ana.demo@example.com";
    static final String CORREO_HUESPED_LUIS = "luis.demo@example.com";

    private static final String FOTOS = "https://picsum.photos/seed/";

    private record Nuevo(String codigo, String nombre, int capacidad, int dormitorios, long base,
                         List<String> dotacion) {
    }

    // TODO(equipo): reemplazar por el inventario de la Ficha del Alojamiento (A.2)
    private static final List<Nuevo> NUEVOS = List.of(
            new Nuevo("APT-105", "Loft Yarumo", 2, 1, 115_000, List.of("Wifi", "Cocina", "Agua caliente")),
            new Nuevo("APT-106", "Terraza Cafetal", 3, 2, 105_000, List.of("Wifi", "Terraza", "Agua caliente")),
            new Nuevo("APT-107", "Balcón del Parque", 4, 2, 100_000, List.of("Wifi", "Balcón", "Televisor")),
            new Nuevo("APT-108", "Estudio Bahareque", 2, 1, 110_000, List.of("Wifi", "Cocina", "Televisor")),
            new Nuevo("APT-109", "Patio de los Guayacanes", 4, 2, 95_000, List.of("Wifi", "Patio", "Lavadora")),
            new Nuevo(ULTIMO, "Mirador del Cocora", 3, 2, 125_000, List.of("Wifi", "Vista al valle", "Chimenea")));

    static final String DESAYUNO = "Desayuno";

    private record ServicioDemo(String nombre, long valor) {
    }

    /** Servicios que se cobran en el folio (BL-12); el Parqueadero de {@code sga.alojamiento} no genera cargo. */
    private static final List<ServicioDemo> SERVICIOS = List.of(new ServicioDemo(DESAYUNO, 25_000),
            new ServicioDemo("Agua", 3_000), new ServicioDemo("Lavandería", 18_000));

    private final AlojamientoRepository alojamientos;
    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final TitularRepository titulares;
    private final TarifaRepository tarifas;
    private final CalendarioTemporadasRepository calendarios;
    private final PoliticaCancelacionRepository politicas;
    private final UsuarioRepository usuarios;
    private final CrearApartamentoUseCase crearApartamento;
    private final DefinirTarifaUseCase definirTarifa;
    private final ActivarApartamentoUseCase activarApartamento;
    private final ConsultarCalendarioUseCase consultarCalendario;
    private final IniciarPreparacionUseCase iniciarPreparacion;
    private final MarcarPreparadoUseCase marcarPreparado;
    private final DeclararFueraDeServicioUseCase declararFueraDeServicio;
    private final RegistrarBloqueoUseCase registrarBloqueo;
    private final RegistrarCanalUseCase registrarCanal;
    private final TarificacionDomainService tarificacion;
    private final GeneradorCodigos codigos;
    private final PasswordEncoder cifrador;
    private final TransactionTemplate transaccion;
    private final SgaPropiedades propiedades;
    private final Clock reloj;
    private final String clave;

    public CargadorEscenarioDemo(AlojamientoRepository alojamientos, ApartamentoRepository apartamentos,
                                 ReservaRepository reservas, FolioRepository folios, TitularRepository titulares,
                                 TarifaRepository tarifas, CalendarioTemporadasRepository calendarios,
                                 PoliticaCancelacionRepository politicas, UsuarioRepository usuarios,
                                 CrearApartamentoUseCase crearApartamento, DefinirTarifaUseCase definirTarifa,
                                 ActivarApartamentoUseCase activarApartamento,
                                 ConsultarCalendarioUseCase consultarCalendario,
                                 IniciarPreparacionUseCase iniciarPreparacion,
                                 MarcarPreparadoUseCase marcarPreparado,
                                 DeclararFueraDeServicioUseCase declararFueraDeServicio,
                                 RegistrarBloqueoUseCase registrarBloqueo, RegistrarCanalUseCase registrarCanal,
                                 TarificacionDomainService tarificacion, GeneradorCodigos codigos,
                                 PasswordEncoder cifrador, TransactionTemplate transaccion,
                                 SgaPropiedades propiedades, Clock reloj,
                                 @Value("${sga.demo.clave:Demo12345}") String clave) {
        this.alojamientos = alojamientos;
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.folios = folios;
        this.titulares = titulares;
        this.tarifas = tarifas;
        this.calendarios = calendarios;
        this.politicas = politicas;
        this.usuarios = usuarios;
        this.crearApartamento = crearApartamento;
        this.definirTarifa = definirTarifa;
        this.activarApartamento = activarApartamento;
        this.consultarCalendario = consultarCalendario;
        this.iniciarPreparacion = iniciarPreparacion;
        this.marcarPreparado = marcarPreparado;
        this.declararFueraDeServicio = declararFueraDeServicio;
        this.registrarBloqueo = registrarBloqueo;
        this.registrarCanal = registrarCanal;
        this.tarificacion = tarificacion;
        this.codigos = codigos;
        this.cifrador = cifrador;
        this.transaccion = transaccion;
        this.propiedades = propiedades;
        this.reloj = reloj;
        this.clave = clave;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        String alojamientoId = propiedades.alojamiento().id();
        if (alojamientos.buscarPorId(new AlojamientoId(alojamientoId)).isEmpty()) {
            return;
        }
        try {
            transaccion.executeWithoutResult(estado -> completarConfiguracion(alojamientoId));
        } catch (RuntimeException error) {
            LOG.warn("Datos de prueba: no se agregaron los servicios con precio ({})", error.getMessage(), error);
        }
        if (apartamentos.buscarPorCodigo(new ApartamentoId(ULTIMO)).isPresent()) {
            return;
        }
        try {
            transaccion.executeWithoutResult(estado -> cargar(alojamientoId));
            LOG.info("Datos de prueba (12.3): 10 apartamentos, un usuario por rol y reservas en todos los estados");
        } catch (RuntimeException error) {
            LOG.warn("Datos de prueba (12.3): no se cargaron ({}). El backend sigue sin ellos", error.getMessage(),
                    error);
        }
    }

    /**
     * Deja el alojamiento listo para probar el folio y los extras (BL-12 · BL-23): servicios que se cobran y, en una
     * base creada antes de V15 (sin parqueadero ni mascotas), los valores iniciales de {@code sga.parametros}. Corre
     * una sola vez: si ya existe {@link #DESAYUNO}, no toca nada, para no deshacer lo que configuró el administrador.
     */
    private void completarConfiguracion(String alojamientoId) {
        Alojamiento alojamiento = alojamientos.buscarPorId(new AlojamientoId(alojamientoId)).orElseThrow();
        if (alojamiento.serviciosAdicionales().stream().anyMatch(s -> s.nombre().equals(DESAYUNO))) {
            return;
        }
        SERVICIOS.forEach(s -> alojamiento.agregarServicioAdicional(new ServicioAdicional(
                new ServicioAdicionalId(codigos.siguiente(SerieCodigo.SERVICIO)), s.nombre(), true,
                Dinero.de(s.valor()), true)));
        alojamiento.cambiarParametros(conParqueaderoYMascotas(alojamiento.parametros(), propiedades.parametros()));
        alojamientos.guardar(alojamiento);
        LOG.info("Datos de prueba: servicios con precio, parqueadero y mascotas listos para el folio");
    }

    /**
     * Los parámetros con el parqueadero y las mascotas de {@code sga.parametros} si el alojamiento no tiene ninguno de
     * los dos (una base creada antes de V15 quedó con cupo 0 y sin mascotas); si no, los mismos.
     */
    static ParametrosAlojamiento conParqueaderoYMascotas(ParametrosAlojamiento actual,
                                                         SgaPropiedades.Parametros iniciales) {
        if (actual.cupoParqueadero() > 0 || actual.aceptaMascotas()) {
            return actual;
        }
        return new ParametrosAlojamiento(actual.umbralEdadFacturable(), actual.horaEntrada(), actual.horaSalida(),
                actual.tiempoPreparacion(), actual.plazoConfirmacion(), actual.horaLimiteNoShow(), actual.anticipo(),
                actual.minimoMediosPago(), actual.minimoServiciosAdicionales(), actual.minimoTemporadas(),
                actual.minimoTramosCancelacion(), actual.minimoCapacidadesDistintas(), actual.pagoEnLinea(),
                actual.plazoPagoEnLinea(), iniciales.cupoParqueadero(), iniciales.aceptaMascotas(),
                iniciales.limiteMascotas(), new Dinero(iniciales.cargoMascota()),
                CobroParqueadero.valueOf(iniciales.cobroParqueadero()), new Dinero(iniciales.cargoCarro()),
                new Dinero(iniciales.cargoMoto()));
    }

    private void cargar(String alojamientoId) {
        crearApartamentosNuevos(alojamientoId);
        Usuario recepcion = crearUsuario(alojamientoId, "Recepción Demo", CORREO_RECEPCION, Rol.RECEPCIONISTA);
        crearUsuario(alojamientoId, "Servicio Demo", CORREO_SERVICIO, Rol.PERSONAL_SERVICIO);
        crearUsuario(alojamientoId, "Administración Demo", CORREO_ADMINISTRADOR, Rol.ADMINISTRADOR);

        // Todos quedan listos para entregar (RN-11), salvo los de abajo
        List.of("APT-101", "APT-102", "APT-103", "APT-104", "APT-105", "APT-106", "APT-107", "APT-108", "APT-109")
                .forEach(this::prepararParaEntregar);
        // El de la reserva en curso; ocuparlo no es una acción de la API, sino parte de registrar la llegada
        Apartamento ocupado = apartamentos.buscarPorCodigo(new ApartamentoId("APT-103")).orElseThrow();
        ocupado.ocupar();
        apartamentos.guardar(ocupado);
        declararFueraDeServicio.ejecutar(ULTIMO, "Cambio de la tubería del baño");
        LocalDate hoy = LocalDate.now(reloj);
        registrarBloqueo.ejecutar(new RegistrarBloqueoCommand("APT-109", hoy.plusDays(20), hoy.plusDays(25),
                "Mantenimiento de la cubierta"));

        registrarCanal.ejecutar(new RegistrarCanalCommand(alojamientoId, "Portal web", "PORTAL", null));
        registrarCanal.ejecutar(new RegistrarCanalCommand(alojamientoId, "Recepción", "DIRECTO", null));
        CanalResult booking = registrarCanal.ejecutar(new RegistrarCanalCommand(alojamientoId, "Booking", "EXTERNO",
                "BOOKING_API_KEY"));

        Titular ana = crearTitular(alojamientoId, "Ana Demo", "10001", CORREO_HUESPED_ANA);
        Titular luis = crearTitular(alojamientoId, "Luis Demo", "10002", CORREO_HUESPED_LUIS);
        crearReservas(alojamientoId, ana, luis, new CanalId(booking.id()), new UsuarioId(recepcion.id()), hoy);
    }

    private void crearApartamentosNuevos(String alojamientoId) {
        List<TemporadaResult> temporadas = consultarCalendario.ejecutar(alojamientoId).temporadas().stream()
                .filter(TemporadaResult::activa)
                .toList();
        NUEVOS.forEach(n -> {
            crearApartamento.ejecutar(new CrearApartamentoCommand(n.codigo(), alojamientoId, n.nombre(),
                    "Apartamento " + n.nombre() + " en Puerta al Sol.", n.capacidad(), n.dormitorios(),
                    List.of(new ImagenCommand(FOTOS + n.codigo() + "-a/1200/675", true),
                            new ImagenCommand(FOTOS + n.codigo() + "-b/1200/675", false)),
                    n.dotacion()));
            temporadas.forEach(t -> definirTarifa.ejecutar(new DefinirTarifaCommand(n.codigo(), t.id(),
                    BigDecimal.valueOf(t.esBase() ? n.base() : n.base() * 13 / 10))));
            activarApartamento.ejecutar(n.codigo());
        });
    }

    private void prepararParaEntregar(String codigo) {
        iniciarPreparacion.ejecutar(codigo);
        marcarPreparado.ejecutar(codigo);
    }

    private Usuario crearUsuario(String alojamientoId, String nombre, String correo, Rol rol) {
        Usuario usuario = Usuario.crear(new UsuarioId(codigos.siguiente(SerieCodigo.USUARIO)), alojamientoId, nombre,
                new Correo(correo), cifrador.encode(clave), rol);
        usuarios.save(usuario);
        return usuario;
    }

    private Titular crearTitular(String alojamientoId, String nombre, String documento, String correo) {
        Titular titular = new Titular(new TitularId(codigos.siguiente(SerieCodigo.TITULAR)),
                new AlojamientoId(alojamientoId), nombre, new Documento(TipoDocumento.CC, documento),
                new Correo(correo), "3001234567");
        titulares.guardar(titular);
        return titular;
    }

    private void crearReservas(String alojamientoId, Titular ana, Titular luis, CanalId booking, UsuarioId recepcion,
                               LocalDate hoy) {
        AlojamientoId id = new AlojamientoId(alojamientoId);
        Alojamiento alojamiento = alojamientos.buscarPorId(id).orElseThrow();
        Contexto c = new Contexto(alojamiento, calendarios.buscarPorAlojamiento(id).orElseThrow(),
                politicas.buscarVigente(id).orElseThrow().id(), hoy, recepcion);
        LocalDateTime ahora = LocalDateTime.now(reloj);

        reserva(c, "APT-101", ana, hoy.plusDays(30), hoy.plusDays(33), EstadoReserva.PENDIENTE, CanalOrigen.DIRECTO,
                null, null, ahora);
        Reserva confirmada = reserva(c, "APT-102", ana, hoy.plusDays(10), hoy.plusDays(13), EstadoReserva.CONFIRMADA,
                CanalOrigen.PORTAL, null, null, ahora.minusDays(2));
        declararExtras(c, confirmada, 1, 1); // recepción los puede corregir antes de la llegada (BL-23)
        pagar(c, confirmada, confirmada.valorTotal().porcentaje(alojamiento.parametros().anticipo()));
        Reserva enCurso = reserva(c, "APT-103", luis, hoy.minusDays(1), hoy.plusDays(2), EstadoReserva.EN_CURSO,
                CanalOrigen.DIRECTO, null, null, ahora.minusDays(5));
        pagar(c, enCurso, enCurso.valorTotal());
        consumir(c, enCurso, DESAYUNO, 2); // queda con saldo pendiente para probar el cobro en la salida (BL-12)
        Reserva finalizada = reserva(c, "APT-104", luis, hoy.minusDays(10), hoy.minusDays(7), EstadoReserva.FINALIZADA,
                CanalOrigen.DIRECTO, null, null, ahora.minusDays(20));
        pagar(c, finalizada, finalizada.valorTotal());
        reserva(c, "APT-105", ana, hoy.plusDays(40), hoy.plusDays(42), EstadoReserva.CANCELADA, CanalOrigen.PORTAL,
                null, null, ahora.minusDays(3));
        reserva(c, "APT-106", luis, hoy.minusDays(3), hoy.minusDays(1), EstadoReserva.NO_SHOW, CanalOrigen.DIRECTO,
                null, null, ahora.minusDays(8));
        reserva(c, "APT-107", luis, hoy.plusDays(15), hoy.plusDays(17), EstadoReserva.CONFIRMADA, CanalOrigen.EXTERNO,
                booking, "BK-DEMO-0001", ahora.minusDays(1));
    }

    private record Contexto(Alojamiento alojamiento, CalendarioTemporadas calendario, PoliticaId politica,
                            LocalDate hoy, UsuarioId recepcion) {
    }

    private Reserva reserva(Contexto c, String apartamento, Titular titular, LocalDate entrada, LocalDate salida,
                            EstadoReserva estado, CanalOrigen canal, CanalId canalId, String idExterno,
                            LocalDateTime creadaEn) {
        ApartamentoId codigo = new ApartamentoId(apartamento);
        Estancia estancia = new Estancia(entrada, salida);
        List<Ocupante> ocupantes = List.of(new Ocupante(new OcupanteId("OCU-1"), titular.nombre(), null, null,
                LocalDate.of(1990, 5, 1), titular.documento(), "CO", DatosViaje.VACIOS, true));
        Cotizacion cotizacion = tarificacion.calcularValorEstancia(codigo, estancia, ocupantes,
                c.alojamiento().parametros().umbralEdadFacturable(), c.calendario(),
                tarifas.buscarVigentesPorApartamento(codigo, c.hoy()));
        boolean inicio = estado == EstadoReserva.EN_CURSO || estado == EstadoReserva.FINALIZADA;
        List<Registro> registros = inicio
                ? List.of(new Registro(new RegistroId("REG-1"), entrada.atTime(LocalTime.of(15, 0)), c.recepcion(), false,
                        null))
                : List.of();
        Salida finDeEstancia = estado == EstadoReserva.FINALIZADA
                ? new Salida(new SalidaId("SAL-1"), salida.atTime(LocalTime.of(11, 0)), c.recepcion())
                : null;
        Reserva reserva = new Reserva(new ReservaId(codigos.siguiente(SerieCodigo.RESERVA)), codigo, titular.id(),
                estancia, estado, canal, canalId, idExterno, ocupantes, registros, finDeEstancia,
                LocalTime.of(16, 0), cotizacion.total(), cotizacion.desglose(), c.politica(), creadaEn);
        reservas.guardar(reserva);
        folios.guardar(Folio.abrir(new FolioId(codigos.siguiente(SerieCodigo.FOLIO)), reserva.codigo(),
                reserva.valorTotal(), creadaEn.toLocalDate()));
        return reserva;
    }

    private void declararExtras(Contexto c, Reserva reserva, int carros, int mascotas) {
        reserva.declararExtras(carros, 0, mascotas, c.alojamiento().parametros().calcularExtras(carros, 0, mascotas,
                reserva.estancia().noches().size()));
        reservas.guardar(reserva);
        Folio folio = folios.buscarPorReserva(reserva.codigo()).orElseThrow();
        folio.cargarExtras(reserva.extras(), c.hoy());
        folios.guardar(folio);
    }

    private void consumir(Contexto c, Reserva reserva, String servicio, int cantidad) {
        Folio folio = folios.buscarPorReserva(reserva.codigo()).orElseThrow();
        c.alojamiento().serviciosAdicionales().stream()
                .filter(s -> s.nombre().equals(servicio))
                .findFirst()
                .ifPresent(s -> folio.agregarServicioAdicional(s.nombre(), s.valor(), cantidad, c.hoy()));
        folios.guardar(folio);
    }

    private void pagar(Contexto c, Reserva reserva, Dinero monto) {
        List<MedioPago> medios = c.alojamiento().mediosPago();
        Folio folio = folios.buscarPorReserva(reserva.codigo()).orElseThrow();
        folio.registrarPago(medios.getFirst(), monto, c.hoy(), c.hoy(), medios);
        folios.guardar(folio);
    }
}
