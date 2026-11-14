package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Quién puede llamar cada endpoint (DEC-22 · DEC-67), según la hoja «APIs» del Excel. Las reglas se leen de arriba
 * abajo y gana la primera que coincide. El administrador puede todo lo de los roles internos. Lo que no esté aquí
 * queda solo para el administrador, y fuera de {@code /api} no se atiende nada salvo la documentación y la salud.
 */
final class PermisosPorRol {

    private static final String HUESPED = Rol.HUESPED.name();
    private static final String RECEPCIONISTA = Rol.RECEPCIONISTA.name();
    private static final String PERSONAL_SERVICIO = Rol.PERSONAL_SERVICIO.name();
    private static final String ADMINISTRADOR = Rol.ADMINISTRADOR.name();

    private PermisosPorRol() {
    }

    static void configurar(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry permisos) {
        permisos
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                // Documentación de la API (DEC-79): solo existe con SGA_DOCUMENTACION_API=true; si no, responde 404
                .requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html",
                        "/swagger-ui/**").permitAll()

                // Salud del servicio para el proveedor y el monitor de disponibilidad (DEC-121): solo dice UP o DOWN
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                // Sin sesión: entrar (personal con contraseña, huésped con PIN) y lo que muestra el portal (CU-02 ·
                // CU-07 · CU-08 · CU-50 · DEC-68)
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/huesped/pin",
                        "/api/auth/huesped/login", "/api/auth/huesped/cambio-correo").permitAll()
                // El cambio de correo no pide sesión: lo autoriza el PIN que llega al teléfono y al correo (DEC-69)
                .requestMatchers(HttpMethod.PUT, "/api/auth/huesped/cambio-correo").permitAll()
                // El personal fija su contraseña con el código que le llega al correo (CU-05 · DEC-71)
                .requestMatchers(HttpMethod.POST, "/api/auth/recuperar-contrasena", "/api/auth/restablecer-contrasena",
                        "/api/auth/activar").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                // La reserva del portal y su pago en línea: no hace falta cuenta para reservar (DEC-68 · DEC-69). Pagar
                // y liberar piden la clave de la reserva o la sesión del huésped, que revisa el controlador (DEC-93)
                .requestMatchers(HttpMethod.POST, "/api/portal/reservas", "/api/portal/reservas/{codigo}/pago")
                .permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/portal/reservas/{codigo}/liberar").permitAll()
                // Lo que falta por preparar es del personal (CU-41); va antes de la regla de {codigo}
                .requestMatchers(HttpMethod.GET, "/api/apartamentos/pendientes-preparacion")
                .hasAnyRole(PERSONAL_SERVICIO, ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, "/api/alojamiento", "/api/apartamentos/disponibles",
                        "/api/apartamentos/{codigo}/calendario").permitAll()
                // La ficha completa de un apartamento (estado operativo, motivos de bloqueos y de fuera de servicio)
                // es solo del personal; el público ve /api/portal/apartamentos (DEC-80 · DEC-113)
                .requestMatchers(HttpMethod.GET, "/api/apartamentos", "/api/apartamentos/{codigo}")
                .hasAnyRole(RECEPCIONISTA, PERSONAL_SERVICIO, ADMINISTRADOR)
                // Buscar y cotizar en el portal, sin cuenta: solo datos públicos y un cálculo que no guarda nada
                // (CU-07 · CU-08 · CU-09 · equipo web)
                .requestMatchers(HttpMethod.GET, "/api/portal/apartamentos", "/api/portal/apartamentos/disponibles",
                        "/api/portal/apartamentos/{codigo}", "/api/portal/apartamentos/{codigo}/estancia",
                        "/api/portal/politica").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/cotizaciones").permitAll()

                // El contrato del canal no usa usuario: cada mensaje trae la clave del canal, que revisa
                // AutenticadorCanal (CAN-03 · DEC-73)
                .requestMatchers("/api/canales-externos/**").permitAll()

                // El huésped: su cuenta y sus reservas; que cada reserva sea suya lo revisa el controlador (USU-04)
                .requestMatchers("/api/mi-cuenta", "/api/mis-reservas").hasRole(HUESPED)
                .requestMatchers(HttpMethod.GET, "/api/reservas/{codigo}", "/api/reservas/{codigo}/folio",
                        "/api/reservas/{codigo}/cancelacion", "/api/reservas/{codigo}/pago-en-linea")
                .hasAnyRole(HUESPED, RECEPCIONISTA, ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/api/reservas/{codigo}/cancelar")
                .hasAnyRole(HUESPED, RECEPCIONISTA, ADMINISTRADOR)

                // Venta y recepción (CU-10 a CU-29)
                .requestMatchers(HttpMethod.PUT, "/api/reservas/{codigo}/corregir-registro").hasRole(ADMINISTRADOR)
                .requestMatchers("/api/reservas/**", "/api/titulares/**", "/api/ocupacion")
                .hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)
                // Recepción lee la política congelada en la reserva y la vigente; publicar y ver el historial de
                // versiones es del administrador (RN-13 · DEC-104)
                .requestMatchers(HttpMethod.GET, "/api/politicas/{id}").hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)

                // El personal y su propia cuenta (CU-03 · CU-04); gestionar a los demás es del administrador
                .requestMatchers("/api/usuarios/perfil", "/api/usuarios/perfil/**")
                .hasAnyRole(RECEPCIONISTA, PERSONAL_SERVICIO, ADMINISTRADOR)

                // Preparación y novedades (CU-41 a CU-44 · CU-55); EOPE-04 se revisa en el controlador
                .requestMatchers(HttpMethod.PUT, "/api/apartamentos/{codigo}/iniciar-preparacion",
                        "/api/apartamentos/{codigo}/marcar-preparado",
                        "/api/apartamentos/{codigo}/marcar-pendiente-preparacion",
                        "/api/novedades/{id}/avanzar").hasAnyRole(PERSONAL_SERVICIO, ADMINISTRADOR)
                .requestMatchers("/api/apartamentos/{codigo}/novedades")
                .hasAnyRole(PERSONAL_SERVICIO, ADMINISTRADOR)

                // Configuración, tarifas, políticas, bloqueos, canales, conflictos y bitácora
                .requestMatchers("/api/**").hasRole(ADMINISTRADOR)
                .anyRequest().denyAll();
    }
}
