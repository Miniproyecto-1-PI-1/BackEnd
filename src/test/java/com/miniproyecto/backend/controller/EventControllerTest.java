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
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        JsonMapper jsonMapper = JsonMapper.builder().build();
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
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
                .andExpect(jsonPath("$.status").value(400))
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
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Evento no encontrado"));
    }

    @Test
    void create_returns400WithSameShapeWhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Solicitud inválida"))
                .andExpect(jsonPath("$.errors").isMap());
    }

    @Test
    void getById_returns400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/events/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void addTask_returns201() throws Exception {
        when(eventService.addTask(eq(15L), any())).thenReturn(emptyEvent());

        mockMvc.perform(post("/api/events/15/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Contratar DJ", "dueDate": "2026-11-30", "estimatedHours": 2 }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void addTask_returns400WhenNameIsBlankOrHoursAreNotPositive() throws Exception {
        mockMvc.perform(post("/api/events/15/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": " ", "estimatedHours": 0 }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("El nombre de la gestión es obligatorio."))
                .andExpect(jsonPath("$.errors.estimatedHours").value("Las horas estimadas deben ser mayores que 0."));

        verify(eventService, never()).addTask(any(), any());
    }

    @Test
    void addTask_returns400WhenEndTimeIsNotAfterStartTime() throws Exception {
        mockMvc.perform(post("/api/events/15/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Montaje", "startTime": "10:00", "endTime": "09:00" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.timeRangeValid").exists());
    }

    @Test
    void updateTask_returns200AndDeleteTaskReturns204() throws Exception {
        when(eventService.updateTask(eq(15L), eq(3L), any())).thenReturn(emptyEvent());

        mockMvc.perform(put("/api/events/15/tasks/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Contratar DJ", "startTime": "09:00", "endTime": "11:00", "status": "DONE" }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/events/15/tasks/3"))
                .andExpect(status().isNoContent());
        verify(eventService).deleteTask(15L, 3L);
    }

    private static EventDetailResponse emptyEvent() {
        return new EventDetailResponse(15L, "Fiesta de prueba", null, LocalDate.of(2026, 12, 5),
                LocalTime.of(18, 0), "Salón Central", null, List.of(), 0, 0, 0);
    }
}
