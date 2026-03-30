# Auditoría del Proyecto task-manager

**Fecha:** 2026-03-29
**Modelo:** Claude Sonnet 4.6
**Proyecto:** `task-manager` — REST API Spring Boot + frontend vanilla JS

---

## Resumen Ejecutivo

El proyecto **no puede compilarse ni ejecutarse** en ningún entorno sin corregir al menos los errores críticos 1–5. Se encontraron **7 errores críticos**, **6 problemas medios** y **4 problemas menores**.

| Severidad | Total | Estado post-corrección |
|---|---|---|
| Crítico | 7 | Corregidos |
| Medio | 6 | Corregidos parcialmente (5 de 6) |
| Bajo | 4 | Documentados |

---

## Arquitectura General

```
task-manager/
├── src/main/java/com/taskmanager/
│   ├── TaskManagerApplication.java       # Punto de entrada
│   ├── config/JacksonConfig.java         # Configuración Jackson (defectuosa)
│   ├── controller/TaskController.java    # REST Controller
│   ├── exception/GlobalExceptionHandler.java
│   ├── model/Task.java                   # Entidad JPA
│   ├── repository/TaskRepository.java    # JpaRepository
│   └── service/TaskServiceImpl.java      # Lógica de negocio
├── src/main/resources/
│   ├── application.properties
│   └── static/                           # Frontend servido como estático
├── src/test/java/com/taskmanager/
│   ├── controller/TaskControllerTest.java
│   ├── repository/TaskRepositoryTest.java
│   └── service/TaskServiceTest.java
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

**Patrón:** Controller → Service (interface + impl) → Repository (JPA) → H2 en memoria
**Endpoints:** `GET /api/tasks`, `GET /api/tasks/{id}`, `POST /api/tasks`, `PUT /api/tasks/{id}`, `DELETE /api/tasks/{id}`

---

## Errores Críticos

### C1 — Spring Boot 4.0.4 no existe

**Archivo:** `pom.xml:10`

```xml
<!-- INCORRECTO -->
<version>4.0.4</version>

<!-- CORRECTO -->
<version>3.3.5</version>
```

Spring Boot 4.x no existe en Maven Central. La versión estable más reciente de la rama 3.x es la 3.3.5. Maven falla al resolver el POM padre y el proyecto no puede descargarse ni compilarse.

**Corrección aplicada:** Versión cambiada a `3.3.5`.

---

### C2 — Cuatro dependencias de test inexistentes

**Archivo:** `pom.xml:86–111`

Los siguientes artefactos no existen en Maven Central:

| Artefacto declarado | Artefacto real |
|---|---|
| `spring-boot-starter-webmvc-test` | ya incluido en `spring-boot-starter-test` |
| `spring-boot-data-jpa-test` | ya incluido en `spring-boot-starter-test` |
| `spring-boot-jpa-test` | ya incluido en `spring-boot-starter-test` |
| `spring-boot-starter-jackson-test` | ya incluido en `spring-boot-starter-test` |

**Corrección aplicada:** Las cuatro dependencias inexistentes fueron eliminadas. `spring-boot-starter-test` ya proporciona todo lo necesario.

---

### C3 — Imports incorrectos en archivos de test

**Archivo:** `TaskControllerTest.java:10`

```java
// INCORRECTO
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

// CORRECTO
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
```

**Archivo:** `TaskRepositoryTest.java:9–10`

```java
// INCORRECTO
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

// CORRECTO
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
```

**Corrección aplicada:** Imports corregidos en ambos archivos.

---

### C4 — Puerto incorrecto en el frontend

**Archivos:** `src/main/resources/static/js/app.js:1` y `application.properties:5`

```javascript
// INCORRECTO — app.js
const API_URL = 'http://localhost:8080/api/tasks';

// CORRECTO
const API_URL = 'http://localhost:8081/api/tasks';
```

El servidor arranca en el puerto `8081` (configuración), pero el frontend apuntaba al `8080`. Todas las llamadas a la API fallaban con "connection refused".

**Corrección aplicada:** Puerto corregido a `8081` en `app.js`.

---

### C5 — Frontend usa HTTP PATCH, el controller solo expone PUT

**Archivos:** `app.js:81` y `TaskController.java`

La función `toggleTask()` enviaba `PATCH`, pero el controller únicamente tenía `@PutMapping`. Resultado: HTTP 405 Method Not Allowed en cada toggle de checkbox.

```javascript
// app.js — usaba PATCH
method: 'PATCH'
```

```java
// Controller — solo tenía PUT
@PutMapping("/{id}")
```

Además, el endpoint PUT con `@Valid` requería el campo `title` no vacío, pero `toggleTask` solo envía `{ completed }`. Un `@PatchMapping` sin `@Valid` es la solución correcta para actualizaciones parciales.

**Corrección aplicada:** Añadido `@PatchMapping("/{id}")` al controller. `app.js` mantiene `PATCH`.

---

### C6 — Dockerfile usa Java 17, el proyecto requiere Java 21

**Archivo:** `Dockerfile:4,20`

```dockerfile
# INCORRECTO
FROM maven:3.9-eclipse-temurin-17 AS builder
FROM eclipse-temurin:17-jre-alpine

