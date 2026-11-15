package co.edu.uniquindio.sga_avanzada_2026_2.application.novedad;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.apartamento.ApartamentoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.Pagina;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.EstadoNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.FiltroNovedades;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.GravedadNovedad;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.novedad.NovedadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: las novedades de todos los apartamentos en una sola lista, para atenderlas sin abrir apartamento por
 * apartamento (DEC-132 · CU-44). De la más reciente a la más antigua, de 10 en 10, con filtros de varios valores.
 */
@Service
public class ListarNovedadesUseCase {

    private final NovedadRepository novedades;

    public ListarNovedadesUseCase(NovedadRepository novedades) {
        this.novedades = novedades;
    }

    @Transactional(readOnly = true)
    public Pagina<NovedadResult> ejecutar(ListarNovedadesCommand comando) {
        FiltroNovedades filtro = new FiltroNovedades(
                conjunto(comando.estados(), EstadoNovedad::valueOf),
                conjunto(comando.gravedades(), GravedadNovedad::valueOf),
                conjunto(comando.apartamentos(), ApartamentoId::new));
        return novedades.listar(filtro, comando.numeroPagina()).map(NovedadResult::de);
    }

    private static <T> Set<T> conjunto(List<String> valores, Function<String, T> conversion) {
        return valores == null ? Set.of() : valores.stream()
                .filter(valor -> valor != null && !valor.isBlank())
                .map(String::trim)
                .map(conversion)
                .collect(Collectors.toSet());
    }
}
