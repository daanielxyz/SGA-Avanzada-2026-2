package co.edu.uniquindio.sga_avanzada_2026_2.application.compartido;

/**
 * Puerto de salida para avisar por correo y por SMS (DEC-68). Toda implementación es no bloqueante: si el envío
 * falla, lo registra y vuelve sin lanzar, para no impedir reservar ni cobrar.
 */
public interface Notificador {

    void enviarCorreo(String destino, String asunto, String cuerpo);

    void enviarSms(String telefono, String texto);
}
