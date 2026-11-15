package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.externos;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.RolConsultante;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente.SeccionManual;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El manual de verdad, el de {@code asistente/manual/}, se lee completo y bien escrito (DEC-127). */
class ManualEnArchivosTest {

    private final List<SeccionManual> secciones = new ManualEnArchivos().secciones();

    @Test
    @Tag("DEC-127")
    void deberiaLeerTodasLasSeccionesConIdsDistintos() {
        Set<String> ids = secciones.stream().map(SeccionManual::id).collect(Collectors.toSet());

        assertTrue(secciones.size() >= 21, "Se leyeron " + secciones.size() + " secciones");
        assertEquals(secciones.size(), ids.size(), "Hay ids repetidos en el manual");
        assertTrue(ids.containsAll(Set.of("general", "reservas-llegada", "folio", "preparacion", "politica",
                "mi-cuenta", "usuarios")), ids.toString());
    }

    @Test
    @Tag("DEC-127")
    void deberiaDarleACadaRolLoSuyo() {
        Set<String> deServicio = idsPara(RolConsultante.PERSONAL_SERVICIO);
        Set<String> deRecepcion = idsPara(RolConsultante.RECEPCIONISTA);

        assertTrue(deServicio.containsAll(Set.of("general", "preparacion", "mi-cuenta")), deServicio.toString());
        assertTrue(deServicio.stream().noneMatch(id -> id.startsWith("reservas") || "folio".equals(id)),
                "Servicio no lee lo de reservas: " + deServicio);
        assertTrue(deRecepcion.contains("reservas-llegada"));
        assertFalse(deRecepcion.contains("usuarios"), "Usuarios es solo de administración");
        assertEquals(secciones.size(), idsPara(RolConsultante.ADMINISTRADOR).size(),
                "Administración lee todo el manual");
    }

    @Test
    @Tag("DEC-127")
    void deberiaQuitarLaCabeceraYLasComillasDelTitulo() {
        SeccionManual miCuenta = secciones.stream().filter(s -> "mi-cuenta".equals(s.id())).findFirst().orElseThrow();

        assertEquals("Mi cuenta: activarla, entrar y mi perfil", miCuenta.titulo());
        assertEquals("/perfil", miCuenta.pantalla());
        assertTrue(miCuenta.contenido().startsWith("# Mi cuenta"), miCuenta.contenido());
    }

    @Test
    @Tag("DEC-127")
    void deberiaRechazarUnArchivoSinCabeceraOConUnRolQueNoExiste() {
        assertThrows(IllegalStateException.class, () -> ManualEnArchivos.analizar("x.md", "# Sin cabecera"));
        assertThrows(IllegalStateException.class, () -> ManualEnArchivos.analizar("x.md",
                "---\nid: x\ntitulo: X\npantalla: /\nroles: HUESPED\n---\n# X"));
    }

    private Set<String> idsPara(RolConsultante rol) {
        return secciones.stream().filter(s -> s.esPara(rol)).map(SeccionManual::id).collect(Collectors.toSet());
    }
}
