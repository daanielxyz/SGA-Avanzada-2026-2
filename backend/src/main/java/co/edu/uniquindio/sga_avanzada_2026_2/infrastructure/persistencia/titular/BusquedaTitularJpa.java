package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.persistencia.titular;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.titular.BusquedaTitular;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * La regla de {@link BusquedaTitular} escrita como consulta, para que la base filtre y pagine (DEC-131). {@code
 * translate} quita las tildes en PostgreSQL y en H2, que la tienen igual.
 */
final class BusquedaTitularJpa {

    private static final String CON_TILDE = "áéíóúüñàèìòù";
    private static final String SIN_TILDE = "aeiouunaeiou";

    private BusquedaTitularJpa() {
    }

    static Specification<TitularJpa> de(String alojamientoId, BusquedaTitular busqueda) {
        return (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(raiz.get("alojamientoId"), alojamientoId));
            Expression<String> nombre = sinTildes(cb, cb.lower(unir(cb, " ",
                    cb.coalesce(raiz.<String>get("nombres"), ""),
                    cb.coalesce(raiz.<String>get("primerApellido"), ""),
                    cb.coalesce(raiz.<String>get("segundoApellido"), ""))));
            Expression<String> documento = cb.lower(raiz.get("numeroDocumento"));
            for (String palabra : busqueda.palabras()) {
                Predicate enNombre = cb.like(nombre, contiene(palabra), '\\');
                String comoDocumento = BusquedaTitular.comoDocumento(palabra);
                condiciones.add(comoDocumento.isEmpty()
                        ? enNombre
                        : cb.or(enNombre, cb.like(documento, contiene(comoDocumento), '\\')));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static Expression<String> unir(CriteriaBuilder cb, String separador, Expression<String> primera,
                                           Expression<String> segunda, Expression<String> tercera) {
        return cb.concat(cb.concat(cb.concat(cb.concat(primera, separador), segunda), separador), tercera);
    }

    private static Expression<String> sinTildes(CriteriaBuilder cb, Expression<String> texto) {
        return cb.function("translate", String.class, texto, cb.literal(CON_TILDE), cb.literal(SIN_TILDE));
    }

    /** «%palabra%», con los comodines de LIKE escritos por la persona tomados como letras. */
    private static String contiene(String palabra) {
        return "%" + palabra.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
