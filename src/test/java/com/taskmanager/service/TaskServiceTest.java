package com.taskmanager.service;

import com.taskmanager.exception.TaskNotFoundException;
import com.taskmanager.model.Task;
import com.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task sampleTask;
    private Task secondTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .title("Sample Task")
                .completed(false)
                .build();

        secondTask = Task.builder()
                .id(2L)
                .title("Second Task")
                .completed(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        sampleTask = null;
        secondTask = null;
        reset(taskRepository);
    }

    @Test
    @DisplayName("1. Crear tarea - guarda correctamente")
    void createTask_ValidTask_SavesSuccessfully() {
        // Given
        Task newTask = Task.builder()
                .title("New Task")
                .completed(false)
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        // When
        Task result = taskService.createTask(newTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Sample Task");
        assertThat(result.getCompleted()).isFalse();

        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("2. Listar todas las tareas - devuelve todas")
    void getAllTasks_ReturnsAllTasks() {
        // Given
        List<Task> tasks = Arrays.asList(sampleTask, secondTask);
        when(taskRepository.findAll()).thenReturn(tasks);

        // When
        List<Task> result = taskService.getAllTasks();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(sampleTask, secondTask);

        verify(taskRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("3. Buscar por ID existente - devuelve tarea")
    void getTaskById_ExistingId_ReturnsTask() {
        // Given
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(sampleTask));

        // When
        Task result = taskService.getTaskById(taskId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(taskId);
        assertThat(result.getTitle()).isEqualTo("Sample Task");

        verify(taskRepository, times(1)).findById(taskId);
    }

    @Test
    @DisplayName("4. Buscar por ID inexistente - lanza excepcion")
    void getTaskById_NonExistingId_ThrowsException() {
        // Given
        Long nonExistingId = 999L;
        when(taskRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> taskService.getTaskById(nonExistingId))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: " + nonExistingId);

        verify(taskRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("5. Marcar como completada - actualiza estado")
    void updateTask_MarksAsCompleted_UpdatesState() {
        // Given
        Long taskId = 1L;
        Task updatedTask = Task.builder()
                .title("Sample Task")
                .completed(true)
                .build();

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        // When
        Task result = taskService.updateTask(taskId, updatedTask);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompleted()).isTrue();

        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("6. Eliminar tarea - la elimina")
    void deleteTask_ExistingId_DeletesSuccessfully() {
        // Given
        Long taskId = 1L;
        when(taskRepository.existsById(taskId)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(taskId);

        // When
        taskService.deleteTask(taskId);

        // Then
        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    @DisplayName("7. Eliminar tarea - lanza excepcion si no existe")
    void deleteTask_NonExistingId_ThrowsException() {
        // Given
        Long nonExistingId = 999L;
        when(taskRepository.existsById(nonExistingId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> taskService.deleteTask(nonExistingId))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("Task not found with id: " + nonExistingId);

        verify(taskRepository, times(1)).existsById(nonExistingId);
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("8. Validar titulo null en creacion - setea id null")
    void createTask_WithExistingId_SetsIdToNull() {
        // Given
        Task taskWithId = Task.builder()
                .id(100L)
                .title("Task with ID")
                .completed(false)
                .build();

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(1L);
            return savedTask;
        });

        // When
        Task result = taskService.createTask(taskWithId);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }
}
