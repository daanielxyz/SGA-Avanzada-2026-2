package co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ImpuestoAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Porcentaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Folio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.FolioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.Saldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.folio.SituacionSaldo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.DatosViaje;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.IvaReserva;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.Reserva;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static co.edu.uniquindio.sga_avanzada_2026_2.domain.servicio.ReservasDePrueba.confirmada;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tarifas que ya traen el IVA (DEC-141): el huésped paga el precio que escribió el hotel, el folio separa el
 * alojamiento de su IVA y quien llega con pasaporte paga sin él.
 */
@Tag("DEC-141")
class IvaIncluidoTest {

    private static final ImpuestoEstanciaDomainService SERVICIO = new ImpuestoEstanciaDomainService();
    private static final ImpuestoAlojamiento INCLUIDO = new ImpuestoAlojamiento(true, new Porcentaje(19), true);
    private static final Documento CC = new Documento(TipoDocumento.CC, "1094");
    private static final Documento PASAPORTE = new Documento(TipoDocumento.PASAPORTE, "AB123456");
    private static final LocalDate HOY = LocalDate.of(2026, 12, 11);

    /** Dos noches de 100.000: con el IVA dentro, 200.000 son 168.067 de alojamiento y 31.933 de IVA. */
    private static Reserva reserva() {
        Reserva reserva = confirmada("RES-2026-00042", 11, 13);
        reserva.designarHuespedPrincipal(new OcupanteId("OCU-1"));
        return reserva;
    }

    // Como CrearReservaUseCase: el IVA se congela antes de abrir el folio, que carga el alojamiento sin él
    private static Folio crear(Reserva reserva, ImpuestoAlojamiento impuesto, Documento documento) {
        SERVICIO.congelar(reserva, impuesto, documento);
        Folio folio = Folio.abrir(new FolioId("FOL-1"), reserva.codigo(), reserva.valorAlojamiento(), HOY);
        SERVICIO.cargarAlCrear(reserva, folio, HOY);
        return folio;
    }

    @Test
    void elHuespedPagaElPrecioDeLaTarifaYElFolioSeparaElIva() {
        Reserva reserva = reserva();

        Folio folio = crear(reserva, INCLUIDO, CC);

        assertEquals(Dinero.de(168_067), reserva.valorAlojamiento());
        assertEquals(Dinero.de(31_933), reserva.iva());
        assertEquals(Dinero.de(200_000), reserva.valorEstancia());
        assertEquals(new Saldo(Dinero.de(200_000), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    void conPasaporteSeLeDescuentaElIvaQueTraeLaTarifa() {
        Reserva reserva = reserva();

        Folio folio = crear(reserva, INCLUIDO, PASAPORTE);

        assertTrue(reserva.ivaReserva().exento());
        assertEquals(Dinero.CERO, reserva.iva());
        assertEquals(Dinero.de(168_067), reserva.valorEstancia());
        assertEquals(new Saldo(Dinero.de(168_067), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    void siLlegaConPasaporteElIvaSeCorrigeYPagaSoloElAlojamiento() {
        Reserva reserva = reserva();
        Folio folio = crear(reserva, INCLUIDO, CC);
        reserva.completarDatosOcupante(new OcupanteId("OCU-1"), "Ana", "Ríos", null, PASAPORTE, null,
                DatosViaje.VACIOS);

        SERVICIO.revisarEnLaLlegada(reserva, folio, CC, HOY);

        assertEquals(Dinero.de(168_067), reserva.valorEstancia());
        assertEquals(new Saldo(Dinero.de(168_067), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    @Test
    void elAnticipoEsSobreElPrecioDeLaTarifa() {
        Reserva reserva = reserva();
        crear(reserva, INCLUIDO, CC);

        assertEquals(Dinero.de(60_000), reserva.anticipoExigido(new Porcentaje(30)));
    }

    @Test
    void siElIvaSeSumaTodoSigueComoAntes() {
        Reserva reserva = reserva();

        Folio folio = crear(reserva, new ImpuestoAlojamiento(true, new Porcentaje(19)), CC);

        assertEquals(Dinero.de(200_000), reserva.valorAlojamiento());
        assertEquals(Dinero.de(238_000), reserva.valorEstancia());
        assertEquals(new Saldo(Dinero.de(238_000), SituacionSaldo.PENDIENTE), folio.saldo());
    }

    // El IVA es la diferencia con la base redondeada: base más IVA da exacto el precio, cualquiera que sea
    @ParameterizedTest
    @ValueSource(longs = {119_000, 120_000, 99_999, 100_001, 1, 333_333})
    void baseMasIvaDanExactoElPrecioDeLaTarifa(long precio) {
        IvaReserva iva = new IvaReserva(new Porcentaje(19), false, true);
        Dinero tarifa = Dinero.de(precio);

        Dinero base = tarifa.restar(iva.incluidoEn(tarifa));

        assertEquals(tarifa, base.sumar(iva.sobre(tarifa)));
    }

    @Test
    void conUnaReservaSinIvaNadaSeDescuenta() {
        assertEquals(Dinero.CERO, IvaReserva.NINGUNO.incluidoEn(Dinero.de(200_000)));
        assertEquals(Dinero.de(19_000), new IvaReserva(new Porcentaje(19), false, true).sobre(Dinero.de(119_000)));
    }
}
