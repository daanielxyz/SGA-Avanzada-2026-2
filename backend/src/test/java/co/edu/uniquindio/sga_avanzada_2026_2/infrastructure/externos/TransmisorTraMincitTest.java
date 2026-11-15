package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Dinero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.OcupanteId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.reserva.ReservaId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.HuespedTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TraNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** El adaptador del Ministerio contra un servidor simulado: nunca llama a pms.mincit.gov.co (DEC-130). */
@Tag("DEC-130")
class TransmisorTraMincitTest {

    private static final String URL = "https://ministerio.prueba";

    private MockRestServiceServer servidor;
    private RestClient.Builder cliente;

    @BeforeEach
    void preparar() {
        cliente = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(cliente).build();
    }

    private TransmisorTraMincit transmisor(String token) {
        return new TransmisorTraMincit(new TraPropiedades("mincit", token, URL, 2), cliente);
    }

    private static HuespedTra huesped(String id, String tipo, String nombres) {
        return new HuespedTra(new OcupanteId(id), tipo, "1094", nombres, "Ríos Gómez", "Pereira", "Bogotá");
    }

    private static ReporteTra reporte() {
        return new ReporteTra(new ReservaId("RES-2026-00042"), huesped("OCU-1", "C.C", "Ana"),
                List.of(huesped("OCU-2", "Pasaporte", "Luis")), "Turismo y vacaciones", LocalDate.of(2026, 11, 17),
                LocalDate.of(2026, 11, 20), "APT-101", "Apartamento", Dinero.de(540_000), "Puerta al Sol", "123456");
    }

    @Test
    void elPrincipalVaConElTokenYLosCamposDelManual() {
        servidor.expect(requestTo(URL + "/one/"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "token secreto-de-prueba"))
                .andExpect(jsonPath("$.tipo_identificacion").value("C.C"))
                .andExpect(jsonPath("$.numero_identificacion").value("1094"))
                .andExpect(jsonPath("$.nombres").value("Ana"))
                .andExpect(jsonPath("$.apellidos").value("Ríos Gómez"))
                .andExpect(jsonPath("$.cuidad_residencia").value("Pereira"))
                .andExpect(jsonPath("$.cuidad_procedencia").value("Bogotá"))
                .andExpect(jsonPath("$.numero_habitacion").value("APT-101"))
                .andExpect(jsonPath("$.motivo").value("Turismo y vacaciones"))
                .andExpect(jsonPath("$.numero_acompanantes").value("1"))
                .andExpect(jsonPath("$.check_in").value("2026-11-17"))
                .andExpect(jsonPath("$.check_out").value("2026-11-20"))
                .andExpect(jsonPath("$.tipo_acomodacion").value("Apartamento"))
                .andExpect(jsonPath("$.costo").value("540000"))
                .andExpect(jsonPath("$.nombre_establecimiento").value("Puerta al Sol"))
                .andExpect(jsonPath("$.rnt_establecimiento").value("123456"))
                .andRespond(withSuccess("{\"code\":19}", MediaType.APPLICATION_JSON));

        assertEquals("19", transmisor("secreto-de-prueba").enviarPrincipal(reporte()));
        servidor.verify();
    }

    @Test
    void cadaAcompananteLlevaElCodigoDelPrincipalEnPadre() {
        servidor.expect(requestTo(URL + "/two/"))
                .andExpect(header("Authorization", "token secreto-de-prueba"))
                .andExpect(jsonPath("$.tipo_identificacion").value("Pasaporte"))
                .andExpect(jsonPath("$.nombres").value("Luis"))
                .andExpect(jsonPath("$.padre").value(19))
                .andExpect(jsonPath("$.motivo").doesNotExist())
                .andRespond(withSuccess("{\"code\":20}", MediaType.APPLICATION_JSON));

        ReporteTra reporte = reporte();
        transmisor("secreto-de-prueba").enviarAcompanante(reporte, reporte.acompanantes().getFirst(), "19");
        servidor.verify();
    }

    @Test
    void sinTokenNoSaleNada() {
        servidor.expect(never(), requestTo(URL + "/one/"));

        TraNoDisponibleException sinToken = assertThrows(TraNoDisponibleException.class,
                () -> transmisor(" ").enviarPrincipal(reporte()));

        assertEquals("Falta el token del Ministerio (SGA_TRA_TOKEN)", sinToken.getMessage());
        servidor.verify();
    }

    @Test
    void unRechazoOUnaCaidaDejanElMotivoSinDatosDelHuesped() {
        servidor.expect(requestTo(URL + "/one/")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        servidor.expect(requestTo(URL + "/one/")).andRespond(withServerError());
        servidor.expect(requestTo(URL + "/one/")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        TransmisorTraMincit transmisor = transmisor("secreto-de-prueba");

        assertEquals("El Ministerio respondió 401: revisa el token",
                assertThrows(TraNoDisponibleException.class, () -> transmisor.enviarPrincipal(reporte())).getMessage());
        assertEquals("El Ministerio respondió 500",
                assertThrows(TraNoDisponibleException.class, () -> transmisor.enviarPrincipal(reporte())).getMessage());
        assertEquals("El Ministerio no devolvió el código del huésped principal",
                assertThrows(TraNoDisponibleException.class, () -> transmisor.enviarPrincipal(reporte())).getMessage());
        servidor.verify();
    }

    @Test
    void elTokenNoSaleEnElTexto() {
        assertEquals("TraPropiedades[modo=mincit, url=" + URL + ", token=***]",
                new TraPropiedades("mincit", "secreto-de-prueba", URL, 2).toString());
    }
}
