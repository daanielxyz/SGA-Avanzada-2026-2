package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.HuespedTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.ReporteTra;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TraNoDisponibleException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.tra.TransmisorTra;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link TransmisorTra} con la API del Ministerio para PMS (Resolución 409 de 2022, DEC-130), activo con
 * {@code sga.tra.modo=mincit}. {@code POST {url}/one/} con el huésped principal devuelve un {@code code};
 * {@code POST {url}/two/} envía cada acompañante con ese {@code code} en {@code padre}. Se autentica con
 * {@code Authorization: token …}. Los nombres de los campos son los del manual del Ministerio, también
 * {@code cuidad_residencia}, escrito así. Nunca deja en el log los datos del huésped ni el token.
 */
@Component
@Primary
@ConditionalOnProperty(name = "sga.tra.modo", havingValue = "mincit")
@EnableConfigurationProperties(TraPropiedades.class)
public class TransmisorTraMincit implements TransmisorTra {

    private static final Logger LOG = LoggerFactory.getLogger(TransmisorTraMincit.class);

    private final TraPropiedades propiedades;
    private final RestClient cliente;

    @Autowired
    public TransmisorTraMincit(TraPropiedades propiedades) {
        this(propiedades, RestClient.builder().requestFactory(fabrica(propiedades)));
    }

    TransmisorTraMincit(TraPropiedades propiedades, RestClient.Builder cliente) {
        this.propiedades = propiedades;
        this.cliente = cliente.baseUrl(propiedades.url()).build();
        if (!propiedades.tieneToken()) {
            LOG.warn("sga.tra.modo=mincit sin SGA_TRA_TOKEN: los envíos del TRA quedarán con error");
        }
    }

    // DEC-130
    @Override
    public String enviarPrincipal(ReporteTra reporte) {
        Map<String, Object> cuerpo = huesped(reporte.principal(), reporte);
        cuerpo.put("motivo", reporte.motivo());
        cuerpo.put("numero_acompanantes", String.valueOf(reporte.numeroAcompanantes()));
        cuerpo.put("tipo_acomodacion", reporte.tipoAcomodacion());
        cuerpo.put("costo", reporte.costo().monto().toPlainString());
        cuerpo.put("nombre_establecimiento", reporte.nombreEstablecimiento());
        cuerpo.put("rnt_establecimiento", reporte.rnt());
        Object codigo = enviar("/one/", cuerpo, reporte).get("code");
        if (codigo == null || codigo.toString().isBlank()) {
            throw new TraNoDisponibleException("El Ministerio no devolvió el código del huésped principal");
        }
        LOG.info("TRA: reserva {}, huésped principal recibido", reporte.reserva().valor());
        return codigo.toString();
    }

    @Override
    public void enviarAcompanante(ReporteTra reporte, HuespedTra acompanante, String codigoPrincipal) {
        Map<String, Object> cuerpo = huesped(acompanante, reporte);
        // El manual lo pide numérico; si el Ministerio devolviera otra cosa, se manda tal cual
        cuerpo.put("padre", codigoPrincipal.matches("[0-9]{1,18}") ? Long.valueOf(codigoPrincipal) : codigoPrincipal);
        enviar("/two/", cuerpo, reporte);
    }

    private static Map<String, Object> huesped(HuespedTra huesped, ReporteTra reporte) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("tipo_identificacion", huesped.tipoDocumento());
        cuerpo.put("numero_identificacion", huesped.numeroDocumento());
        cuerpo.put("nombres", huesped.nombres());
        cuerpo.put("apellidos", huesped.apellidos());
        cuerpo.put("cuidad_residencia", huesped.ciudadResidencia());
        cuerpo.put("cuidad_procedencia", huesped.ciudadProcedencia());
        cuerpo.put("numero_habitacion", reporte.numeroHabitacion());
        cuerpo.put("check_in", reporte.entrada().toString());
        cuerpo.put("check_out", reporte.salida().toString());
        return cuerpo;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enviar(String ruta, Map<String, Object> cuerpo, ReporteTra reporte) {
        if (!propiedades.tieneToken()) {
            throw new TraNoDisponibleException("Falta el token del Ministerio (SGA_TRA_TOKEN)");
        }
        try {
            Map<String, Object> respuesta = cliente.post().uri(ruta)
                    .header(HttpHeaders.AUTHORIZATION, "token " + propiedades.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
            return respuesta == null ? Map.of() : respuesta;
        } catch (RestClientResponseException rechazo) {
            LOG.warn("TRA: el Ministerio respondió {} a la reserva {}", rechazo.getStatusCode().value(),
                    reporte.reserva().valor());
            throw new TraNoDisponibleException("El Ministerio respondió " + rechazo.getStatusCode().value()
                    + (rechazo.getStatusCode().value() == 401 ? ": revisa el token" : ""));
        } catch (RestClientException caido) {
            LOG.warn("TRA: el Ministerio no respondió a la reserva {}", reporte.reserva().valor());
            throw new TraNoDisponibleException("El Ministerio no respondió; se intenta de nuevo más tarde");
        }
    }

    private static JdkClientHttpRequestFactory fabrica(TraPropiedades propiedades) {
        Duration limite = Duration.ofSeconds(propiedades.tiempoLimiteSegundos());
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(limite).build());
        fabrica.setReadTimeout(limite);
        return fabrica;
    }
}
