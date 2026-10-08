package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacionRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.TarificacionDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.Cotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TarifaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Caso de uso: crear una reserva por el portal (CU-10) o en recepción (CU-11), según el flujo de MODELO §3 (DEC-36 ·
 * DEC-37). Guarda en una transacción la reserva, su folio abierto con el cargo del alojamiento (FOL-01 · DEC-44) y el
 * titular nuevo o actualizado (DEC-58 · DEC-59).
 */
@Service
public class CrearReserva {

    private final CargadorReserva cargador;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final TitularRepository titulares;
    private final TarifaRepository tarifas;
    private final PoliticaCancelacionRepository politicas;
    private final DisponibilidadDomainService disponibilidad;
    private final TarificacionDomainService tarificacion;
    private final GeneradorCodigos codigos;
    private final Clock reloj;

    public CrearReserva(CargadorReserva cargador, ReservaRepository reservas, FolioRepository folios,
                        TitularRepository titulares, TarifaRepository tarifas, PoliticaCancelacionRepository politicas,
                        DisponibilidadDomainService disponibilidad, TarificacionDomainService tarificacion,
                        GeneradorCodigos codigos, Clock reloj) {
        this.cargador = cargador;
        this.reservas = reservas;
        this.folios = folios;
        this.titulares = titulares;
        this.tarifas = tarifas;
        this.politicas = politicas;
        this.disponibilidad = disponibilidad;
        this.tarificacion = tarificacion;
        this.codigos = codigos;
        this.reloj = reloj;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el apartamento, su alojamiento, el calendario o la política
     *                                      vigente
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el apartamento no
     *         está disponible (RN-01 · RN-07 · RN-20 · DISP-02), falta una tarifa (RN-05) o la reserva viola una
     *         regla de creación (RN-02 · RN-03 · RN-04 · OCU-02 · OCU-05 · OCU-12 · RP-01 · TIT-01 · CORI-03)
     */
    @Transactional
    public ReservaResult ejecutar(CrearReservaCommand comando) {
        Apartamento apartamento = cargador.apartamento(comando.apartamento());
        Alojamiento alojamiento = cargador.alojamientoDe(apartamento);
        ParametrosAlojamiento parametros = alojamiento.parametros();
        CalendarioTemporadas calendario = cargador.calendario(alojamiento.id());
        PoliticaCancelacion politica = politicas.buscarVigente(alojamiento.id())
                .orElseThrow(() -> new RecursoNoEncontradoException("la política vigente del alojamiento",
                        alojamiento.id().valor()));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());
        List<Ocupante> ocupantes = OcupanteCommand.aDominio(comando.ocupantes());
        Titular titular = cargador.titularPara(alojamiento.id(), comando.titular());

        disponibilidad.verificarDisponibilidad(apartamento, estancia, ocupantes.size(),
                reservas.buscarActivasPorApartamento(apartamento.codigo()), parametros).exigir();
        Cotizacion cotizacion = tarificacion.calcularValorEstancia(apartamento.codigo(), estancia, ocupantes,
                parametros.umbralEdadFacturable(), calendario,
                tarifas.buscarVigentesPorApartamento(apartamento.codigo(), ahora.toLocalDate()));
        Reserva reserva = Reserva.crear(new ReservaId(codigos.siguiente(SerieCodigo.RESERVA)), apartamento.codigo(),
                titular, estancia, CanalOrigen.valueOf(comando.canalOrigen()), null, null, ocupantes,
                comando.horaEstimadaLlegada(), cotizacion, politica.id(), apartamento.capacidad(),
                calendario.estanciaMinimaPara(estancia), parametros.umbralEdadFacturable(), ahora);
        Folio folio = Folio.abrir(new FolioId(codigos.siguiente(SerieCodigo.FOLIO)), reserva.codigo(),
                reserva.valorTotal(), ahora.toLocalDate());

        titulares.guardar(titular);
        reservas.guardar(reserva);
        folios.guardar(folio);
        return ReservaResult.de(reserva);
    }
}
