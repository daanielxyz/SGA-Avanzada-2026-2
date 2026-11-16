package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.IvaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Ocupante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de dominio sin estado: el IVA de la estancia (DEC-133 · DEC-134). Se congela en la reserva desde que se
 * crea, con su cargo en el folio, y se revisa en la llegada con el documento de quien llega. Cruza Reserva, Folio y la
 * configuración del alojamiento, que recibe por parámetro; no usa repositorios.
 */
public class ImpuestoEstanciaDomainService {

    /**
     * Al crear la reserva: congela el IVA con la configuración del alojamiento y lo carga en el folio. Las tarifas
     * son la base; el anticipo se calcula ya sobre el total con IVA.
     *
     * @param documentoTitular decide si el huésped principal todavía no tiene documento
     */
    // DEC-134
    public void congelarAlCrear(Reserva reserva, Folio folio, ImpuestoAlojamiento impuesto,
                                Documento documentoTitular, LocalDate fecha) {
        reserva.congelarIva(IvaReserva.para(impuesto, documentoQueDecide(reserva.ocupantes(), documentoTitular)));
        if (reserva.iva().esPositivo()) {
            folio.cargarIva(reserva.iva(), reserva.ivaReserva().porcentaje(), fecha);
        }
    }

    /**
     * Al modificar: como las tarifas, el IVA se recalcula con la configuración vigente (RN-14 · DEC-134); la
     * diferencia queda en el folio como corrección del cargo de IVA y uno nuevo.
     */
    // DEC-134
    public void recalcularAlModificar(Reserva reserva, Folio folio, ImpuestoAlojamiento impuesto,
                                      Documento documentoTitular, LocalDate fecha) {
        reserva.congelarIva(IvaReserva.para(impuesto, documentoQueDecide(reserva.ocupantes(), documentoTitular)));
        folio.ajustarIva(reserva.iva(), reserva.ivaReserva().porcentaje(), "modificación de la reserva", fecha);
    }

    /**
     * En la llegada se revisa la exención con el documento de quien llega (DEC-134): si el huésped principal trae
     * pasaporte, el IVA se corrige en el folio; si no lo trae y la reserva se creó exenta, se carga. Una reserva
     * creada sin IVA sigue sin IVA, y la tasa congelada no cambia aunque el alojamiento haya cambiado la suya.
     */
    // DEC-134
    public void revisarEnLaLlegada(Reserva reserva, Folio folio, Documento documentoTitular, LocalDate fecha) {
        reserva.revisarIvaEnLaLlegada(documentoQueDecide(reserva.ocupantes(), documentoTitular));
        folio.ajustarIva(reserva.iva(), reserva.ivaReserva().porcentaje(),
                reserva.ivaReserva().exento() ? "llega con pasaporte" : "revisión en la llegada", fecha);
    }

    /** El documento del huésped principal del TRA (DEC-97); si todavía no lo tiene, el del titular. */
    private static Documento documentoQueDecide(List<Ocupante> ocupantes, Documento documentoTitular) {
        return ocupantes.stream()
                .filter(Ocupante::principal)
                .map(Ocupante::documento)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(documentoTitular);
    }
}
