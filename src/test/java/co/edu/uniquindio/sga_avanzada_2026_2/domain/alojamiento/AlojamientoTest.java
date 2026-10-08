package co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlojamientoTest {

    private static final MedioPago EFECTIVO = new MedioPago("EFECTIVO");
    private static final MedioPago TARJETA = new MedioPago("TARJETA");
    private static final MedioPago PSE = new MedioPago("PSE");
    private static final ServicioAdicionalId DESAYUNO = new ServicioAdicionalId("SRV-1");
    private static final ServicioAdicionalId PARQUEADERO = new ServicioAdicionalId("SRV-2");

    private static ParametrosAlojamiento parametros(int minimoMedios, int minimoServicios) {
        return new ParametrosAlojamiento(12, LocalTime.of(15, 0), LocalTime.of(11, 0), Duration.ofHours(3),
                Duration.ofHours(24), LocalTime.of(22, 0), new Porcentaje(30), minimoMedios, minimoServicios, 2, 2, 2);
    }

    private static ServicioAdicional servicio(ServicioAdicionalId id) {
        return new ServicioAdicional(id, "Servicio " + id.valor(), true, Dinero.de(20_000), true);
    }

    private static Alojamiento alojamiento(String nombre, ParametrosAlojamiento parametros,
                                           List<ServicioAdicional> servicios, List<MedioPago> medios) {
        return new Alojamiento(new AlojamientoId("ALO-1"), nombre, "Apartamentos frente al mar", "Santa Marta",
                "Calle 1 # 2-3", new Ubicacion(11.24, -74.21), "No mascotas", parametros, servicios, medios);
    }

    /** Puerta al Sol con los mínimos de la Ficha: 2 medios y 1 servicio. */
    private static Alojamiento puertaAlSol() {
        return alojamiento("Puerta al Sol", parametros(2, 1), List.of(servicio(DESAYUNO), servicio(PARQUEADERO)),
                List.of(EFECTIVO, TARJETA, PSE));
    }

    // --- Datos obligatorios y mínimos de la Ficha

    @Test
    @Tag("ALO-02")
    void deberiaCrearAlojamientoConSusDatos() {
        Alojamiento alojamiento = puertaAlSol();

        assertEquals("Puerta al Sol", alojamiento.nombre());
        assertEquals(3, alojamiento.mediosPago().size());
        assertEquals(2, alojamiento.serviciosAdicionales().size());
    }

    @Test
    @Tag("ALO-02")
    void deberiaRechazarAlojamientoSinNombre() {
        assertThrows(ReglaDominioException.class, () -> alojamiento(" ", parametros(2, 1),
                List.of(servicio(DESAYUNO)), List.of(EFECTIVO, TARJETA)));
    }

    @Test
    @Tag("ALO-06")
    @Tag("MPAG-01")
    void deberiaRechazarMenosMediosDePagoQueElMinimo() {
        assertThrows(ReglaDominioException.class, () -> alojamiento("Puerta al Sol", parametros(2, 1),
                List.of(servicio(DESAYUNO)), List.of(EFECTIVO)));
    }

    @Test
    @Tag("ALO-06")
    @Tag("SERV-05")
    void deberiaRechazarMenosServiciosQueElMinimo() {
        assertThrows(ReglaDominioException.class, () -> alojamiento("Puerta al Sol", parametros(2, 1),
                List.of(), List.of(EFECTIVO, TARJETA)));
    }

    @Test
    @Tag("ALO-06")
    void deberiaPermitirOperarConMenosSiElMinimoConfiguradoLoPermite() {
        Alojamiento hotelPequeno = alojamiento("Hotel real", parametros(1, 0), List.of(), List.of(EFECTIVO));

        assertEquals(1, hotelPequeno.mediosPago().size());
    }

    @Test
    @Tag("ALO-02")
    void deberiaRechazarMediosDePagoRepetidos() {
        assertThrows(ReglaDominioException.class, () -> alojamiento("Puerta al Sol", parametros(2, 1),
                List.of(servicio(DESAYUNO)), List.of(EFECTIVO, new MedioPago("efectivo"))));
    }

    // --- Parámetros y ubicación

    @Test
    @Tag("ALO-03")
    @Tag("ALO-04")
    void deberiaReemplazarLosParametros() {
        Alojamiento alojamiento = puertaAlSol();

        alojamiento.cambiarParametros(parametros(3, 1));

        assertEquals(3, alojamiento.parametros().minimoMediosPago());
    }

    @Test
    @Tag("ALO-06")
    void deberiaRechazarParametrosConMinimosQueElCatalogoNoCumple() {
        Alojamiento alojamiento = puertaAlSol();

        assertThrows(ReglaDominioException.class, () -> alojamiento.cambiarParametros(parametros(4, 1)));
        assertEquals(2, alojamiento.parametros().minimoMediosPago());
    }

    @Test
    @Tag("UBI-03")
    void deberiaReemplazarLaUbicacion() {
        Alojamiento alojamiento = puertaAlSol();

        alojamiento.cambiarUbicacion(new Ubicacion(4.53, -75.68));

        assertEquals(new Ubicacion(4.53, -75.68), alojamiento.ubicacion());
    }

    // --- Medios de pago

    @Test
    @Tag("RN-15")
    @Tag("MPAG-02")
    void deberiaAceptarMedioDePagoHabilitado() {
        assertTrue(puertaAlSol().aceptaMedioPago(TARJETA));
    }

    @Test
    @Tag("RN-15")
    @Tag("MPAG-02")
    void deberiaRechazarMedioDePagoDeshabilitado() {
        Alojamiento alojamiento = puertaAlSol();
        alojamiento.deshabilitarMedioPago(PSE);

        assertFalse(alojamiento.aceptaMedioPago(PSE));
        assertFalse(alojamiento.aceptaMedioPago(new MedioPago("BITCOIN")));
    }

    @Test
    @Tag("MPAG-06")
    void deberiaHabilitarUnMedioNuevo() {
        Alojamiento alojamiento = puertaAlSol();

        alojamiento.habilitarMedioPago(new MedioPago("Nequi"));

        assertTrue(alojamiento.aceptaMedioPago(new MedioPago("NEQUI")));
    }

    @Test
    @Tag("MPAG-06")
    void deberiaRechazarHabilitarUnMedioYaHabilitado() {
        assertThrows(ReglaDominioException.class, () -> puertaAlSol().habilitarMedioPago(EFECTIVO));
    }

    @Test
    @Tag("ALO-06")
    @Tag("MPAG-01")
    void deberiaRechazarDeshabilitarSiQuedaPorDebajoDelMinimo() {
        Alojamiento alojamiento = puertaAlSol();
        alojamiento.deshabilitarMedioPago(PSE);

        assertThrows(ReglaDominioException.class, () -> alojamiento.deshabilitarMedioPago(TARJETA));
        assertTrue(alojamiento.aceptaMedioPago(TARJETA));
    }

    @Test
    @Tag("MPAG-06")
    void deberiaRechazarDeshabilitarUnMedioNoHabilitado() {
        assertThrows(ReglaDominioException.class,
                () -> puertaAlSol().deshabilitarMedioPago(new MedioPago("BITCOIN")));
    }

    // --- Servicios adicionales

    @Test
    @Tag("SERV-01")
    void deberiaAgregarServicioAdicional() {
        Alojamiento alojamiento = puertaAlSol();

        alojamiento.agregarServicioAdicional(servicio(new ServicioAdicionalId("SRV-3")));

        assertEquals(3, alojamiento.serviciosAdicionales().size());
    }

    @Test
    @Tag("SERV-01")
    void deberiaRechazarServicioConIdRepetido() {
        assertThrows(ReglaDominioException.class, () -> puertaAlSol().agregarServicioAdicional(servicio(DESAYUNO)));
    }

    @Test
    @Tag("SERV-02")
    void deberiaCambiarElValorDeUnServicio() {
        Alojamiento alojamiento = puertaAlSol();

        alojamiento.cambiarValorServicio(DESAYUNO, Dinero.de(30_000));

        assertEquals(Dinero.de(30_000), alojamiento.serviciosAdicionales().getFirst().valor());
    }

    @Test
    @Tag("SERV-04")
    void deberiaDesactivarServicioSinBorrarlo() {
        Alojamiento alojamiento = puertaAlSol();

        alojamiento.desactivarServicio(PARQUEADERO);

        assertEquals(2, alojamiento.serviciosAdicionales().size());
        assertFalse(alojamiento.serviciosAdicionales().get(1).activo());
    }

    @Test
    @Tag("ALO-06")
    @Tag("SERV-05")
    void deberiaRechazarDesactivarElUltimoServicioActivo() {
        Alojamiento alojamiento = puertaAlSol();
        alojamiento.desactivarServicio(PARQUEADERO);

        assertThrows(ReglaDominioException.class, () -> alojamiento.desactivarServicio(DESAYUNO));
    }

    @Test
    @Tag("SERV-04")
    void deberiaRechazarOperarSobreUnServicioInexistente() {
        assertThrows(ReglaDominioException.class,
                () -> puertaAlSol().desactivarServicio(new ServicioAdicionalId("SRV-9")));
    }

    @Test
    @Tag("SERV-04")
    void noDeberiaPermitirModificarElCatalogoDesdeFuera() {
        Alojamiento alojamiento = puertaAlSol();

        assertThrows(UnsupportedOperationException.class, () -> alojamiento.mediosPago().add(PSE));
        assertThrows(UnsupportedOperationException.class,
                () -> alojamiento.serviciosAdicionales().add(servicio(new ServicioAdicionalId("SRV-7"))));
    }
}
