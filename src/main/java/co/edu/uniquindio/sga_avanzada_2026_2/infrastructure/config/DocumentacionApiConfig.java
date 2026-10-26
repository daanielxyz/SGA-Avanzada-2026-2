package co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.config;

import co.edu.uniquindio.sga_avanzada_2026_2.infrastructure.rest.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contrato OpenAPI de la API para los dos frontends (DEC-79): springdoc lo arma solo a partir de los controladores;
 * aquí van el título y las dos formas de entrar, el token JWT del personal y del huésped y la clave de un canal.
 */
@Configuration
public class DocumentacionApiConfig {

    static final String TOKEN = "token";
    static final String CLAVE_CANAL = "claveCanal";

    @Bean
    public OpenAPI contratoApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGA · API de Puerta al Sol")
                        .version("MVP1")
                        .description("Backend de la web de reservas (web/) y del PMS (pms/). Los errores llegan "
                                + "siempre como ErrorResponse (estado, codigo, mensaje, detalles)."))
                .components(new Components()
                        .addSecuritySchemes(TOKEN, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token de POST /api/auth/login (personal) o "
                                        + "POST /api/auth/huesped/login (huésped)"))
                        .addSecuritySchemes(CLAVE_CANAL, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Canal-Clave")
                                .description("Solo para /api/canales-externos/{canalId} (DEC-73)")))
                // Swagger UI manda el token en todas las llamadas una vez se pega con «Authorize»; los endpoints
                // públicos lo ignoran
                .addSecurityItem(new SecurityRequirement().addList(TOKEN));
    }

    /**
     * Los errores no salen de los controladores sino de {@code ManejadorErrores}, así que springdoc no los ve: se
     * agrega a cada endpoint una respuesta {@code default} con {@link ErrorResponse} (DEC-33).
     */
    @Bean
    public OpenApiCustomizer respuestaDeError() {
        return contrato -> {
            ModelConverters.getInstance().readAll(ErrorResponse.class)
                    .forEach((nombre, esquema) -> contrato.getComponents().addSchemas(nombre, esquema));
            ApiResponse error = new ApiResponse()
                    .description("Error: 400 VALIDACION, 401 NO_AUTENTICADO, 403 SIN_PERMISO, 404 NO_ENCONTRADO, "
                            + "409 REGLA_NEGOCIO, entre otros")
                    .content(new Content().addMediaType("application/json", new MediaType()
                            .schema(new Schema<ErrorResponse>().$ref("#/components/schemas/ErrorResponse"))));
            contrato.getPaths().values().forEach(ruta -> ruta.readOperations()
                    .forEach(operacion -> operacion.getResponses().addApiResponse("default", error)));
        };
    }
}
