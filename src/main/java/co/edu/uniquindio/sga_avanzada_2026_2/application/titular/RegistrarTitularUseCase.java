package co.edu.uniquindio.sga_avanzada_2026_2.application.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.GeneradorCodigos;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.SerieCodigo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Correo;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Documento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.TipoDocumento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.Titular;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.TitularRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Caso de uso: recepción registra un titular sin crear todavía una reserva (TIT-03). Al crear una reserva el titular
 * se registra o se reutiliza solo (DEC-58).
 */
@Service
public class RegistrarTitularUseCase {

    private final TitularRepository titulares;
    private final AlojamientoRepository alojamientos;
    private final GeneradorCodigos codigos;

    public RegistrarTitularUseCase(TitularRepository titulares, AlojamientoRepository alojamientos,
                                   GeneradorCodigos codigos) {
        this.titulares = titulares;
        this.alojamientos = alojamientos;
        this.codigos = codigos;
    }

    /**
     * @throws RecursoNoEncontradoException si no existe el alojamiento
     * @throws RecursoDuplicadoException    si el alojamiento ya tiene un titular con ese documento (TIT-05 · DEC-63)
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si falta el nombre o el
     *                                      documento, no trae ningún contacto o alguno es inválido (DEC-49)
     */
    @Transactional
    public TitularResult ejecutar(RegistrarTitularCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        alojamientos.buscarPorId(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", alojamientoId.valor()));
        Documento documento = new Documento(TipoDocumento.valueOf(comando.tipoDocumento()),
                comando.numeroDocumento());
        titulares.buscarPorDocumento(alojamientoId, documento).ifPresent(existente -> {
            throw new RecursoDuplicadoException("el titular con documento",
                    documento.tipo() + " " + documento.numero() + " (" + existente.id().valor() + ")");
        });

        Titular titular = new Titular(new TitularId(codigos.siguiente(SerieCodigo.TITULAR)), alojamientoId,
                comando.nombre(), documento, correo(comando.correo()), comando.telefono());

        titulares.guardar(titular);
        return TitularResult.de(titular);
    }

    /** Un correo vacío es un correo que no llegó. */
    static Correo correo(String valor) {
        return Optional.ofNullable(valor).filter(c -> !c.isBlank()).map(Correo::new).orElse(null);
    }
}
