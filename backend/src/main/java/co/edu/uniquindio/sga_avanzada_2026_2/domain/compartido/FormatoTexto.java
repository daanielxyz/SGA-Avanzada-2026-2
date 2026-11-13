package co.edu.uniquindio.sga_avanzada_2026_2.domain.compartido;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fechas y horas como las lee una persona en los mensajes de rechazo (#8 · DEC-118): «13/11/2026» y «13/11/2026 a las
 * 22:00», no el formato de máquina «2026-11-13T22:00».
 */
public final class FormatoTexto {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private FormatoTexto() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    public static String fechaHora(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA) + " a las " + fechaHora.format(HORA);
    }
}
