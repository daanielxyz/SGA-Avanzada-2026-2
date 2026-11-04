package co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicionalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FiltroBusquedaTest {

    private static final Apartamento APT = Apartamento.crear(new ApartamentoId("APT-101"), new AlojamientoId("ALO-1"),
            "Suite", null, new Capacidad(4), new Dormitorio(2), List.of(new Imagen("https://cdn.sga.co/a.jpg", true)),
            List.of(new Caracteristica("Wifi"), new Caracteristica("Balcón")));
    private static final List<ServicioAdicional> SERVICIOS = List.of(new ServicioAdicional(
            new ServicioAdicionalId("SRV-1"), "Parqueadero", false, Dinero.CERO, true));

    private static FiltroBusqueda filtro(Integer capacidad, List<String> caracteristicas, List<String> servicios) {
        return new FiltroBusqueda(capacidad, null, null, caracteristicas, servicios);
    }

    @Test
    void sinFiltrosAdmiteTodo() {
        assertTrue(filtro(null, null, null).admite(APT, SERVICIOS));
    }

    @Test
    void filtraPorCapacidad() {
        assertTrue(filtro(4, null, null).admite(APT, SERVICIOS));
        assertFalse(filtro(5, null, null).admite(APT, SERVICIOS));
    }

    @Test
    void filtraPorCaracteristicasSinDistinguirMayusculas() {
        assertTrue(filtro(null, List.of("wifi", "BALCÓN"), null).admite(APT, SERVICIOS));
        assertFalse(filtro(null, List.of("wifi", "piscina"), null).admite(APT, SERVICIOS));
    }

    @Test
    void filtraPorServiciosDelAlojamiento() {
        assertTrue(filtro(null, null, List.of("parqueadero")).admite(APT, SERVICIOS));
        assertFalse(filtro(null, null, List.of("spa")).admite(APT, SERVICIOS));
    }

    @Test
    void filtraPorRangoDePrecio() {
        FiltroBusqueda rango = new FiltroBusqueda(null, new BigDecimal("100"), new BigDecimal("200"), null, null);

        assertTrue(rango.admitePrecio(new BigDecimal("150")));
        assertFalse(rango.admitePrecio(new BigDecimal("99")));
        assertFalse(rango.admitePrecio(new BigDecimal("201")));
    }

    @Test
    void elPrecioMinimoNoPuedeSuperarAlMaximo() {
        assertThrows(ReglaDominioException.class,
                () -> new FiltroBusqueda(null, new BigDecimal("200"), new BigDecimal("100"), null, null));
    }
}
