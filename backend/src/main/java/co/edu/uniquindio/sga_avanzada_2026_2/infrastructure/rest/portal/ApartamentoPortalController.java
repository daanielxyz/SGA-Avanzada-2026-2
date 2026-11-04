package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ApartamentoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento.ConsultarApartamentoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ApartamentoDisponibleResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarDisponiblesUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarProximasNochesLibresCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.BuscarProximasNochesLibresUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizacionResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizarEstanciaUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CotizarPorCantidadCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.ProximaNocheLibreResult;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.AlojamientoActual;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.PaginaResponse;
import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.portal.ApartamentoPublicoResponse.PrecioResponse;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que el visitante del portal ve de los apartamentos, sin sesión (CU-07 · CU-08 · CU-09): todos con el precio de
 * su próxima noche libre, los libres para unas fechas y la ficha de cada uno, solo con los datos públicos
 * ({@link ApartamentoPublicoResponse}). Reusa los casos de uso del personal sin cambiarlos. Solo traduce HTTP ⇄ caso
 * de uso (DEC-33).
 */
@RestController
@RequestMapping("/api/portal/apartamentos")
public class ApartamentoPortalController {

    // Hasta dónde mira el portal para encontrar la próxima noche libre: un año
    private static final int HORIZONTE_DIAS = 365;

    private final AlojamientoActual alojamiento;
    private final BuscarDisponiblesUseCase buscarDisponibles;
    private final BuscarProximasNochesLibresUseCase proximasNoches;
    private final ConsultarApartamentoUseCase consultarApartamento;
    private final CotizarEstanciaUseCase cotizarEstancia;
    private final Clock reloj;

    public ApartamentoPortalController(AlojamientoActual alojamiento, BuscarDisponiblesUseCase buscarDisponibles,
                                       BuscarProximasNochesLibresUseCase proximasNoches,
                                       ConsultarApartamentoUseCase consultarApartamento,
                                       CotizarEstanciaUseCase cotizarEstancia, Clock reloj) {
        this.alojamiento = alojamiento;
        this.buscarDisponibles = buscarDisponibles;
        this.proximasNoches = proximasNoches;
        this.consultarApartamento = consultarApartamento;
        this.cotizarEstancia = cotizarEstancia;
        this.reloj = reloj;
    }

    /**
     * Los apartamentos a la venta, de 10 en 10 (DEC-18), cada uno con lo que paga un adulto su próxima noche libre
     * desde hoy, para mostrar precios antes de que el visitante elija fechas. Solo se calcula el precio de los de la
     * página pedida.
     *
     * @param pagina empieza en 0; una que no existe llega vacía
     */
    @GetMapping
    public PaginaResponse<ApartamentoPublicoResponse> listar(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(pagina(proximasNoches(LocalDate.now(reloj)), pagina), this::conSuProximaNoche);
    }

    /**
     * Cada libre trae su precio para el grupo, sin pedir fechas de nacimiento: el visitante dice cuántos son niños y
     * los demás pagan (RN-06). Los datos de cada ocupante se piden al reservar.
     *
     * @param salida    exclusiva: esa noche no se ocupa (EST-01)
     * @param ocupantes tamaño del grupo; todos cuentan para la capacidad (RN-02)
     * @param ninos     cuántos del grupo no alcanzan el umbral de edad a la entrada; sin él, todos pagan. Si son más
     *                  que el grupo, el dominio lo rechaza (409)
     * @param pagina    de 10 en 10 (DEC-18), empieza en 0
     */
    @GetMapping("/disponibles")
    public PaginaResponse<ApartamentoPublicoResponse> buscar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam @Min(value = 1, message = "debe ser al menos 1") int ocupantes,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativo") int ninos,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativa") int pagina) {
        return PaginaResponse.de(pagina(libres(entrada, salida, ocupantes), pagina),
                codigo -> ApartamentoPublicoResponse.de(consultarApartamento.ejecutar(codigo),
                        cotizar(codigo, entrada, salida, ocupantes - ninos), null));
    }

    /** La ficha, con su próxima noche libre como en el listado. APA-16: uno retirado no existe para el público. */
    @GetMapping("/{codigo}")
    public ApartamentoPublicoResponse consultar(@PathVariable String codigo) {
        aLaVenta(codigo);
        return conSuProximaNoche(proximaNoche(codigo, LocalDate.now(reloj)));
    }

    /**
     * Si unas fechas y un grupo caben en este apartamento, cuánto costaría y, si no está libre, desde qué noche lo
     * está. Los mismos parámetros que {@code /disponibles}.
     */
    @GetMapping("/{codigo}/estancia")
    public EstanciaPortalResponse estancia(
            @PathVariable String codigo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
            @RequestParam @Min(value = 1, message = "debe ser al menos 1") int ocupantes,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "no puede ser negativo") int ninos) {
        aLaVenta(codigo);
        return new EstanciaPortalResponse(libres(entrada, salida, ocupantes).contains(codigo),
                PrecioResponse.de(cotizar(codigo, entrada, salida, ocupantes - ninos)),
                proximaNoche(codigo, entrada).noche());
    }

    // El listado se arma en memoria (los libres, o la próxima noche de cada uno): se corta aquí la página pedida
    private static <T> Pagina<T> pagina(List<T> todos, int numero) {
        int desde = (int) Math.min((long) numero * Pagina.TAMANO, todos.size());
        return new Pagina<>(todos.subList(desde, Math.min(desde + Pagina.TAMANO, todos.size())), numero, Pagina.TAMANO,
                todos.size());
    }

    private void aLaVenta(String codigo) {
        ApartamentoResult apartamento = consultarApartamento.ejecutar(codigo);
        if (!apartamento.activo()) {
            throw new RecursoNoEncontradoException("el apartamento", codigo);
        }
    }

    private List<String> libres(LocalDate entrada, LocalDate salida, int ocupantes) {
        return buscarDisponibles.ejecutar(new BuscarDisponiblesCommand(alojamiento.id(), entrada, salida, ocupantes))
                .stream().map(ApartamentoDisponibleResult::codigo).toList();
    }

    private List<ProximaNocheLibreResult> proximasNoches(LocalDate desde) {
        return proximasNoches.ejecutar(new BuscarProximasNochesLibresCommand(alojamiento.id(), desde, HORIZONTE_DIAS));
    }

    private ProximaNocheLibreResult proximaNoche(String codigo, LocalDate desde) {
        return proximasNoches(desde).stream().filter(l -> l.codigo().equals(codigo)).findFirst()
                .orElse(new ProximaNocheLibreResult(codigo, null));
    }

    // Un adulto, su próxima noche libre; sin noche libre en el año, sin precio
    private ApartamentoPublicoResponse conSuProximaNoche(ProximaNocheLibreResult libre) {
        LocalDate noche = libre.noche();
        return ApartamentoPublicoResponse.de(consultarApartamento.ejecutar(libre.codigo()),
                noche == null ? null : cotizar(libre.codigo(), noche, noche.plusDays(1), 1), noche);
    }

    private CotizacionResult cotizar(String codigo, LocalDate entrada, LocalDate salida, int facturables) {
        return cotizarEstancia.ejecutar(new CotizarPorCantidadCommand(codigo, entrada, salida, facturables));
    }
}
