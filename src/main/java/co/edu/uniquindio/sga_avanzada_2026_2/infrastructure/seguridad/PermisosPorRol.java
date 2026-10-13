package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.seguridad;

import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Quién puede llamar cada endpoint (DEC-22 · DEC-67), según la hoja «APIs» del Excel. Las reglas se leen de arriba
 * abajo y gana la primera que coincide. El administrador puede todo lo de los roles internos. Lo que no esté aquí
 * queda solo para el administrador, y fuera de {@code /api} no se atiende nada.
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

                // Sin sesión: entrar y lo que muestra el portal (CU-02 · CU-07 · CU-08 · CU-50)
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/alojamiento", "/api/apartamentos",
                        "/api/apartamentos/disponibles", "/api/apartamentos/{codigo}").permitAll()

                // TODO(B4): el contrato del canal se autentica con la credencial del canal, no con un usuario
                //  (CAN-03 · DEC-47 · DEC-66); hasta entonces queda abierto.
                .requestMatchers(HttpMethod.POST, "/api/canales/{id}/reservas").permitAll()

                // Venta y recepción (CU-09 a CU-29). TODO(B3b): el huésped, solo sobre lo suyo (USU-04)
                .requestMatchers(HttpMethod.POST, "/api/cotizaciones")
                .hasAnyRole(HUESPED, RECEPCIONISTA, ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/api/reservas/{codigo}/corregir-registro").hasRole(ADMINISTRADOR)
                .requestMatchers("/api/reservas/**", "/api/titulares/**").hasAnyRole(RECEPCIONISTA, ADMINISTRADOR)

                // Preparación y novedades (CU-41 a CU-44 · CU-55); EOPE-04 se revisa en el controlador
                .requestMatchers(HttpMethod.PUT, "/api/apartamentos/{codigo}/estado-operativo",
                        "/api/novedades/{id}/avanzar").hasAnyRole(PERSONAL_SERVICIO, ADMINISTRADOR)
                .requestMatchers("/api/apartamentos/{codigo}/novedades")
                .hasAnyRole(PERSONAL_SERVICIO, ADMINISTRADOR)

                // Configuración, tarifas, políticas, bloqueos, canales, conflictos y bitácora
                .requestMatchers("/api/**").hasRole(ADMINISTRADOR)
                .anyRequest().denyAll();
    }
}
