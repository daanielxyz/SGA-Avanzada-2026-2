package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ActivarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoCommand.ImagenCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.CrearApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaCommand.TarifaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.AgregarTemporadaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.CalendarioResult.TemporadaResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.ConsultarCalendarioUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.tarifa.DefinirTarifaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.function.Function;

/**
 * Datos de ejemplo para probar a mano y para las demos (DEC-81): cuatro apartamentos activos, con fotos, dotación y
 * tarifas en la temporada base y en dos temporadas altas. Solo con {@code SGA_DATOS_DEMO=true}, nunca en un servidor
 * real. Se cargan con los mismos casos de uso del administrador, así que cumplen las reglas del dominio; si ya están,
 * no hace nada, y si algo falla, no deja nada a medias y el backend arranca igual.
 */
@Component
@Order(2) // después de InicializadorAlojamiento: necesita el alojamiento y su calendario
@ConditionalOnProperty(name = "sga.datos-demo", havingValue = "true")
public class CargadorDatosDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargadorDatosDemo.class);

    /** Si este existe, los datos de ejemplo ya se cargaron (o alguien creó apartamentos a mano): no se toca nada. */
    static final String PRIMERO = "APT-101";

    // Fotos de relleno de picsum.photos (una imagen fija por semilla); las reales las sube el administrador
    private static final String FOTOS = "https://picsum.photos/seed/";

    /**
     * Capacidades distintas a propósito: la Ficha exige variedad entre los activos (CAP-05).
     *
     * @param base valor por ocupante y noche en la temporada base; en temporada alta se cobra un 30 % más
     */
    record Ejemplo(String codigo, String nombre, String descripcion, int capacidad, int dormitorios, long base,
                   List<String> dotacion) {
    }

    static final List<Ejemplo> EJEMPLOS = List.of(
            new Ejemplo(PRIMERO, "Estudio Cafetal", "Estudio para dos, con ventanal hacia los cafetales y "
                    + "desayunador.", 2, 1, 120_000, List.of("Wifi", "Cocina", "Ventanal", "Agua caliente")),
            new Ejemplo("APT-102", "Suite Guadua", "Dos habitaciones con techos en guadua y balcón con hamaca, a "
                    + "pocos pasos del parque principal.", 4, 2, 100_000,
                    List.of("Wifi", "Cocina", "Balcón", "Hamaca", "Agua caliente", "Televisor")),
            new Ejemplo("APT-103", "Casa Palma de Cera", "La más amplia: tres habitaciones, patio con zona de "
                    + "asados y parqueadero cubierto para un carro.", 6, 3, 90_000,
                    List.of("Wifi", "Cocina", "Patio", "Zona de asados", "Lavadora", "Parqueadero", "Agua caliente",
                            "Televisor")),
            new Ejemplo("APT-104", "Mirador del Valle", null, 3, 2, 110_000,
                    List.of("Wifi", "Vista al valle", "Agua caliente")));

    private final ApartamentoRepository apartamentos;
    private final AlojamientoRepository alojamientos;
    private final CrearApartamentoUseCase crearApartamento;
    private final ConsultarCalendarioUseCase consultarCalendario;
    private final DefinirTarifaUseCase definirTarifa;
    private final AgregarTemporadaUseCase agregarTemporada;
    private final ActivarApartamentoUseCase activarApartamento;
    private final TransactionTemplate transaccion;
    private final SgaPropiedades propiedades;
    private final Clock reloj;

    public CargadorDatosDemo(ApartamentoRepository apartamentos, AlojamientoRepository alojamientos,
                             CrearApartamentoUseCase crearApartamento, ConsultarCalendarioUseCase consultarCalendario,
                             DefinirTarifaUseCase definirTarifa, AgregarTemporadaUseCase agregarTemporada,
                             ActivarApartamentoUseCase activarApartamento, TransactionTemplate transaccion,
                             SgaPropiedades propiedades, Clock reloj) {
        this.apartamentos = apartamentos;
        this.alojamientos = alojamientos;
        this.crearApartamento = crearApartamento;
        this.consultarCalendario = consultarCalendario;
        this.definirTarifa = definirTarifa;
        this.agregarTemporada = agregarTemporada;
        this.activarApartamento = activarApartamento;
        this.transaccion = transaccion;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        String alojamientoId = propiedades.alojamiento().id();
        if (alojamientos.buscarPorId(new AlojamientoId(alojamientoId)).isEmpty()) {
            LOG.warn("Datos demo: no existe el alojamiento {}; no se cargó nada", alojamientoId);
            return;
        }
        if (apartamentos.buscarPorCodigo(new ApartamentoId(PRIMERO)).isPresent()) {
            LOG.info("Datos demo: ya existe {}; no se cargó nada", PRIMERO);
            return;
        }
        try {
            // Una sola transacción: o quedan todos los datos o ninguno
            transaccion.executeWithoutResult(estado -> cargar(alojamientoId));
            LOG.info("Datos demo: {} apartamentos activos con sus temporadas y tarifas", EJEMPLOS.size());
        } catch (RuntimeException error) {
            LOG.warn("Datos demo: no se cargaron ({}). El backend sigue sin ellos", error.getMessage());
        }
    }

    // Mismo orden que seguiría el administrador: crear, tarifa base, temporadas con sus tarifas y activar (APA-11)
    private void cargar(String alojamientoId) {
        EJEMPLOS.forEach(e -> crearApartamento.ejecutar(new CrearApartamentoCommand(e.codigo(), alojamientoId,
                e.nombre(), e.descripcion(), e.capacidad(), e.dormitorios(),
                List.of(new ImagenCommand(FOTOS + e.codigo() + "-a/1200/675", true),
                        new ImagenCommand(FOTOS + e.codigo() + "-b/1200/675", false)),
                e.dotacion())));

        String base = consultarCalendario.ejecutar(alojamientoId).temporadas().stream()
                .filter(TemporadaResult::esBase)
                .map(TemporadaResult::id)
                .findFirst()
                .orElseThrow();
        EJEMPLOS.forEach(e -> definirTarifa.ejecutar(new DefinirTarifaCommand(e.codigo(), base,
                BigDecimal.valueOf(e.base()))));

        // TEM-04: el mínimo de temporadas específicas para activar es 2 (sga.parametros.minimo-temporadas)
        LocalDate hoy = LocalDate.now(reloj);
        agregarTemporadaAlta(alojamientoId, "Temporada alta de fin de año", proxima(hoy, MonthDay.of(12, 15)), 31);
        agregarTemporadaAlta(alojamientoId, "Vacaciones de mitad de año", proxima(hoy, MonthDay.of(6, 15)), 30);

        EJEMPLOS.forEach(e -> activarApartamento.ejecutar(e.codigo()));
    }

    /** @param noches de la temporada; su último día es {@code inicio + noches} (TEM-01, fin inclusivo) */
    private void agregarTemporadaAlta(String alojamientoId, String nombre, LocalDate inicio, int noches) {
        Function<Ejemplo, TarifaCommand> tarifaAlta = e -> new TarifaCommand(e.codigo(),
                BigDecimal.valueOf(e.base() * 13 / 10));
        agregarTemporada.ejecutar(new AgregarTemporadaCommand(alojamientoId, nombre, inicio, inicio.plusDays(noches),
                0, EJEMPLOS.stream().map(tarifaAlta).toList()));
    }

    /** La próxima vez que llega ese día del año, para que las temporadas sirvan sin importar cuándo se arranque. */
    static LocalDate proxima(LocalDate hoy, MonthDay dia) {
        LocalDate esteAnio = dia.atYear(hoy.getYear());
        return esteAnio.isBefore(hoy) ? esteAnio.plusYears(1) : esteAnio;
    }
}
