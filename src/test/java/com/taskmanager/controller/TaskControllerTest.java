package com.taskmanager.controller;

import tools.jackson.databind.ObjectMapper;
import com.taskmanager.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .title("Sample Task")
                .completed(false)
                .build();
    }

    @AfterEach
    void tearDown() {
        sampleTask = null;
    }

    @Test
    @DisplayName("1. GET /api/tasks - devuelve lista vacia inicialmente")
    void getAllTasks_EmptyList_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("2. GET /api/tasks - devuelve lista con tareas")
    void getAllTasks_WithTasks_ReturnsTaskList() throws Exception {
        // Crear primera tarea
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTask)))
                .andExpect(status().isCreated());

        // Crear segunda tarea
        Task secondTask = Task.builder()
                .title("Second Task")
                .completed(true)
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondTask)))
                .andExpect(status().isCreated());

        // Verificar que devuelve lista con 2 tareas
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Sample Task")))
                .andExpect(jsonPath("$[0].completed", is(false)))
                .andExpect(jsonPath("$[1].title", is("Second Task")))
                .andExpect(jsonPath("$[1].completed", is(true)));
    }

    @Test
    @DisplayName("3. GET /api/tasks/{id} - devuelve tarea existente")
    void getTaskById_ExistingTask_ReturnsTask() throws Exception {
        // Crear tarea primero
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTask)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Task createdTask = objectMapper.readValue(responseContent, Task.class);
        Long taskId = createdTask.getId();

        // Obtener la tarea por ID
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Sample Task")))
                .andExpect(jsonPath("$.completed", is(false)));
    }

    @Test
    @DisplayName("4. GET /api/tasks/{id} - devuelve 404 si no existe")
    void getTaskById_NonExistingTask_Returns404() throws Exception {
        Long nonExistingId = 99999L;

        mockMvc.perform(get("/api/tasks/{id}", nonExistingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("Task not found with id: " + nonExistingId)));
    }

    @Test
    @DisplayName("5. POST /api/tasks - crea tarea correctamente")
    void createTask_ValidTask_ReturnsCreated() throws Exception {
        Task newTask = Task.builder()
                .title("New Task")
                .completed(false)
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.completed", is(false)));

        // Verificar que se persistio
        mockMvc.perform(get("/api/tasks"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("6. POST /api/tasks - devuelve 400 si title esta vacio")
    void createTask_EmptyTitle_Returns400() throws Exception {
        Task taskWithEmptyTitle = Task.builder()
                .title("")
                .completed(false)
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskWithEmptyTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("7. PUT /api/tasks/{id} - actualiza tarea correctamente")
    void updateTask_ExistingTask_ReturnsUpdatedTask() throws Exception {
        // Crear tarea primero
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTask)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Task createdTask = objectMapper.readValue(responseContent, Task.class);
        Long taskId = createdTask.getId();

        // Actualizar la tarea
        Task updateData = Task.builder()
                .title("Updated Title")
                .completed(true)
                .build();

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(taskId.intValue())))
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.completed", is(true)));
    }

    @Test
    @DisplayName("8. PUT /api/tasks/{id} - devuelve 404 si no existe")
    void updateTask_NonExistingTask_Returns404() throws Exception {
        Long nonExistingId = 99999L;

        Task updateData = Task.builder()
                .title("Updated Title")
                .completed(true)
                .build();

        mockMvc.perform(put("/api/tasks/{id}", nonExistingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    @DisplayName("9. DELETE /api/tasks/{id} - elimina tarea correctamente")
    void deleteTask_ExistingTask_ReturnsNoContent() throws Exception {
        // Crear tarea primero
        MvcResult createResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleTask)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseContent = createResult.getResponse().getContentAsString();
        Task createdTask = objectMapper.readValue(responseContent, Task.class);
        Long taskId = createdTask.getId();

        // Eliminar la tarea
        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        // Verificar que se eliminó
        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("10. DELETE /api/tasks/{id} - devuelve 404 si no existe")
    void deleteTask_NonExistingTask_Returns404() throws Exception {
        Long nonExistingId = 99999L;

        mockMvc.perform(delete("/api/tasks/{id}", nonExistingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")));
    }
}
