const API_URL = 'http://localhost:8081/api/tasks';

// Elementos del DOM
const taskForm = document.getElementById('task-form');
const taskInput = document.getElementById('task-input');
const addBtn = document.getElementById('add-btn');
const taskList = document.getElementById('task-list');
const loadingIndicator = document.getElementById('loading');
const errorMessage = document.getElementById('error-message');
const emptyMessage = document.getElementById('empty-message');

// Estado local de las tareas
let tasks = [];

/**
 * Obtiene todas las tareas desde la API
 */
async function fetchTasks() {
    showLoading(true);
    hideError();

    try {
        const response = await fetch(API_URL);

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        tasks = await response.json();
        renderTasks();
    } catch (error) {
        showError('No se pudieron cargar las tareas. Verifica que el servidor esté corriendo.');
        console.error('Error al obtener tareas:', error);
    } finally {
        showLoading(false);
    }
}

/**
 * Crea una nueva tarea
 * @param {string} title - Título de la tarea
 */
async function createTask(title) {
    showLoading(true);
    hideError();

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ title, completed: false })
        });

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        const newTask = await response.json();
        tasks.push(newTask);
        renderTasks();
        taskInput.value = '';
        updateAddButtonState();
    } catch (error) {
        showError('No se pudo crear la tarea.');
        console.error('Error al crear tarea:', error);
    } finally {
        showLoading(false);
    }
}

/**
 * Alterna el estado de completada de una tarea
 * @param {string} id - ID de la tarea
 * @param {boolean} completed - Estado actual
 */
async function toggleTask(id, completed) {
    try {
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ completed })
        });

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        const updatedTask = await response.json();
        const index = tasks.findIndex(t => t.id === id);
        if (index !== -1) {
            tasks[index] = updatedTask;
        }
        renderTasks();
    } catch (error) {
        showError('No se pudo actualizar la tarea.');
        console.error('Error al actualizar tarea:', error);
        // Revertir el checkbox visual en caso de error
        renderTasks();
    }
}

/**
 * Elimina una tarea
 * @param {string} id - ID de la tarea
 */
async function deleteTask(id) {
    try {
        const response = await fetch(`${API_URL}/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }

        tasks = tasks.filter(t => t.id !== id);
        renderTasks();
    } catch (error) {
        showError('No se pudo eliminar la tarea.');
        console.error('Error al eliminar tarea:', error);
    }
}

/**
 * Renderiza la lista de tareas en el DOM
 */
function renderTasks() {
    taskList.innerHTML = '';

    if (tasks.length === 0) {
        emptyMessage.classList.remove('hidden');
        return;
    }

    emptyMessage.classList.add('hidden');

    tasks.forEach(task => {
        const li = document.createElement('li');
        li.className = `task-item${task.completed ? ' completed' : ''}`;
        li.dataset.id = task.id;

        li.innerHTML = `
            <input
                type="checkbox"
                class="task-checkbox"
                ${task.completed ? 'checked' : ''}
                aria-label="Marcar como ${task.completed ? 'pendiente' : 'completada'}"
            >
            <span class="task-title">${escapeHtml(task.title)}</span>
            <button type="button" class="delete-btn">Eliminar</button>
        `;

        // Event listeners
        const checkbox = li.querySelector('.task-checkbox');
        checkbox.addEventListener('change', () => {
            toggleTask(task.id, checkbox.checked);
        });

        const deleteBtn = li.querySelector('.delete-btn');
        deleteBtn.addEventListener('click', () => {
            deleteTask(task.id);
        });

        taskList.appendChild(li);
    });
}

/**
 * Escapa caracteres HTML para prevenir XSS
 * @param {string} text - Texto a escapar
 * @returns {string} Texto escapado
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Muestra u oculta el indicador de carga
 * @param {boolean} show
 */
function showLoading(show) {
    if (show) {
        loadingIndicator.classList.remove('hidden');
    } else {
        loadingIndicator.classList.add('hidden');
    }
}

/**
 * Muestra un mensaje de error
 * @param {string} message
 */
function showError(message) {
    errorMessage.textContent = message;
    errorMessage.classList.remove('hidden');
}

/**
 * Oculta el mensaje de error
 */
function hideError() {
    errorMessage.classList.add('hidden');
}

/**
 * Actualiza el estado del botón Añadir según el input
 */
function updateAddButtonState() {
    addBtn.disabled = taskInput.value.trim() === '';
}

// Event Listeners
taskForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const title = taskInput.value.trim();
    if (title) {
        await createTask(title);
    }
});

taskInput.addEventListener('input', updateAddButtonState);

// Inicialización
document.addEventListener('DOMContentLoaded', () => {
    fetchTasks();
    console.log('[Frontend] Listo para consumir la API en', API_URL);
});