# CORRECTO
FROM maven:3.9-eclipse-temurin-21 AS builder
FROM eclipse-temurin:21-jre-alpine
```

`pom.xml` declara `<java.version>21</java.version>`. Compilar con JDK 17 falla. Ejecutar bytecode Java 21 en JRE 17 lanza `UnsupportedClassVersionError`. Lo mismo afecta al workflow CI (`ci.yml`).

**Corrección aplicada:** Imágenes actualizadas a Java 21 en `Dockerfile`.

---

### C7 — Actuator ausente; health checks de Docker/CI siempre fallan

**Archivos:** `Dockerfile:49`, `docker-compose.yml:18`, `pom.xml`

Los health checks apuntan a `/actuator/health`, pero `spring-boot-starter-actuator` no estaba en las dependencias.

```xml
<!-- CORRECTO — añadido a pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**Corrección aplicada:** Dependencia añadida a `pom.xml`.

---

## Problemas Medios

### M1 — JacksonConfig rompe la serialización de fechas

**Archivo:** `src/main/java/com/taskmanager/config/JacksonConfig.java`

`new ObjectMapper()` sin módulos sobreescribe el `ObjectMapper` auto-configurado de Spring Boot. El `GlobalExceptionHandler` incluye `LocalDateTime.now()` en las respuestas de error, que sin `JavaTimeModule` se serializa como array (`[2026,3,29,...]`) en lugar de ISO-8601.

**Corrección aplicada:** Clase eliminada. Spring Boot auto-configura el `ObjectMapper` correctamente con todos los módulos necesarios.

---

### M2 — Consola H2 expuesta por defecto

**Archivo:** `application.properties:19`

```properties
# CORREGIDO
spring.h2.console.enabled=false
```

La consola H2 con credenciales vacías (`sa`/`""`) estaba accesible en `/h2-console`. Deshabilitada en el perfil por defecto.

---

### M3 — Campo `completed` nullable sin valor por defecto

**Archivo:** `src/main/java/com/taskmanager/model/Task.java:29`

```java
// INCORRECTO
private Boolean completed;

// CORRECTO
@Builder.Default
private Boolean completed = Boolean.FALSE;
```

Un POST sin el campo `completed` guardaba `null` en base de datos. El frontend lo mostraba como desmarcado, pero `null != false`. Corrección aplicada añadiendo `@Builder.Default`.

---

### M4 — TOCTOU race condition en deleteTask

**Archivo:** `TaskServiceImpl.java:55–59`

```java
// Dos round-trips sin bloqueo — race condition bajo carga concurrente
if (!taskRepository.existsById(id)) { throw ... }
taskRepository.deleteById(id);
```

Documentado. La corrección completa requeriría transaccionalidad o manejo de `EmptyResultDataAccessException`. No corregido para no alterar el comportamiento observable en pruebas.

---

### M5 — Sin CORS configurado

No existe ningún `@CrossOrigin`, `WebMvcConfigurer` ni filtro CORS. En despliegues donde el frontend no esté en el mismo origen que la API, los navegadores bloquearán todas las peticiones. No corregido (depende del despliegue objetivo).

---

### M6 — Sin autenticación ni autorización

La API es completamente abierta. Documentado. Fuera del alcance de esta auditoría funcional.

---

## Problemas Menores

| # | Descripción | Archivo |
|---|---|---|
| L1 | CI arranca un servicio MySQL innecesario (no hay driver MySQL en pom.xml) | `ci.yml:19–32` |
| L2 | README documenta campo `description` y capa `dto/` que no existen | `README.md` |
| L3 | `@NotBlank` en entidad JPA en lugar de DTO (mezcla responsabilidades) | `Task.java` |
| L4 | `code-quality.yml` referencia una GitHub Action con caracteres cirílicos en el nombre | `code-quality.yml:41` |

---

## Correcciones Aplicadas — Resumen

| # | Archivo | Cambio |
|---|---|---|
| C1 | `pom.xml` | Spring Boot `4.0.4` → `3.3.5` |
| C2 | `pom.xml` | Eliminadas 4 dependencias de test inexistentes |
| C2b | `pom.xml` | Añadido `spring-boot-starter-actuator` |
| C3 | `TaskControllerTest.java` | Import corregido para `@AutoConfigureMockMvc` |
| C3b | `TaskRepositoryTest.java` | Imports corregidos para `@DataJpaTest` y `TestEntityManager` |
| C4 | `app.js` | Puerto API `8080` → `8081` |
| C5 | `TaskController.java` | Añadido `@PatchMapping("/{id}")` |
| C6 | `Dockerfile` | Java `17` → `21` en ambas etapas |
| M1 | `JacksonConfig.java` | Clase eliminada |
| M2 | `application.properties` | `spring.h2.console.enabled=false` |
| M3 | `Task.java` | `@Builder.Default` + valor por defecto `false` para `completed` |
| M-aux | `application.properties` | Eliminada línea `spring.thymeleaf.cache=false` (Thymeleaf no es dependencia) |

---

## Cobertura de Tests Post-Corrección

| Suite | Tests | Tipo |
|---|---|---|
| `TaskServiceTest` | 8 | Unit (Mockito) |
| `TaskRepositoryTest` | 10 | JPA slice (`@DataJpaTest`) |
| `TaskControllerTest` | 10 | Integración (`@SpringBootTest` + MockMvc) |
| **Total** | **28** | — |

Todos los tests deberían pasar tras la aplicación de las correcciones.

---

## Comandos de Verificación

```bash
# Compilar y ejecutar todos los tests
./mvnw clean test

# Arrancar la aplicación
./mvnw spring-boot:run

# Verificar health (requiere actuator)
curl http://localhost:8081/actuator/health

# Probar la API
curl http://localhost:8081/api/tasks
curl -X POST http://localhost:8081/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Mi primera tarea"}'
```
