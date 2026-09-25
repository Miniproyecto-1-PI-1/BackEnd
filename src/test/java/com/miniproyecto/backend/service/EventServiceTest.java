package com.miniproyecto.backend.service;

import com.miniproyecto.backend.dto.ClientRequest;
import com.miniproyecto.backend.dto.CreateEventRequest;
import com.miniproyecto.backend.dto.EventDetailResponse;
import com.miniproyecto.backend.dto.TaskRequest;
import com.miniproyecto.backend.entity.AppUser;
import com.miniproyecto.backend.entity.Client;
import com.miniproyecto.backend.entity.Event;
import com.miniproyecto.backend.entity.Task;
import com.miniproyecto.backend.entity.TaskStatus;
import com.miniproyecto.backend.exception.NotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                LocalTime.of(10, 30),
                null,
                null
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
    void addTask_usesExplicitEstimatedHoursAndStartsPending() {
        Event event = event(10L);
        when(eventRepository.findDetailByIdAndUserId(10L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        EventDetailResponse response = eventService.addTask(10L, task("Contratar DJ", new BigDecimal("2.5"), null));

        assertThat(response.tasks()).hasSize(1);
        assertThat(response.tasks().getFirst().estimatedHours()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(response.tasks().getFirst().status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void updateTask_changesFieldsAndStatus() {
        Event event = event(10L);
        Task existing = existingTask(event, 5L);
        when(eventRepository.findDetailByIdAndUserId(10L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);

        EventDetailResponse response = eventService.updateTask(10L, 5L, task("Confirmar menú", new BigDecimal("3"), TaskStatus.DONE));

        assertThat(existing.getName()).isEqualTo("Confirmar menú");
        assertThat(existing.getEstimatedHours()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(existing.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(response.doneTasks()).isEqualTo(1);
        assertThat(response.progress()).isEqualTo(100);
    }

    @Test
    void deleteTask_removesTaskFromEvent() {
        Event event = event(10L);
        existingTask(event, 5L);
        when(eventRepository.findDetailByIdAndUserId(10L, 1L)).thenReturn(Optional.of(event));

        eventService.deleteTask(10L, 5L);

        assertThat(event.getTasks()).isEmpty();
        verify(eventRepository).save(event);
    }

    @Test
    void taskOperations_throwNotFoundForMissingEventOrTask() {
        when(eventRepository.findDetailByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventService.deleteTask(99L, 5L)).isInstanceOf(NotFoundException.class);

        Event event = event(10L);
        when(eventRepository.findDetailByIdAndUserId(10L, 1L)).thenReturn(Optional.of(event));
        assertThatThrownBy(() -> eventService.updateTask(10L, 404L, task("X", null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Gestión no encontrada");
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

    private Event event(Long id) {
        Event event = new Event();
        event.setId(id);
        event.setUser(user);
        event.setName("Fiesta de prueba");
        event.setEventDate(LocalDate.of(2026, 12, 5));
        event.setEventTime(LocalTime.of(18, 0));
        event.setPlace("Salón Central");
        return event;
    }

    private static Task existingTask(Event event, Long id) {
        Task task = new Task();
        task.setId(id);
        task.setEvent(event);
        task.setName("Reservar salón");
        task.setDueDate(LocalDate.of(2026, 11, 20));
        task.setEstimatedHours(new BigDecimal("1.00"));
        task.setStatus(TaskStatus.PENDING);
        event.getTasks().add(task);
        return task;
    }

    private static TaskRequest task(String name, BigDecimal hours, TaskStatus status) {
        return new TaskRequest(name, null, LocalDate.of(2026, 11, 20), null, null, hours, status);
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
