package co.edu.uniquindio.sga_avanzada_2026_2.application.folio;

import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.FolioResult.AutorizacionResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.FolioResult.CargoResult;
import co.edu.uniquindio.sga_avanzada_2026_2.application.folio.FolioResult.PagoResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** DEC-103: el personal ve el nombre de quien corrigió, revirtió o devolvió; el huésped, nada de quién fue. */
class FolioResultTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 9);
    private static final AutorizacionResult AUTORIZACION = new AutorizacionResult("USR-4", "Cuenta por cobrar",
            LocalDateTime.of(2026, 11, 9, 12, 0), null);

    private static CargoResult cargo(String id, String autor) {
        return new CargoResult(id, "AJUSTE", "Corrección", BigDecimal.TEN, "DISMINUYE", HOY, null, autor, null);
    }

    private static PagoResult pago(String id, String autor) {
        return new PagoResult(id, "EFECTIVO", BigDecimal.TEN, "REVERSO", HOY, null, autor, null);
    }

    private static FolioResult folio(AutorizacionResult autorizacion) {
        return new FolioResult("FOL-1", "RES-2026-00001",
                List.of(cargo("CAR-1", null), cargo("CAR-2", "USR-1")),
                List.of(pago("PAG-1", null), pago("PAG-2", "USR-1"), pago("PAG-3", "USR-9")),
                BigDecimal.ZERO, BigDecimal.ZERO, "AL_DIA", false, autorizacion);
    }

    @Test
    @Tag("DEC-103")
    void conNombresPideCadaAutorUnaVezYDejaVaciosLosQueNoTienen() {
        List<String> consultados = new ArrayList<>();

        FolioResult resultado = folio(null).conNombres(id -> {
            consultados.add(id.valor());
            return "USR-1".equals(id.valor()) ? Optional.of("Laura Gómez") : Optional.empty();
        });

        assertEquals(2, consultados.size());
        assertNull(resultado.cargos().getFirst().autorNombre());
        assertEquals("Laura Gómez", resultado.cargos().getLast().autorNombre());
        assertEquals("USR-1", resultado.cargos().getLast().autor());
        assertNull(resultado.pagos().getFirst().autorNombre());
        assertEquals("Laura Gómez", resultado.pagos().get(1).autorNombre());
        assertNull(resultado.pagos().getLast().autorNombre()); // esa persona ya no existe
        assertEquals("USR-9", resultado.pagos().getLast().autor());
    }

    @Test
    @Tag("RN-17")
    @Tag("DEC-103")
    void conNombresTraeElNombreDeQuienAutorizoElCierre() {
        FolioResult resultado = folio(AUTORIZACION).conNombres(id -> Optional.of("Nombre de " + id.valor()));

        assertEquals("USR-4", resultado.autorizacion().autor());
        assertEquals("Nombre de USR-4", resultado.autorizacion().autorNombre());
        assertEquals("Cuenta por cobrar", resultado.autorizacion().motivo());
    }

    @Test
    @Tag("DEC-103")
    @Tag("USU-04")
    void sinDatosDelPersonalNoQuedaQuienHizoCadaMovimiento() {
        FolioResult resultado = folio(AUTORIZACION).sinDatosDelPersonal();

        resultado.cargos().forEach(c -> assertNull(c.autor()));
        resultado.pagos().forEach(p -> assertNull(p.autor()));
        assertNull(resultado.autorizacion().autor());
        assertNull(resultado.autorizacion().autorNombre());
        assertEquals("Cuenta por cobrar", resultado.autorizacion().motivo());
        assertEquals(3, resultado.pagos().size());
    }
}
