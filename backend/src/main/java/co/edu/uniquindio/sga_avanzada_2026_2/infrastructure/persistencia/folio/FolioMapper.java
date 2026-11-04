package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.CargoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Pago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.PagoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;

/**
 * Convierte Folio ⇄ FolioJpa. Al cargar usa el constructor de reconstrucción del dominio (DEC-06).
 */
final class FolioMapper {

    private FolioMapper() {
    }

    static Folio aDominio(FolioJpa jpa) {
        return new Folio(
                new FolioId(jpa.getId()),
                new ReservaId(jpa.getReservaCodigo()),
                jpa.getCargos().stream()
                        .map(c -> new Cargo(new CargoId(c.getId()), c.getTipo(), c.getConcepto(),
                                new Dinero(c.getValor()), c.getSentido(), c.getFecha(),
                                c.getCorrigeA() == null ? null : new CargoId(c.getCorrigeA())))
                        .toList(),
                jpa.getPagos().stream()
                        .map(p -> new Pago(new PagoId(p.getId()), new MedioPago(p.getMedio()), new Dinero(p.getMonto()),
                                p.getTipo(), p.getFecha(), p.getReversaDe() == null ? null : new PagoId(p.getReversaDe())))
                        .toList(),
                jpa.isCerrado(),
                jpa.getAutorizacionAutor() == null ? null : new AutorizacionCierre(
                        new UsuarioId(jpa.getAutorizacionAutor()), jpa.getAutorizacionMotivo(),
                        jpa.getAutorizacionFechaHora()));
    }

    /**
     * Copia el estado del dominio sobre una fila nueva o ya cargada; no toca {@code version}, así el bloqueo
     * optimista sigue funcionando.
     */
    static void copiar(Folio folio, FolioJpa jpa) {
        jpa.setId(folio.id().valor());
        jpa.setReservaCodigo(folio.reservaId().valor());
        jpa.setCerrado(folio.cerrado());
        AutorizacionCierre autorizacion = folio.autorizacion();
        jpa.setAutorizacionAutor(autorizacion == null ? null : autorizacion.autor().valor());
        jpa.setAutorizacionMotivo(autorizacion == null ? null : autorizacion.motivo());
        jpa.setAutorizacionFechaHora(autorizacion == null ? null : autorizacion.fechaHora());
        jpa.getCargos().clear();
        folio.cargos().forEach(c -> jpa.getCargos().add(new CargoJpa(c.id().valor(), c.tipo(), c.concepto(),
                c.valor().monto(), c.sentido(), c.fecha(), c.corrigeA() == null ? null : c.corrigeA().valor())));
        jpa.getPagos().clear();
        folio.pagos().forEach(p -> jpa.getPagos().add(new PagoJpa(p.id().valor(), p.medio().nombre(),
                p.monto().monto(), p.tipo(), p.fecha(), p.reversaDe() == null ? null : p.reversaDe().valor())));
    }
}
