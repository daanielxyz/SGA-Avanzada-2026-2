package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.DirectorioPersonal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.AutorizacionCierre;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Pago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Salida de los casos de uso del Folio (DEC-31): movimientos, total pagado y saldo calculado (RN-15).
 *
 * @param situacionSaldo PENDIENTE (debe el huésped), A_FAVOR (hay que devolver) o AL_DIA
 * @param autorizacion   solo si se cerró con saldo distinto de cero (RN-17)
 */
public record FolioResult(String id, String reserva, List<CargoResult> cargos, List<PagoResult> pagos,
                          BigDecimal totalPagado, BigDecimal saldo, String situacionSaldo, boolean cerrado,
                          AutorizacionResult autorizacion) {

    /**
     * @param sentido  solo en un AJUSTE: AUMENTA o DISMINUYE
     * @param corrigeA el cargo que este AJUSTE revierte, si corrige alguno
     * @param autor    quien hizo la corrección; vacío en los demás cargos (DEC-100)
     * @param autorNombre su nombre vigente, solo con {@link FolioResult#conNombres} (DEC-103)
     */
    public record CargoResult(String id, String tipo, String concepto, BigDecimal valor, String sentido,
                              LocalDate fecha, String corrigeA, String autor, String autorNombre) {

        static CargoResult de(Cargo cargo) {
            return new CargoResult(cargo.id().valor(), cargo.tipo().name(), cargo.concepto(), cargo.valor().monto(),
                    Optional.ofNullable(cargo.sentido()).map(Enum::name).orElse(null), cargo.fecha(),
                    Optional.ofNullable(cargo.corrigeA()).map(c -> c.valor()).orElse(null),
                    Optional.ofNullable(cargo.autor()).map(a -> a.valor()).orElse(null), null);
        }

        private CargoResult conAutor(String nuevoAutor, String nombre) {
            return new CargoResult(id, tipo, concepto, valor, sentido, fecha, corrigeA, nuevoAutor, nombre);
        }
    }

    /**
     * @param tipo      ABONO o REVERSO
     * @param reversaDe el abono que este REVERSO corrige; vacío en una devolución
     * @param autor     quien hizo el REVERSO o la devolución; vacío en un abono (DEC-100)
     * @param autorNombre su nombre vigente, solo con {@link FolioResult#conNombres} (DEC-103)
     */
    public record PagoResult(String id, String medio, BigDecimal monto, String tipo, LocalDate fecha,
                             String reversaDe, String autor, String autorNombre) {

        static PagoResult de(Pago pago) {
            return new PagoResult(pago.id().valor(), pago.medio().nombre(), pago.monto().monto(), pago.tipo().name(),
                    pago.fecha(), Optional.ofNullable(pago.reversaDe()).map(p -> p.valor()).orElse(null),
                    Optional.ofNullable(pago.autor()).map(a -> a.valor()).orElse(null), null);
        }

        private PagoResult conAutor(String nuevoAutor, String nombre) {
            return new PagoResult(id, medio, monto, tipo, fecha, reversaDe, nuevoAutor, nombre);
        }
    }

    /** @param autorNombre el nombre vigente de quien autorizó, solo con {@link FolioResult#conNombres} (DEC-103) */
    public record AutorizacionResult(String autor, String motivo, LocalDateTime fechaHora, String autorNombre) {

        static AutorizacionResult de(AutorizacionCierre autorizacion) {
            return new AutorizacionResult(autorizacion.autor().valor(), autorizacion.motivo(),
                    autorizacion.fechaHora(), null);
        }

        private AutorizacionResult conAutor(String nuevoAutor, String nombre) {
            return new AutorizacionResult(nuevoAutor, motivo, fechaHora, nombre);
        }
    }

    public static FolioResult de(Folio folio) {
        Saldo saldo = folio.saldo();
        return new FolioResult(
                folio.id().valor(),
                folio.reservaId().valor(),
                folio.cargos().stream().map(CargoResult::de).toList(),
                folio.pagos().stream().map(PagoResult::de).toList(),
                folio.totalPagado().monto(),
                saldo.monto().monto(),
                saldo.situacion().name(),
                folio.cerrado(),
                Optional.ofNullable(folio.autorizacion()).map(AutorizacionResult::de).orElse(null));
    }

    /** Lo que ve el personal: el nombre de quien corrigió, revirtió, devolvió o autorizó (DEC-100 · DEC-103). */
    public FolioResult conNombres(DirectorioPersonal personal) {
        Map<String, Optional<String>> nombres = Stream
                .of(cargos.stream().map(CargoResult::autor), pagos.stream().map(PagoResult::autor),
                        Stream.ofNullable(autorizacion).map(AutorizacionResult::autor))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), autor -> personal.nombreDe(new UsuarioId(autor))));
        return new FolioResult(id, reserva,
                cargos.stream().map(c -> c.conAutor(c.autor(), nombreDe(c.autor(), nombres))).toList(),
                pagos.stream().map(p -> p.conAutor(p.autor(), nombreDe(p.autor(), nombres))).toList(),
                totalPagado, saldo, situacionSaldo, cerrado,
                Optional.ofNullable(autorizacion)
                        .map(a -> a.conAutor(a.autor(), nombreDe(a.autor(), nombres)))
                        .orElse(null));
    }

    private static String nombreDe(String autor, Map<String, Optional<String>> nombres) {
        return Optional.ofNullable(autor).flatMap(nombres::get).orElse(null);
    }

    /** Lo que ve el huésped: quién del personal hizo cada movimiento no es dato suyo (USU-04 · DEC-103). */
    public FolioResult sinDatosDelPersonal() {
        return new FolioResult(id, reserva,
                cargos.stream().map(c -> c.conAutor(null, null)).toList(),
                pagos.stream().map(p -> p.conAutor(null, null)).toList(),
                totalPagado, saldo, situacionSaldo, cerrado,
                Optional.ofNullable(autorizacion)
                        .map(a -> a.conAutor(null, null))
                        .orElse(null));
    }
}
