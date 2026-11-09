package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.UsuarioId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.ValidacionIdentidad;

/**
 * Convierte Titular ⇄ TitularJpa. Al cargar usa el constructor del dominio, que vuelve a validar (DEC-06).
 */
final class TitularMapper {

    private TitularMapper() {
    }

    static Titular aDominio(TitularJpa jpa) {
        return new Titular(new TitularId(jpa.getId()), new AlojamientoId(jpa.getAlojamientoId()), jpa.getNombres(),
                jpa.getPrimerApellido(), jpa.getSegundoApellido(),
                new Documento(jpa.getTipoDocumento(), jpa.getNumeroDocumento()),
                jpa.getCorreo() == null ? null : new Correo(jpa.getCorreo()), jpa.getTelefono(),
                jpa.getValidadoPor() == null ? null
                        : new ValidacionIdentidad(new UsuarioId(jpa.getValidadoPor()), jpa.getValidadoEn()));
    }

    /** Copia sobre una fila nueva o ya cargada; no toca {@code version}. */
    static void copiar(Titular titular, TitularJpa jpa) {
        jpa.setId(titular.id().valor());
        jpa.setAlojamientoId(titular.alojamientoId().valor());
        jpa.setNombres(titular.nombres());
        jpa.setPrimerApellido(titular.primerApellido());
        jpa.setSegundoApellido(titular.segundoApellido());
        jpa.setTipoDocumento(titular.documento().tipo());
        jpa.setNumeroDocumento(titular.documento().numero());
        jpa.setCorreo(titular.correo() == null ? null : titular.correo().valor());
        jpa.setTelefono(titular.telefono());
        jpa.setValidadoPor(titular.validacion() == null ? null : titular.validacion().autor().valor());
        jpa.setValidadoEn(titular.validacion() == null ? null : titular.validacion().fechaHora());
    }
}
