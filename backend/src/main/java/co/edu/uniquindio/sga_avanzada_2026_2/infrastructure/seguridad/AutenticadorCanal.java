package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.Canal;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.canal.CanalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Autentica los mensajes de un canal externo con su clave (CAN-03 · DEC-73): la que llega en el encabezado
 * {@value #ENCABEZADO} debe ser el secreto que la configuración guarda bajo la {@code referenciaCredencial} del canal,
 * en {@code sga.canales.<referencia>} o en una variable de entorno con el nombre de la referencia. El secreto nunca
 * está en el repositorio ni en la base (CAN-02 · DEC-47).
 */
@Component
public class AutenticadorCanal {

    public static final String ENCABEZADO = "X-Canal-Clave";

    private static final Logger LOG = LoggerFactory.getLogger(AutenticadorCanal.class);

    private final CanalRepository canales;
    private final Environment entorno;

    public AutenticadorCanal(CanalRepository canales, Environment entorno) {
        this.canales = canales;
        this.entorno = entorno;
    }

    /**
     * @param clave la que trajo el mensaje; nula si no trajo
     * @throws CanalNoAutenticadoException si falta la clave, no coincide, el canal no existe o no tiene secreto
     *                                     configurado; la respuesta no dice cuál de los casos fue
     */
    public void exigir(String canalId, String clave) {
        Optional<String> secreto = canales.buscarPorId(new CanalId(canalId))
                .map(Canal::referenciaCredencial)
                .map(this::secretoDe);
        if (secreto.isEmpty() && canalExterno(canalId)) {
            LOG.warn("El canal {} no tiene su secreto configurado: no puede autenticarse", canalId);
        }
        if (clave == null || secreto.isEmpty() || !iguales(clave, secreto.get())) {
            throw new CanalNoAutenticadoException();
        }
    }

    private String secretoDe(String referencia) {
        String secreto = entorno.getProperty("sga.canales." + referencia, entorno.getProperty(referencia, ""));
        return secreto.isBlank() ? null : secreto;
    }

    private boolean canalExterno(String canalId) {
        return canales.buscarPorId(new CanalId(canalId)).map(Canal::referenciaCredencial).isPresent();
    }

    /** Compara en tiempo constante, para que el tiempo de respuesta no delate cuánto de la clave acertó. */
    private static boolean iguales(String clave, String secreto) {
        return MessageDigest.isEqual(clave.getBytes(StandardCharsets.UTF_8), secreto.getBytes(StandardCharsets.UTF_8));
    }
}
