package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.ClientRequest;
import com.miniproyecto.backend.dto.CreateEventRequest;
import com.miniproyecto.backend.dto.EventDetailResponse;
import com.miniproyecto.backend.dto.TaskRequest;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Client;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.repository.AppUserRepository;
import com.miniproyecto.backend.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private EventService eventService;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1L);
        user.setName("Valentina");
    }

    private void stubCreatePersistence() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(10L);
            }
            return event;
        });
    }

    @Test
    void create_withNewClient_attachesCreatedClient() {
        stubCreatePersistence();
        ClientRequest clientRequest = new ClientRequest("Camila Ríos", "310 555 0101", "camila@mail.com");
        Client createdClient = client(20L, "Camila Ríos");
        when(clientService.findOrCreate(user, clientRequest)).thenReturn(createdClient);

        EventDetailResponse response = eventService.create(request(clientRequest, List.of()));

        assertThat(response.client().id()).isEqualTo(20L);
        assertThat(response.client().name()).isEqualTo("Camila Ríos");
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getClient()).isSameAs(createdClient);
    }

    @Test
    void create_withExistingClient_reusesClient() {
        stubCreatePersistence();
        ClientRequest clientRequest = new ClientRequest("Camila Ríos", "311 000 0000", null);
        Client existing = client(7L, "Camila Ríos");
        when(clientService.findOrCreate(user, clientRequest)).thenReturn(existing);

        EventDetailResponse response = eventService.create(request(clientRequest, List.of()));

        assertThat(response.client().id()).isEqualTo(7L);
        verify(clientService).findOrCreate(user, clientRequest);
    }

    @Test
    void create_calculatesEstimatedHoursFromTaskTimes() {
        stubCreatePersistence();
        TaskRequest task = new TaskRequest(
                "Reservar salón",
                "Llamar al lugar",
                LocalDate.of(2026, 11, 20),
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );
        when(clientService.findOrCreate(user, null)).thenReturn(null);

        EventDetailResponse response = eventService.create(request(null, List.of(task)));

        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().getFirst().estimatedHours()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(response.tasks().getFirst().status()).isEqualTo(TaskStatus.PENDING);
        assertThat(response.totalTasks()).isEqualTo(1);
        assertThat(response.doneTasks()).isZero();
        assertThat(response.progress()).isZero();
    }

    @Test
    void estimatedHours_usesDurationWithTwoDecimals() {
        assertThat(eventService.estimatedHours(LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(eventService.estimatedHours(LocalTime.of(9, 0), LocalTime.of(10, 45)))
                .isEqualByComparingTo(new BigDecimal("1.75"));
    }

    @Test
    void estimatedHours_defaultsToOneWhenTimesAreMissingOrInvalid() {
        assertThat(eventService.estimatedHours(null, LocalTime.of(10, 0)))
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(eventService.estimatedHours(LocalTime.of(10, 0), LocalTime.of(9, 0)))
                .isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(eventService.estimatedHours(LocalTime.of(9, 0), LocalTime.of(9, 0)))
                .isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void progress_roundsPercentageAndIsZeroWithoutTasks() {
        assertThat(eventService.progress(0, 0)).isZero();
        assertThat(eventService.progress(4, 1)).isEqualTo(25);
        assertThat(eventService.progress(3, 1)).isEqualTo(33);
        assertThat(eventService.progress(3, 2)).isEqualTo(67);
        assertThat(eventService.progress(2, 2)).isEqualTo(100);
    }

    private static Client client(Long id, String name) {
        Client client = new Client();
        client.setId(id);
        client.setName(name);
        return client;
    }

    private static CreateEventRequest request(ClientRequest client, List<TaskRequest> tasks) {
        return new CreateEventRequest(
                "Fiesta de prueba",
                LocalDate.of(2026, 12, 5),
                LocalTime.of(18, 0),
                "Salón Central",
                "Celebración",
                client,
                tasks
        );
    }
}
