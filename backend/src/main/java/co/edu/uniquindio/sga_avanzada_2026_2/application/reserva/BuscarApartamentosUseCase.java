package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Caso de uso: buscar apartamentos libres por fechas, composición del grupo y filtros, con su valor estimado, de 10 en
 * 10 (CU-07 · DEC-18). La disponibilidad la decide el dominio (RN-01 · RN-02 · RN-07 · RN-20), igual que al reservar.
 */
@Service
public class BuscarApartamentosUseCase {

    private final CargadorReserva cargador;
    private final ApartamentoRepository apartamentos;
    private final ReservaRepository reservas;
    private final TarifaRepository tarifas;
    private final DisponibilidadDomainService disponibilidad;
    private final TarificacionDomainService tarificacion;
    private final Clock reloj;

    public BuscarApartamentosUseCase(CargadorReserva cargador, ApartamentoRepository apartamentos,
                                     ReservaRepository reservas, TarifaRepository tarifas,
                                     DisponibilidadDomainService disponibilidad,
                                     TarificacionDomainService tarificacion, Clock reloj) {
        this.cargador = cargador;
        this.apartamentos = apartamentos;
        this.reservas = reservas;
        this.tarifas = tarifas;
        this.disponibilidad = disponibilidad;
        this.tarificacion = tarificacion;
        this.reloj = reloj;
    }

    /**
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException si el alojamiento o su calendario no existen
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si la estancia es inválida (RN-03) o las fechas de
     *         nacimiento no corresponden al grupo
     */
    @Transactional(readOnly = true)
    public Pagina<ApartamentoEncontradoResult> ejecutar(BuscarApartamentosCommand comando) {
        Alojamiento alojamiento = cargador.alojamiento(new AlojamientoId(comando.alojamientoId()));
        CalendarioTemporadas calendario = cargador.calendario(alojamiento.id());
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());
        LocalDate hoy = LocalDate.now(reloj);
        int facturables = tarificacion.contarFacturables(comando.ocupantes(), comando.fechasNacimiento(),
                comando.entrada(), alojamiento.parametros().umbralEdadFacturable());

        return Pagina.de(apartamentos.buscarActivos(alojamiento.id()).stream()
                .filter(a -> comando.filtro().admite(a, alojamiento.serviciosAdicionales()))
                .filter(a -> disponibilidad.verificarDisponibilidad(a, estancia, comando.ocupantes(),
                        reservas.buscarActivasPorApartamento(a.codigo()), alojamiento.parametros()).disponible())
                .map(a -> tarjeta(a, alojamiento, tarificacion.calcularValorEstancia(a.codigo(), estancia, facturables,
                        calendario, tarifas.buscarVigentesPorApartamento(a.codigo(), hoy)).total().monto()))
                .filter(t -> comando.filtro().admitePrecio(t.valorEstimado()))
                .toList(), comando.pagina());
    }

    private static ApartamentoEncontradoResult tarjeta(Apartamento a, Alojamiento alojamiento,
                                                       java.math.BigDecimal valor) {
        return new ApartamentoEncontradoResult(a.codigo().valor(), a.nombre(), a.capacidad().valor(),
                a.dormitorios().cantidad(),
                a.imagenes().stream().filter(Imagen::principal).map(Imagen::url).findFirst().orElse(null),
                a.caracteristicas().stream().map(Caracteristica::nombre).toList(), valor, alojamiento.ciudad(),
                alojamiento.ubicacion().latitud(), alojamiento.ubicacion().longitud());
    }
}
