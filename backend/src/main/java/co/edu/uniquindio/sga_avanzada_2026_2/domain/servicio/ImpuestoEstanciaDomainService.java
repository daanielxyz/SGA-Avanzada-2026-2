package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Servicio de dominio sin estado: el IVA de la estancia al registrar la llegada (DEC-133). Cruza Reserva, Folio y la
 * configuración del alojamiento, que recibe por parámetro; no usa repositorios.
 */
public class ImpuestoEstanciaDomainService {

    /**
     * El IVA que le toca a la estancia: el porcentaje sobre el valor de las noches (las tarifas son la base), o nada
     * si el alojamiento no lo cobra o quien llega trae pasaporte (residente en otro país).
     *
     * @param documentoTitular decide si el huésped principal todavía no tiene documento
     */
    // DEC-133
    public Optional<Dinero> ivaDeLaEstancia(Reserva reserva, Documento documentoTitular, ImpuestoAlojamiento impuesto) {
        if (!impuesto.cobraIva() || documentoQueDecide(reserva, documentoTitular).tipo() == TipoDocumento.PASAPORTE) {
            return Optional.empty();
        }
        return Optional.of(reserva.valorTotal().porcentaje(impuesto.porcentaje()));
    }

    /**
     * Carga el IVA en el folio al registrar la llegada, si le toca y no tiene ya uno vigente (una llegada repetida
     * no lo duplica). Queda congelado: no se recalcula si después cambia la configuración o el documento.
     */
    // DEC-133
    public void aplicarEnLaLlegada(Folio folio, Reserva reserva, Documento documentoTitular,
                                   ImpuestoAlojamiento impuesto, LocalDate fecha) {
        if (folio.tieneIvaVigente()) {
            return;
        }
        ivaDeLaEstancia(reserva, documentoTitular, impuesto)
                .ifPresent(iva -> folio.cargarIva(iva, impuesto.porcentaje(), fecha));
    }

    /** El documento del huésped principal del TRA (DEC-97); si todavía no lo tiene, el del titular. */
    private static Documento documentoQueDecide(Reserva reserva, Documento documentoTitular) {
        return reserva.ocupantes().stream()
                .filter(Ocupante::principal)
                .map(Ocupante::documento)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(Objects.requireNonNull(documentoTitular, "documentoTitular"));
    }
}
