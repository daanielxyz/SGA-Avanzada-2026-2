package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Cargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.TipoCargo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DatosViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El IVA de la estancia (DEC-133): las tarifas son la base, solo la estancia, exentos los de pasaporte. */
@Tag("DEC-133")
class ImpuestoEstanciaDomainServiceTest {

    private static final ImpuestoEstanciaDomainService SERVICIO = new ImpuestoEstanciaDomainService();
    private static final ImpuestoAlojamiento COBRA = new ImpuestoAlojamiento(true, new Porcentaje(19));
    private static final Documento CC = new Documento(TipoDocumento.CC, "1094");
    private static final Documento PASAPORTE = new Documento(TipoDocumento.PASAPORTE, "AB123456");
    private static final Documento CE = new Documento(TipoDocumento.CE, "445566");
    private static final LocalDate HOY = LocalDate.of(2026, 12, 11);

    /** Dos noches de 100.000: la estancia vale 200.000. Ana es la huésped principal. */
    private static Reserva reserva(Documento documentoDeAna) {
        Reserva reserva = confirmada("RES-2026-00042", 11, 13);
        reserva.designarHuespedPrincipal(new OcupanteId("OCU-1"));
        if (documentoDeAna != null) {
            reserva.completarDatosOcupante(new OcupanteId("OCU-1"), "Ana", "Ríos", null, documentoDeAna, null,
                    DatosViaje.VACIOS);
        }
        return reserva;
    }

    private static Folio folio() {
        return Folio.abrir(new FolioId("FOL-1"), new co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId(
                "RES-2026-00042"), Dinero.de(200_000), HOY);
    }

    private static List<Cargo> ivas(Folio folio) {
        return folio.cargos().stream().filter(c -> c.tipo() == TipoCargo.IMPUESTO).toList();
    }

    @Test
    void conCedulaSePagaEl19PorCientoDeLaEstancia() {
        assertEquals(Optional.of(Dinero.de(38_000)), SERVICIO.ivaDeLaEstancia(reserva(CC), PASAPORTE, COBRA));
        assertEquals(Optional.of(Dinero.de(38_000)), SERVICIO.ivaDeLaEstancia(reserva(CE), PASAPORTE, COBRA));
    }

    @Test
    void conPasaporteNoSePagaYSinCobrarloTampoco() {
        assertTrue(SERVICIO.ivaDeLaEstancia(reserva(PASAPORTE), CC, COBRA).isEmpty());
        assertTrue(SERVICIO.ivaDeLaEstancia(reserva(CC), CC, ImpuestoAlojamiento.INICIAL).isEmpty());
    }

    @Test
    void sinDocumentoDelHuespedPrincipalDecideElDelTitular() {
        assertEquals(Optional.of(Dinero.de(38_000)), SERVICIO.ivaDeLaEstancia(reserva(null), CC, COBRA));
        assertTrue(SERVICIO.ivaDeLaEstancia(reserva(null), PASAPORTE, COBRA).isEmpty());
    }

    @Test
    void enLaLlegadaSeCargaUnaSolaVezConSuConcepto() {
        Folio folio = folio();

        SERVICIO.aplicarEnLaLlegada(folio, reserva(CC), CC, COBRA, HOY);
        SERVICIO.aplicarEnLaLlegada(folio, reserva(CC), CC, COBRA, HOY);

        assertEquals(1, ivas(folio).size());
        assertEquals("IVA del alojamiento (19 %)", ivas(folio).getFirst().concepto());
        assertEquals(Dinero.de(38_000), ivas(folio).getFirst().valor());
        assertEquals(Dinero.de(238_000), folio.saldo().monto());
    }

    @Test
    void conPasaporteLaLlegadaNoCargaNada() {
        Folio folio = folio();

        SERVICIO.aplicarEnLaLlegada(folio, reserva(PASAPORTE), CC, COBRA, HOY);

        assertTrue(ivas(folio).isEmpty());
        assertFalse(folio.tieneIvaVigente());
    }

    @Test
    void elFolioNoAceptaDosIvaVigentesPeroSiUnoDespuesDeCorregirlo() {
        Folio folio = folio();
        var primero = folio.cargarIva(Dinero.de(38_000), new Porcentaje(19), HOY);

        assertThrows(ReglaDominioException.class, () -> folio.cargarIva(Dinero.de(38_000), new Porcentaje(19), HOY));
        folio.revertirCargo(primero, "Era extranjero", HOY, new UsuarioId("USR-1"));
        assertFalse(folio.tieneIvaVigente());
    }

    @Test
    void cobrarConCeroPorCientoNoTieneSentido() {
        assertThrows(ReglaDominioException.class, () -> new ImpuestoAlojamiento(true, new Porcentaje(0)));
    }
}
