package com.miniproyecto.backend.controller;

import com.miniproyecto.backend.dto.ClientResponse;
import com.miniproyecto.backend.dto.EventDetailResponse;
import com.miniproyecto.backend.exception.GlobalExceptionHandler;
import com.miniproyecto.backend.exception.NotFoundException;
import com.miniproyecto.backend.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        JsonMapper jsonMapper = JsonMapper.builder()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    void create_returns201AndLocation() throws Exception {
        EventDetailResponse created = new EventDetailResponse(
                15L,
                "Fiesta de prueba",
                "Celebración",
                LocalDate.of(2026, 12, 5),
                LocalTime.of(18, 0),
                "Salón Central",
                new ClientResponse(3L, "Camila Ríos", "310 555 0101", "camila@mail.com"),
                List.of(),
                0,
                0,
                0
        );
        when(eventService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fiesta de prueba",
                                  "date": "2026-12-05",
                                  "time": "18:00",
                                  "place": "Salón Central",
                                  "description": "Celebración",
                                  "client": { "name": "Camila Ríos", "phone": "310 555 0101", "email": "camila@mail.com" },
                                  "tasks": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/15"))
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.name").value("Fiesta de prueba"));
    }

    @Test
    void create_returns400WhenRequiredFieldsAreMissing() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "place": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.date").exists())
                .andExpect(jsonPath("$.errors.place").exists());

        verify(eventService, never()).create(any());
    }

    @Test
    void getById_returns404WhenEventDoesNotExist() throws Exception {
        when(eventService.getById(99L)).thenThrow(new NotFoundException("Evento no encontrado"));

        mockMvc.perform(get("/api/events/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Evento no encontrado"));
    }
}
