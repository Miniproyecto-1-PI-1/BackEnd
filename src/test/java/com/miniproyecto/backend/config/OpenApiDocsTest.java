package com.miniproyecto.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Levanta la app completa contra H2 y comprueba que /v3/api-docs se genera con las rutas y errores del Sprint 4. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:apidocs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "DB_USER=sa",
        "DB_PASSWORD=",
        "JWT_SECRET=clave-de-pruebas-de-al-menos-32-bytes-para-hs256",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class OpenApiDocsTest {

    private static final String STATUS_PATCH = "$.paths['/api/events/{id}/tasks/{taskId}/status'].patch";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsIncludesSprint4RoutesAndOverloadConflict() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/progress']").exists())
                .andExpect(jsonPath("$.paths['/api/events/{id}/progress']").exists())
                .andExpect(jsonPath(STATUS_PATCH).exists())
                .andExpect(jsonPath(STATUS_PATCH + ".responses['409'].content['application/json'].schema.$ref",
                        endsWith("/OverloadConflictResponse")))
                .andExpect(jsonPath(STATUS_PATCH + ".responses['409'].content['application/json'].examples['Sin día libre']").exists())
                .andExpect(jsonPath(STATUS_PATCH + ".responses['400'].content['application/json'].example.errors.status")
                        .value("El estado es obligatorio."));
    }
}
