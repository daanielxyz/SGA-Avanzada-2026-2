package co.edu.uniquindio.sga_avanzada_2026_2.application.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadas;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.CalendarioTemporadasRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Carga por los puertos lo que necesitan los casos de uso de Reserva y traduce un faltante en
 * {@link RecursoNoEncontradoException} (404, DEC-35). No decide nada de negocio.
 */
@Component
class CargadorReserva {

    private final ReservaRepository reservas;
    private final FolioRepository folios;
    private final ApartamentoRepository apartamentos;
    private final AlojamientoRepository alojamientos;
    private final CalendarioTemporadasRepository calendarios;
    private final TitularRepository titulares;
    private final GeneradorCodigos codigos;

    CargadorReserva(ReservaRepository reservas, FolioRepository folios, ApartamentoRepository apartamentos,
                    AlojamientoRepository alojamientos, CalendarioTemporadasRepository calendarios,
                    TitularRepository titulares, GeneradorCodigos codigos) {
        this.reservas = reservas;
        this.folios = folios;
        this.apartamentos = apartamentos;
        this.alojamientos = alojamientos;
        this.calendarios = calendarios;
        this.titulares = titulares;
        this.codigos = codigos;
    }

    Reserva reserva(String codigo) {
        ReservaId id = new ReservaId(codigo);
        return reservas.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("la reserva", id.valor()));
    }

    /** El folio de la reserva; existe siempre desde que se creó (FOL-01 · FOL-02). */
    Folio folioDe(Reserva reserva) {
        return folios.buscarPorReserva(reserva.codigo())
                .orElseThrow(() -> new RecursoNoEncontradoException("el folio de la reserva",
                        reserva.codigo().valor()));
    }

    Apartamento apartamento(String codigo) {
        return apartamento(new ApartamentoId(codigo));
    }

    Apartamento apartamento(ApartamentoId id) {
        return apartamentos.buscarPorCodigo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el apartamento", id.valor()));
    }

    /** El alojamiento llega a través del apartamento: la reserva no repite su id (DEC-26). */
    Alojamiento alojamientoDe(Apartamento apartamento) {
        return alojamiento(apartamento.alojamientoId());
    }

    Alojamiento alojamiento(AlojamientoId id) {
        return alojamientos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", id.valor()));
    }

    CalendarioTemporadas calendario(AlojamientoId id) {
        return calendarios.buscarPorAlojamiento(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el calendario de temporadas del alojamiento",
                        id.valor()));
    }

    Titular titular(TitularId id) {
        return titulares.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("el titular", id.valor()));
    }

    /**
     * El titular con ese documento en el alojamiento, con sus datos actualizados a lo que llegó; o uno nuevo si es
     * la primera vez (TIT-05 · DEC-58). No se guarda aquí: lo guarda el caso de uso junto con la reserva.
     */
    Titular titularPara(AlojamientoId alojamientoId, TitularCommand datos) {
        Documento documento = new Documento(TipoDocumento.valueOf(datos.tipoDocumento()), datos.numeroDocumento());
        Correo correo = Optional.ofNullable(datos.correo()).filter(c -> !c.isBlank()).map(Correo::new).orElse(null);
        return titulares.buscarPorDocumento(alojamientoId, documento)
                .map(existente -> {
                    existente.actualizarDatos(datos.nombre(), correo, datos.telefono());
                    return existente;
                })
                .orElseGet(() -> new Titular(new TitularId(codigos.siguiente(SerieCodigo.TITULAR)), alojamientoId,
                        datos.nombre(), documento, correo, datos.telefono()));
    }
}
