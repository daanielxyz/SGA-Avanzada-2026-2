package co.edu.uniquindio.sga_avanzada_2026_2.application.canal;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.AvisosReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.CargadorReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.OcupanteCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.reserva.TitularParaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalOrigen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.ConflictoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.EventoCanalRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.OperacionCanal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.SentidoEvento;
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
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.Disponibilidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.DisponibilidadDomainService;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.IntegracionExterna;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.SincronizacionCanalDomainService;
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
import java.util.Optional;

/**
 * Caso de uso: integrar la reserva que envía un canal externo (CU-12). Lo invoca el adaptador del canal (B4), no un
 * usuario. Según lo que decida {@link SincronizacionCanalDomainService}, crea la reserva con su folio y su titular
 * (DEC-44), devuelve la que ya existía (RN-19) o registra el conflicto (RN-18); en los tres casos lo anota en la
 * bitácora, todo en la misma transacción (BIT-03 · DEC-62).
 */
@Service
public class RecibirReservaExterna {

    private final CargadorReserva cargador;
    private final CanalRepository canales;
    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final TitularRepository titulares;
    private final TarifaRepository tarifas;
    private final PoliticaCancelacionRepository politicas;
    private final ConflictoCanalRepository conflictos;
    private final EventoCanalRepository eventos;
    private final DisponibilidadDomainService disponibilidad;
    private final TarificacionDomainService tarificacion;
    private final SincronizacionCanalDomainService sincronizacion;
    private final GeneradorCodigos codigos;
    private final AvisosReserva avisos;
    private final Clock reloj;

    public RecibirReservaExterna(CargadorReserva cargador, CanalRepository canales, ReservaRepository reservas,
                                 FolioRepository folios, TitularRepository titulares, TarifaRepository tarifas,
                                 PoliticaCancelacionRepository politicas, ConflictoCanalRepository conflictos,
                                 EventoCanalRepository eventos, DisponibilidadDomainService disponibilidad,
                                 TarificacionDomainService tarificacion,
                                 SincronizacionCanalDomainService sincronizacion, GeneradorCodigos codigos,
                                 AvisosReserva avisos, Clock reloj) {
        this.cargador = cargador;
        this.canales = canales;
        this.reservas = reservas;
        this.folios = folios;
        this.titulares = titulares;
        this.tarifas = tarifas;
        this.politicas = politicas;
        this.conflictos = conflictos;
        this.eventos = eventos;
        this.disponibilidad = disponibilidad;
        this.tarificacion = tarificacion;
        this.sincronizacion = sincronizacion;
        this.codigos = codigos;
        this.avisos = avisos;
        this.reloj = reloj;
    }

    /**
     * La falta de disponibilidad no es un error: devuelve CONFLICTO y lo deja registrado. Un rechazo por otra regla
     * sí lanza la excepción y deshace todo; el adaptador lo anota como RECHAZADO con {@link RegistrarEventoCanal}
     * (DEC-62).
     *
     * @throws RecursoNoEncontradoException si no existe el canal, el apartamento en el alojamiento del canal, el
     *                                      calendario o la política vigente
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si el canal no es
     *                                      externo o está inactivo (CAN-05), falta una tarifa (RN-05) o la reserva
     *                                      viola una regla de creación que no es de disponibilidad (RN-03 · RN-04 ·
     *                                      OCU-02 · OCU-05 · OCU-12 · RP-01 · TIT-01)
     */
    @Transactional
    public IntegracionExternaResult ejecutar(RecibirReservaExternaCommand comando) {
        CanalId canalId = new CanalId(comando.canalId());
        Canal canal = canales.buscarPorId(canalId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el canal", canalId.valor()));
        Apartamento apartamento = apartamentoDelCanal(canal, comando.apartamento());
        ParametrosAlojamiento parametros = cargador.alojamientoDe(apartamento).parametros();
        CalendarioTemporadas calendario = cargador.calendario(canal.alojamientoId());
        PoliticaCancelacion politica = politicas.buscarVigente(canal.alojamientoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("la política vigente del alojamiento",
                        canal.alojamientoId().valor()));
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Estancia estancia = new Estancia(comando.entrada(), comando.salida());
        List<Ocupante> ocupantes = OcupanteCommand.aDominio(comando.ocupantes());
        TitularParaReserva paraReserva = cargador.titularPara(canal.alojamientoId(), comando.titular());
        Titular titular = paraReserva.titular();
        Disponibilidad disponible = disponibilidad.verificarDisponibilidad(apartamento, estancia, ocupantes.size(),
                reservas.buscarActivasPorApartamento(apartamento.codigo()), parametros);

        IntegracionExterna integracion = sincronizacion.integrarReservaExterna(canal, comando.idExterno(),
                reservas.buscarPorCanalEIdExterno(canal.id(), comando.idExterno()), disponible, () -> {
                    Cotizacion cotizacion = tarificacion.calcularValorEstancia(apartamento.codigo(), estancia,
                            ocupantes, parametros.umbralEdadFacturable(), calendario,
                            tarifas.buscarVigentesPorApartamento(apartamento.codigo(), ahora.toLocalDate()));
                    return Reserva.crear(new ReservaId(codigos.siguiente(SerieCodigo.RESERVA)), apartamento.codigo(),
                            titular, estancia, CanalOrigen.EXTERNO, canal.id(), comando.idExterno(), ocupantes, null,
                            cotizacion, politica.id(), apartamento.capacidad(),
                            calendario.estanciaMinimaPara(estancia), parametros.umbralEdadFacturable(), ahora);
                }, new ConflictoId(codigos.siguiente(SerieCodigo.CONFLICTO)), ahora);

        integracion.reservaCreada().ifPresent(reserva -> {
            titulares.guardar(titular);
            reservas.guardar(reserva);
            folios.guardar(Folio.abrir(new FolioId(codigos.siguiente(SerieCodigo.FOLIO)), reserva.codigo(),
                    reserva.valorTotal(), ahora.toLocalDate()));
            avisos.avisarCreacion(reserva, paraReserva, cargador.alojamientoDe(apartamento).nombre());
        });
        integracion.conflictoRegistrado().ifPresent(conflictos::guardar);
        eventos.registrar(new EventoCanal(new EventoCanalId(codigos.siguiente(SerieCodigo.EVENTO)), canal.id(),
                OperacionCanal.CREAR_RESERVA, SentidoEvento.ENTRANTE, ahora, comando.cargaUtil(),
                integracion.resultado(), integracion.detalle()));
        return IntegracionExternaResult.de(integracion);
    }

    /** Un canal solo vende los apartamentos de su alojamiento: el de otro no existe para él (DEC-26). */
    private Apartamento apartamentoDelCanal(Canal canal, String codigo) {
        return Optional.of(cargador.apartamento(codigo))
                .filter(apartamento -> apartamento.alojamientoId().equals(canal.alojamientoId()))
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", codigo));
    }
}
