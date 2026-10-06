package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Estancia;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Noche;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Registro;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.RegistroId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Salida;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.SalidaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.LineaCotizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tarifa.TemporadaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;

/**
 * Convierte Reserva ⇄ ReservaJpa. Al cargar usa el constructor de reconstrucción del dominio, que vuelve a validar
 * las invariantes pero no las reglas de creación (DEC-06): una reserva pasada se carga sin chocar con RN-04.
 */
final class ReservaMapper {

    private ReservaMapper() {
    }

    static Reserva aDominio(ReservaJpa jpa) {
        return new Reserva(
                new ReservaId(jpa.getCodigo()),
                new ApartamentoId(jpa.getApartamentoCodigo()),
                new TitularId(jpa.getTitularId()),
                new Estancia(jpa.getEntrada(), jpa.getSalida()),
                jpa.getEstado(),
                jpa.getCanalOrigen(),
                jpa.getCanalId() == null ? null : new CanalId(jpa.getCanalId()),
                jpa.getIdExterno(),
                jpa.getOcupantes().stream().map(ReservaMapper::ocupante).toList(),
                jpa.getRegistroId() == null ? null : new Registro(new RegistroId(jpa.getRegistroId()),
                        jpa.getRegistroFechaHora(), new UsuarioId(jpa.getRegistroAutor()), jpa.getRegistroAnulado()),
                jpa.getSalidaId() == null ? null : new Salida(new SalidaId(jpa.getSalidaId()),
                        jpa.getSalidaFechaHora(), new UsuarioId(jpa.getSalidaAutor())),
                jpa.getHoraEstimadaLlegada(),
                new Dinero(jpa.getValorTotal()),
                jpa.getDesglose().stream()
                        .map(n -> new LineaCotizacion(new Noche(n.getNoche()), new TemporadaId(n.getTemporadaId()),
                                new Dinero(n.getTarifa()), n.getOcupantesFacturables(), new Dinero(n.getSubtotal())))
                        .toList(),
                new PoliticaId(jpa.getPoliticaVersionId()),
                jpa.getCreadaEn());
    }

    /**
     * Copia el estado del dominio sobre una fila nueva o ya cargada; no toca {@code version}, así el bloqueo
     * optimista sigue funcionando.
     */
    static void copiar(Reserva reserva, ReservaJpa jpa) {
        jpa.setCodigo(reserva.codigo().valor());
        jpa.setApartamentoCodigo(reserva.apartamentoId().valor());
        jpa.setTitularId(reserva.titularId().valor());
        jpa.setEntrada(reserva.estancia().entrada());
        jpa.setSalida(reserva.estancia().salida());
        jpa.setEstado(reserva.estado());
        jpa.setCanalOrigen(reserva.canalOrigen());
        jpa.setCanalId(reserva.canalId() == null ? null : reserva.canalId().valor());
        jpa.setIdExterno(reserva.idExterno());
        jpa.setHoraEstimadaLlegada(reserva.horaEstimadaLlegada());
        jpa.setValorTotal(reserva.valorTotal().monto());
        jpa.setPoliticaVersionId(reserva.politicaVersionId().valor());
        jpa.setCreadaEn(reserva.creadaEn());
        Registro registro = reserva.registro();
        jpa.setRegistroId(registro == null ? null : registro.id().valor());
        jpa.setRegistroFechaHora(registro == null ? null : registro.fechaHora());
        jpa.setRegistroAutor(registro == null ? null : registro.autor().valor());
        jpa.setRegistroAnulado(registro == null ? null : registro.anulado());
        Salida salida = reserva.salida();
        jpa.setSalidaId(salida == null ? null : salida.id().valor());
        jpa.setSalidaFechaHora(salida == null ? null : salida.fechaHora());
        jpa.setSalidaAutor(salida == null ? null : salida.autor().valor());
        jpa.getOcupantes().clear();
        reserva.ocupantes().forEach(o -> jpa.getOcupantes().add(new OcupanteJpa(o.id().valor(), o.nombre(),
                o.fechaNacimiento(), o.documento() == null ? null : o.documento().tipo(),
                o.documento() == null ? null : o.documento().numero(), o.nacionalidad())));
        jpa.getDesglose().clear();
        reserva.desglose().forEach(l -> jpa.getDesglose().add(new NocheReservaJpa(l.noche().fecha(),
                l.temporadaId().valor(), l.tarifa().monto(), l.ocupantesFacturables(), l.subtotal().monto())));
    }

    private static Ocupante ocupante(OcupanteJpa jpa) {
        Documento documento = jpa.getTipoDocumento() == null ? null
                : new Documento(jpa.getTipoDocumento(), jpa.getNumeroDocumento());
        return new Ocupante(new OcupanteId(jpa.getId()), jpa.getNombre(), jpa.getFechaNacimiento(), documento,
                jpa.getNacionalidad());
    }
}
