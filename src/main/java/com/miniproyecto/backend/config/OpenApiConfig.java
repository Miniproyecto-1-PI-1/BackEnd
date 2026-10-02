package com.miniproyecto.backend.config;

import com.miniproyecto.backend.exception.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    private static final String ERROR_SCHEMA = "ApiErrorResponse";
    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        OpenAPI openApi = new OpenAPI().info(new Info()
                .title("Organizador de Eventos Independientes — API")
                .version("Sprint 1")
                .description("Eventos y gestiones del Miniproyecto 1 (Proyecto Integrador I). "
                        + "Todos los errores comparten la forma ApiErrorResponse. "
                        + "Salvo /api/auth/register y /api/auth/login, las rutas exigen Authorization: Bearer <token>."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
        ModelConverters.getInstance().read(ApiErrorResponse.class).forEach(openApi::schema);
        return openApi;
    }

    /** Documenta los 400 (si hay body o id) y 404 (si hay id) que devuelve GlobalExceptionHandler. */
    @Bean
    public OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            boolean hasBody = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(RequestBody.class));
            boolean hasId = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(p -> p.hasParameterAnnotation(PathVariable.class));
            ApiResponses responses = operation.getResponses();
            if (hasBody || hasId) {
                responses.addApiResponse("400", error("Solicitud inválida: validación de campos, JSON mal formado o id no numérico"));
            }
            if (hasId) {
                responses.addApiResponse("404", error("Evento o gestión no encontrados"));
            }
            return operation;
        };
    }

    private static ApiResponse error(String description) {
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(ref)));
    }
}
