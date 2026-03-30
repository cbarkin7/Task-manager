package com.taskmanager.repository;

import com.taskmanager.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    private Task sampleTask;
    private Task secondTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .title("Sample Task")
                .completed(false)
                .build();

        secondTask = Task.builder()
                .title("Second Task")
                .completed(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
        sampleTask = null;
        secondTask = null;
    }

    @Test
    @DisplayName("1. Guardar tarea - persiste correctamente")
    void save_ValidTask_PersistsSuccessfully() {
        // When
        Task savedTask = taskRepository.save(sampleTask);

        // Then
        assertThat(savedTask.getId()).isNotNull();
        assertThat(savedTask.getTitle()).isEqualTo("Sample Task");
        assertThat(savedTask.getCompleted()).isFalse();
    }

    @Test
    @DisplayName("2. FindAll con tareas - devuelve todas")
    void findAll_WithTasks_ReturnsAllTasks() {
        // Given
        entityManager.persist(sampleTask);
        entityManager.persist(secondTask);
        entityManager.flush();

        // When
        List<Task> tasks = taskRepository.findAll();

        // Then
        assertThat(tasks).hasSize(2);
        assertThat(tasks).extracting(Task::getTitle)
                .containsExactly("Sample Task", "Second Task");
    }

    @Test
    @DisplayName("3. FindAll sin tareas - devuelve lista vacia")
    void findAll_NoTasks_ReturnsEmptyList() {
        // When
        List<Task> tasks = taskRepository.findAll();

        // Then
        assertThat(tasks).isEmpty();
    }

    @Test
    @DisplayName("4. FindById con tarea existente - devuelve tarea")
    void findById_ExistingTask_ReturnsTask() {
        // Given
        Task savedTask = entityManager.persist(sampleTask);
        entityManager.flush();

        // When
        Optional<Task> result = taskRepository.findById(savedTask.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Sample Task");
        assertThat(result.get().getCompleted()).isFalse();
    }

    @Test
    @DisplayName("5. FindById con tarea inexistente - devuelve empty")
    void findById_NonExistingTask_ReturnsEmpty() {
        // When
        Optional<Task> result = taskRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("6. DeleteById elimina correctamente")
    void deleteById_ExistingTask_DeletesSuccessfully() {
        // Given
        Task savedTask = entityManager.persist(sampleTask);
        entityManager.flush();
        Long taskId = savedTask.getId();

        // When
        taskRepository.deleteById(taskId);
        entityManager.flush();

        // Then
        Optional<Task> result = taskRepository.findById(taskId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("7. ExistsById con tarea existente - devuelve true")
    void existsById_ExistingTask_ReturnsTrue() {
        // Given
        Task savedTask = entityManager.persist(sampleTask);
        entityManager.flush();

        // When
        boolean exists = taskRepository.existsById(savedTask.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("8. ExistsById con tarea inexistente - devuelve false")
    void existsById_NonExistingTask_ReturnsFalse() {
        // When
        boolean exists = taskRepository.existsById(999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("9. Count devuelve numero correcto de tareas")
    void count_WithTasks_ReturnsCorrectCount() {
        // Given
        entityManager.persist(sampleTask);
        entityManager.persist(secondTask);
        entityManager.flush();

        // When
        long count = taskRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("10. Save actualiza tarea existente")
    void save_ExistingTask_UpdatesSuccessfully() {
        // Given
        Task savedTask = entityManager.persist(sampleTask);
        entityManager.flush();

        // When
        savedTask.setTitle("Updated Title");
        savedTask.setCompleted(true);
        Task updatedTask = taskRepository.save(savedTask);
        entityManager.flush();

        // Then
        assertThat(updatedTask.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedTask.getCompleted()).isTrue();
    }
}
