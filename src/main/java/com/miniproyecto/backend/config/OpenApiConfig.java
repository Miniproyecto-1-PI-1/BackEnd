package com.miniproyecto.backend.config;

import com.miniproyecto.backend.exception.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    private static final String ERROR_SCHEMA = "ApiErrorResponse";
    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        OpenAPI openApi = new OpenAPI().info(new Info()
                .title("Organizador de Eventos Independientes — API")
                .version("Sprint 2")
                .description("Eventos y gestiones del Miniproyecto 1 (Proyecto Integrador I). "
                        + "Todos los errores comparten la forma ApiErrorResponse. "
                        + "Salvo /api/auth/register, /api/auth/login y /api/health, las rutas exigen "
                        + "Authorization: Bearer <token>; el token se obtiene al registrarse o iniciar sesión."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
        ModelConverters.getInstance().read(ApiErrorResponse.class).forEach(openApi::schema);
        return openApi;
    }

    /**
     * Completa las respuestas de error que devuelven GlobalExceptionHandler y el entry point de seguridad:
     * 400 si hay body, path o query params; 401 si la ruta exige token; 404 si hay id.
     * No pisa las descripciones declaradas con @ApiResponse, pero a todo 4xx/5xx le pone el esquema ApiErrorResponse.
     */
    @Bean
    public OperationCustomizer errorResponses() {
        return (operation, handlerMethod) -> {
            boolean hasBody = hasParameter(handlerMethod, RequestBody.class);
            boolean hasId = hasParameter(handlerMethod, PathVariable.class);
            boolean hasQuery = hasParameter(handlerMethod, RequestParam.class);
            boolean isPublic = handlerMethod.hasMethodAnnotation(SecurityRequirements.class);
            ApiResponses responses = operation.getResponses();
            if (hasBody || hasId || hasQuery) {
                responses.putIfAbsent("400", new ApiResponse()
                        .description("Solicitud inválida: validación de campos, JSON mal formado o parámetro con valor inválido"));
            }
            if (!isPublic) {
                responses.putIfAbsent("401", new ApiResponse()
                        .description("No autenticado: falta el token, es inválido o está vencido"));
            }
            if (hasId) {
                responses.putIfAbsent("404", new ApiResponse().description("Evento o gestión no encontrados"));
            }
            responses.forEach((code, response) -> {
                if (code.startsWith("4") || code.startsWith("5")) {
                    response.setContent(errorContent());
                }
            });
            return operation;
        };
    }

    private static boolean hasParameter(HandlerMethod handlerMethod, Class<? extends Annotation> annotation) {
        return Arrays.stream(handlerMethod.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(annotation));
    }

    private static Content errorContent() {
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA);
        return new Content().addMediaType("application/json", new MediaType().schema(ref));
    }
}
