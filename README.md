# Task Manager

Aplicación de gestión de tareas construida con Spring Boot.

## Descripción

Task Manager es una API REST para la gestión de tareas que permite crear, leer, actualizar y eliminar tareas. El frontend está servido estáticamente por el propio backend Spring Boot.

## Requisitos

- **Java**: 21 o superior
- **Maven**: 3.8 o superior (o usar el wrapper incluido `./mvnw`)
- **Docker**: 20.10 o superior *(solo para ejecución con Docker)*
- **Docker Compose**: 2.0 o superior *(solo para ejecución con Docker)*

---

## Arranque rápido

### Opción 1 — Maven (desarrollo local)

```bash
# 1. Compilar y ejecutar tests
./mvnw clean test

# 2. Arrancar la aplicación
./mvnw spring-boot:run
```

> En Windows sin bash: `mvnw.cmd spring-boot:run`

La aplicación queda disponible en: **http://localhost:8081**
El frontend está en: **http://localhost:8081/index.html**

---

### Opción 2 — Docker Compose (recomendado para despliegue)

```bash
# Construir la imagen y arrancar el contenedor en segundo plano
docker compose up -d --build

# Ver logs en tiempo real
docker compose logs -f

# Detener y eliminar el contenedor
docker compose down
```

La aplicación queda disponible en: **http://localhost:8081**

---

### Opción 3 — Docker directamente

```bash
# Construir la imagen
docker build -t task-manager:latest .

# Ejecutar el contenedor
docker run -d \
  --name task-manager \
  -p 8081:8081 \
  task-manager:latest
```

---

## Verificación de salud

```bash
curl http://localhost:8081/actuator/health
```

Respuesta esperada:
```json
{ "status": "UP" }
```

---

## Endpoints de la API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/tasks` | Listar todas las tareas |
| GET | `/api/tasks/{id}` | Obtener tarea por ID |
| POST | `/api/tasks` | Crear nueva tarea |
| PUT | `/api/tasks/{id}` | Actualizar tarea completa |
| PATCH | `/api/tasks/{id}` | Actualizar campo(s) parcialmente |
| DELETE | `/api/tasks/{id}` | Eliminar tarea |
| GET | `/actuator/health` | Estado de salud |

### Ejemplos con curl

**Crear una tarea**
```bash
curl -X POST http://localhost:8081/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Mi primera tarea", "completed": false}'
```

**Listar todas las tareas**
```bash
curl http://localhost:8081/api/tasks
```

**Obtener una tarea por ID**
```bash
curl http://localhost:8081/api/tasks/1
```

**Actualizar una tarea**
```bash
curl -X PUT http://localhost:8081/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Tarea actualizada", "completed": true}'
```

**Marcar como completada (actualización parcial)**
```bash
curl -X PATCH http://localhost:8081/api/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"completed": true}'
```

**Eliminar una tarea**
```bash
curl -X DELETE http://localhost:8081/api/tasks/1
```

---

## Ejecutar los tests

```bash
# Todos los tests (28 en total)
./mvnw clean test

# Solo tests de servicio
./mvnw test -Dtest=TaskServiceTest

# Solo tests de repositorio
./mvnw test -Dtest=TaskRepositoryTest

# Solo tests de controlador (integración)
./mvnw test -Dtest=TaskControllerTest
```

---

## Estructura del proyecto

```
task-manager/
├── src/
│   ├── main/
│   │   ├── java/com/taskmanager/
│   │   │   ├── config/        # Configuración Jackson
│   │   │   ├── controller/    # Controladores REST
│   │   │   ├── exception/     # Manejo global de errores
│   │   │   ├── model/         # Entidades JPA
│   │   │   ├── repository/    # Repositorios Spring Data
│   │   │   └── service/       # Lógica de negocio
│   │   └── resources/
│   │       ├── static/        # Frontend (HTML + CSS + JS)
│   │       └── application.properties
│   └── test/
│       └── java/com/taskmanager/
│           ├── controller/    # Tests de integración (MockMvc)
│           ├── repository/    # Tests de slice JPA
│           └── service/       # Tests unitarios (Mockito)
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## Configuración

### Variables de entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring activo | — |
| `JAVA_OPTS` | Opciones de JVM | `-Xms256m -Xmx512m -XX:+UseG1GC` |

### Propiedades principales (`application.properties`)

| Propiedad | Valor |
|-----------|-------|
| `server.port` | `8081` |
| `spring.datasource.url` | `jdbc:h2:mem:taskdb` (H2 en memoria) |
| `spring.jpa.hibernate.ddl-auto` | `create-drop` |

> Los datos se pierden al reiniciar la aplicación (base de datos en memoria).

---

## Pipeline CI/CD

El proyecto incluye dos workflows de GitHub Actions:

- **`ci.yml`**: Compilación, tests y build de imagen Docker.
- **`code-quality.yml`**: Checkstyle, SpotBugs, CodeQL y OWASP Dependency Check.

---

## Autor

Desarrollado por el equipo de ED-Claude.

## Licencia

Este proyecto está bajo la licencia MIT.
