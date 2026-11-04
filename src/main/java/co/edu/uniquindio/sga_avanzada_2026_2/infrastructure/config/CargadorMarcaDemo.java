package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.ActualizarDatosAlojamientoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.ActualizarDatosAlojamientoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarMarcaAlojamientoCommand;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.CambiarMarcaAlojamientoUseCase;
import co.edu.uniquindio.sga_avanzada_2026_2.application.alojamiento.MarcaDatos;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoId;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.AlojamientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * La marca del hotel de ejemplo para las demos (BL-05): los valores de {@code web/DISENO.md}, con el contraste ya
 * medido. Va aparte de los apartamentos porque también debe llegar a una base que ya tiene datos. Solo llena lo que el
 * hotel todavía no definió: si ya tiene marca o datos propios, no los toca. Se apaga con {@code SGA_DATOS_DEMO=false}.
 */
@Component
@Order(2) // después de InicializadorAlojamiento: necesita el alojamiento
@ConditionalOnProperty(name = "sga.datos-demo", havingValue = "true")
public class CargadorMarcaDemo implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CargadorMarcaDemo.class);

    // El valor inicial de sga.alojamiento.* mientras el hotel no escribe los suyos
    private static final String SIN_DEFINIR = "Por definir";

    // TODO(equipo): boceto del logo y foto de relleno, hasta tener los definitivos del brandbook
    private static final String LOGO = "data:image/svg+xml," + URLEncoder.encode("""
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><circle cx="34" cy="26" r="10" fill="#F4B942"/>\
            <path d="M4 50C16 34 24 24 32 24s18 12 28 26c-9-7-18-11-28-11S14 43 4 50z" fill="#1B3A24"/>\
            <path d="M47 44V18M47 18c-5-3-9-2-11 1M47 18c5-3 9-2 11 1M47 18c-3-4-7-6-10-5M47 18c3-4 7-6 10-5" \
            stroke="#1B3A24" stroke-width="2.4" fill="none" stroke-linecap="round"/></svg>""", StandardCharsets.UTF_8)
            .replace("+", "%20");
    private static final String PORTADA = "https://picsum.photos/seed/quindio-valle/1600/900";

    static final MarcaDatos MARCA = new MarcaDatos(
            new MarcaDatos.ColoresDatos("#F8F7F1", "#FFFFFF", "#1B3A24", "#55705A", "#D9C9A3", "#8A8066", "#F4B942",
                    "#1B3A24", "#B3261E", "#1E7A3F"),
            new MarcaDatos.FuentesDatos("Playfair Display", "Inter"),
            new MarcaDatos.RadiosDatos("6px", "12px", "20px"),
            LOGO, PORTADA);

    private final AlojamientoRepository alojamientos;
    private final CambiarMarcaAlojamientoUseCase cambiarMarca;
    private final ActualizarDatosAlojamientoUseCase actualizarDatos;
    private final SgaPropiedades propiedades;

    public CargadorMarcaDemo(AlojamientoRepository alojamientos, CambiarMarcaAlojamientoUseCase cambiarMarca,
                             ActualizarDatosAlojamientoUseCase actualizarDatos, SgaPropiedades propiedades) {
        this.alojamientos = alojamientos;
        this.cambiarMarca = cambiarMarca;
        this.actualizarDatos = actualizarDatos;
        this.propiedades = propiedades;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        String id = propiedades.alojamiento().id();
        try {
            alojamientos.buscarPorId(new AlojamientoId(id)).ifPresent(alojamiento -> cargar(id, alojamiento));
        } catch (RuntimeException error) {
            LOG.warn("Marca de ejemplo: no se cargó ({}). El backend sigue sin ella", error.getMessage());
        }
    }

    private void cargar(String id, Alojamiento alojamiento) {
        if (alojamiento.marca().esNeutra()) {
            cambiarMarca.ejecutar(new CambiarMarcaAlojamientoCommand(id, MARCA));
            LOG.info("Marca de ejemplo: cargada en {}", id);
        }
        if (SIN_DEFINIR.equals(alojamiento.descripcion()) || SIN_DEFINIR.equals(alojamiento.ciudad())) {
            actualizarDatos.ejecutar(new ActualizarDatosAlojamientoCommand(id, alojamiento.nombre(),
                    "Apartamentos en el Eje Cafetero para descansar y reconectar con la naturaleza.",
                    "Salento, Quindío", "Salento, Quindío", alojamiento.normas()));
        }
    }
}
