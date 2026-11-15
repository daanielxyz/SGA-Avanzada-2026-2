package co.edu.uniquindio.sga_avanzada_2026_2.domain.asistente;

import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.Alojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.CobroParqueadero;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ParametrosAlojamiento;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.alojamiento.ServicioAdicional;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido.MedioPago;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.BaseRetencion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.Penalizacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.PoliticaCancelacion;
import co.edu.uniquindio.sga_avanzada_2026_2.domain.politica.TramoCancelacion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Cuenta en texto la configuración del alojamiento que el asistente usa para responder con los valores de este hotel
 * (DEC-127): horarios, plazos, anticipo, edad de cobro, parqueadero, mascotas, medios de pago, servicios, normas y la
 * política de cancelación vigente. Nada de huéspedes ni de reservas. Sin estado: recibe los datos por parámetro.
 */
public class ContextoAlojamientoDomainService {

    /**
     * @param vigente la política de cancelación vigente, si ya se publicó alguna
     */
    // DEC-127 · ALO-03 · POL-01
    public String describir(Alojamiento alojamiento, Optional<PoliticaCancelacion> vigente) {
        ParametrosAlojamiento p = alojamiento.parametros();
        List<String> lineas = new ArrayList<>();
        lineas.add("Alojamiento: " + alojamiento.nombre() + " (" + alojamiento.ciudad() + ").");
        lineas.add("Hora de entrada: " + p.horaEntrada() + ". Hora de salida: " + p.horaSalida()
                + ". Hora límite de llegada (después ya no se confirma una pendiente y se puede declarar no-show): "
                + p.horaLimiteNoShow() + ".");
        lineas.add("Plazo para confirmar una reserva pendiente: " + p.plazoConfirmacion().toHours()
                + " horas. Anticipo para confirmar: " + p.anticipo().valor() + " % del total.");
        lineas.add("Edad desde la que se cobra a un ocupante: " + p.umbralEdadFacturable()
                + " años. Tiempo de preparación entre reservas: " + p.tiempoPreparacion().toHours() + " horas.");
        lineas.add(parqueadero(p));
        lineas.add(mascotas(p));
        lineas.add("Medios de pago de recepción: " + alojamiento.mediosPago().stream().map(MedioPago::nombre)
                .filter(nombre -> !"PAGO_EN_LINEA".equals(nombre)).collect(Collectors.joining(", ")) + ".");
        lineas.add("Servicios adicionales activos: " + servicios(alojamiento.serviciosAdicionales()) + ".");
        if (alojamiento.normas() != null && !alojamiento.normas().isBlank()) {
            lineas.add("Normas del alojamiento: " + alojamiento.normas().strip());
        }
        lineas.add(vigente.map(ContextoAlojamientoDomainService::politica)
                .orElse("Todavía no hay política de cancelación publicada."));
        return String.join("\n", lineas);
    }

    private static String parqueadero(ParametrosAlojamiento p) {
        if (p.cupoParqueadero() == 0) {
            return "Parqueadero: no hay.";
        }
        String cobro = p.cobroParqueadero() == CobroParqueadero.POR_NOCHE ? "por noche" : "por estadía";
        return "Parqueadero: " + p.cupoParqueadero() + " cupos entre carros y motos; carro " + p.cargoCarro().enPesos()
                + " y moto " + p.cargoMoto().enPesos() + ", " + cobro + ".";
    }

    private static String mascotas(ParametrosAlojamiento p) {
        if (!p.aceptaMascotas()) {
            return "Mascotas: no se aceptan.";
        }
        return "Mascotas: hasta " + p.limiteMascotas() + " por reserva (cada apartamento puede tener su propio límite), "
                + p.cargoMascota().enPesos() + " por mascota, una vez por estancia.";
    }

    private static String servicios(List<ServicioAdicional> servicios) {
        String activos = servicios.stream().filter(ServicioAdicional::activo)
                .map(s -> s.nombre() + (s.generaCargo() ? " (" + s.valor().enPesos() + ")" : " (incluido)"))
                .collect(Collectors.joining(", "));
        return activos.isEmpty() ? "ninguno" : activos;
    }

    private static String politica(PoliticaCancelacion politica) {
        String tramos = politica.tramos().stream()
                .sorted(Comparator.comparingInt(TramoCancelacion::antelacionMinHoras).reversed())
                .map(t -> "con al menos " + t.antelacionMinHoras() + " horas de antelación a la entrada, "
                        + retencion(t.penalizacion()))
                .collect(Collectors.joining("; "));
        return "Política de cancelación vigente (versión " + politica.version() + "; cada reserva usa la versión con "
                + "que se creó, que puede ser otra): si se cancela " + tramos + ". Si no se presenta: "
                + retencion(politica.penalizacionNoShow()) + ".";
    }

    private static String retencion(Penalizacion penalizacion) {
        if (penalizacion.porcentaje() != null) {
            return penalizacion.porcentaje().valor() == 0 ? "no se retiene nada"
                    : "se retiene el " + penalizacion.porcentaje().valor() + " % " + base(penalizacion.base());
        }
        return "se retienen " + penalizacion.montoFijo().enPesos() + ", sin pasar " + base(penalizacion.base());
    }

    private static String base(BaseRetencion base) {
        return switch (base) {
            case VALOR_TOTAL -> "del valor total";
            case PAGADO -> "de lo pagado";
            case ANTICIPO_EXIGIDO -> "del anticipo exigido";
        };
    }
}
