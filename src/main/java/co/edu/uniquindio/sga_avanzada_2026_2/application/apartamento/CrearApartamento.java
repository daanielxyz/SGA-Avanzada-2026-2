package co.edu.uniquindio.sga_avanzada_2026_2.application.apartamento;

import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoDuplicadoException;
import co.edu.uniquindio.sga_avanzada_2026_2.application.compartido.RecursoNoEncontradoException;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Apartamento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoRepository;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Capacidad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Caracteristica;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Dormitorio;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.Imagen;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: registrar un apartamento nuevo del alojamiento (CU-31). Nace inactivo; se activa aparte cuando
 * tenga tarifas completas.
 */
@Service
public class CrearApartamento {

    private final ApartamentoRepository apartamentos;
    private final AlojamientoRepository alojamientos;

    public CrearApartamento(ApartamentoRepository apartamentos, AlojamientoRepository alojamientos) {
        this.apartamentos = apartamentos;
        this.alojamientos = alojamientos;
    }

    /**
     * @throws RecursoNoEncontradoException si el alojamiento no existe (en BD no hay FK entre agregados, DEC-14)
     * @throws RecursoDuplicadoException    si ya hay un apartamento con ese código
     * @throws co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.ReglaDominioException si los datos violan
     *                                      una regla del apartamento
     */
    @Transactional
    public ApartamentoResult ejecutar(CrearApartamentoCommand comando) {
        AlojamientoId alojamientoId = new AlojamientoId(comando.alojamientoId());
        ApartamentoId codigo = new ApartamentoId(comando.codigo());
        alojamientos.buscarPorId(alojamientoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("el alojamiento", alojamientoId.valor()));
        apartamentos.buscarPorCodigo(codigo).ifPresent(existente -> {
            throw new RecursoDuplicadoException("el apartamento", codigo.valor());
        });

        Apartamento apartamento = Apartamento.crear(codigo, alojamientoId, comando.nombre(), comando.descripcion(),
                new Capacidad(comando.capacidad()), new Dormitorio(comando.dormitorios()),
                comando.imagenes().stream().map(i -> new Imagen(i.url(), i.principal())).toList(),
                comando.caracteristicas().stream().map(Caracteristica::new).toList());

        apartamentos.guardar(apartamento);
        return ApartamentoResult.de(apartamento);
    }
}
