package co.edu.uniquindio.sga_avanzada_2026_2.domain.tra;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.MotivoViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("DEC-130")
class EnvioTraTest {

    private static final ReservaId RES = new ReservaId("RES-2026-00042");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 17, 10, 0);
    private static final OcupanteId LUIS = new OcupanteId("OCU-2");

    private static HuespedTra huesped(String id, String nombre) {
        return new HuespedTra(new OcupanteId(id), "C.C", "1094", nombre, "Ríos", "Pereira", "Pereira");
    }

    private static ReporteTra reporte() {
        return new ReporteTra(RES, huesped("OCU-1", "Ana"), List.of(huesped("OCU-2", "Luis"), huesped("OCU-3", "Eva")),
                "Turismo y vacaciones", LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 20), "APT-101",
                "Apartamento", Dinero.de(540_000), "Puerta al Sol", "123456");
    }

    @Test
    void naceSinHaberEnviadoNada() {
        EnvioTra envio = EnvioTra.pendiente(RES);

        assertEquals(EstadoEnvioTra.PENDIENTE, envio.estado());
        assertFalse(envio.principalEnviado());
        assertEquals(0, envio.intentos());
        assertEquals(2, envio.acompanantesPorEnviar(reporte()).size());
    }

    @Test
    void elPrincipalVaPrimeroYCadaAcompananteSeAnotaUnaVez() {
        EnvioTra envio = EnvioTra.pendiente(RES);
        envio.iniciarIntento(AHORA);

        assertThrows(ReglaDominioException.class, () -> envio.registrarAcompanante(LUIS));
        envio.registrarPrincipal(" 19 ");
        envio.registrarAcompanante(LUIS);

        assertEquals("19", envio.codigoPrincipal());
        assertTrue(envio.yaEnviado(LUIS));
        assertEquals(List.of("Eva"), envio.acompanantesPorEnviar(reporte()).stream().map(HuespedTra::nombres).toList());
        assertThrows(ReglaDominioException.class, () -> envio.registrarPrincipal("20"));
        assertThrows(ReglaDominioException.class, () -> envio.registrarPrincipal(" "));
    }

    @Test
    void unFalloConservaLoQueYaEntro() {
        EnvioTra envio = EnvioTra.pendiente(RES);
        envio.iniciarIntento(AHORA);
        envio.registrarPrincipal("19");
        envio.fallar("El Ministerio respondió 500");

        assertEquals(EstadoEnvioTra.CON_ERROR, envio.estado());
        assertEquals("El Ministerio respondió 500", envio.ultimoError());
        assertTrue(envio.principalEnviado());
        assertEquals(1, envio.intentos());
        assertEquals(AHORA, envio.ultimoIntento());
    }

    @Test
    void terminadoNoSeVuelveAIntentarNiAFallar() {
        EnvioTra envio = EnvioTra.pendiente(RES);
        assertThrows(ReglaDominioException.class, () -> envio.terminar(AHORA));
        envio.iniciarIntento(AHORA);
        envio.registrarPrincipal("19");
        envio.terminar(AHORA);

        assertEquals(EstadoEnvioTra.ENVIADO, envio.estado());
        assertThrows(ReglaDominioException.class, () -> envio.iniciarIntento(AHORA));
        assertThrows(ReglaDominioException.class, () -> envio.fallar("otra vez"));
    }

    @Test
    void unEstadoIncoherenteNoSeReconstruye() {
        assertThrows(ReglaDominioException.class, () -> new EnvioTra(RES, EstadoEnvioTra.ENVIADO, null, Set.of(), 1,
                AHORA, null, AHORA));
        assertThrows(ReglaDominioException.class, () -> new EnvioTra(RES, EstadoEnvioTra.CON_ERROR, null, Set.of(), 1,
                AHORA, null, null));
    }

    @Test
    void elErrorLargoSeRecortaA500() {
        EnvioTra envio = EnvioTra.pendiente(RES);
        envio.fallar("x".repeat(800));

        assertEquals(500, envio.ultimoError().length());
    }

    @Test
    void cadaDocumentoYMotivoVanComoLosEscribeElMinisterio() {
        assertEquals("C.C", ReporteTraDomainService.tipo(TipoDocumento.CC));
        assertEquals("Pasaporte", ReporteTraDomainService.tipo(TipoDocumento.PASAPORTE));
        assertEquals("T.I", ReporteTraDomainService.tipo(TipoDocumento.TI));
        assertEquals("Visita a familiares y amigos", ReporteTraDomainService.texto(MotivoViaje.VISITA_FAMILIARES_AMIGOS));
    }

    @Test
    void unHuespedSinCiudadNoSeReporta() {
        assertThrows(ReglaDominioException.class, () -> new HuespedTra(new OcupanteId("OCU-1"), "C.C", "1094", "Ana",
                "Ríos", " ", "Pereira"));
    }
}
