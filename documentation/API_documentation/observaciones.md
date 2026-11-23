# Observaciones del código y su atención

Revisión del backend contra la especificación de endpoints (`Definición_Endpoints_LMS_V1.pdf`) y la base de datos.

## Atendidas

| # | Observación | Cómo se resolvió |
|---|---|---|
| 1 | `GET /cursos` (y todo lo que incluye un usuario) devolvía el `passwordHash` | `@JsonIgnore` en `Usuario.passwordHash` |
| 2 | Las fechas automáticas (`fecha_registro`, `fecha_inscripcion`, `fecha_creacion`, `fecha_comentario`, `fecha_envio`, `fecha_realizacion`) se guardaban en `NULL`: las entidades esperaban un `DEFAULT CURRENT_TIMESTAMP` de MySQL, pero las tablas las crea Hibernate y no lo define | `@CreationTimestamp` en las 6 entidades |
| 3 | La especificación pide `PATCH` en 6 endpoints que estaban como `PUT` | Cambiados a `@PatchMapping`: `/usuarios/{id}/password`, `/usuarios/{id}/rol`, `/prorrogas/{id}`, `/respuestas/{id}`, `/notificaciones/{id}/leida`, `/notificaciones/leidas` |
| 4 | No existía `PATCH /entregas/{id}/calificacion` | Implementado: solo el maestro, calificación de 0 a 100, avisa con notificación al alumno o al equipo |
| 5 | Repositorios duplicados para la misma tabla (`CursoRepository`/`ICurso`, `UsuarioRepository`/`IUsuario`) | Se dejaron solo `ICurso` e `IUsuario` (ahora `JpaRepository`) |
| 6 | Al calificar una respuesta abierta, el resultado del examen se quedaba en `PENDIENTE_MANUAL` y sin calificación final | Cuando todas las respuestas tienen puntaje, el resultado pasa a `CALIFICADO` y `calificacion_final` es la suma |
| 7 | No había autenticación: cualquiera podía mandar el `idUsuario` de otra persona | Login con token JWT (HS256, 8 horas). Todas las rutas menos login y registro piden `Authorization: Bearer`; si `?idUsuario=` no es el del token responde 403 |
| 8 | El frontend no podía usar fetch contra el backend (CORS) | CORS habilitado para `localhost` en cualquier puerto (`app.cors.origenes`) |

## Corrección de defectos (entregable final)

Revisión del sistema integrado (backend + frontend) después del entregable 4.

| # | Defecto | Cómo se reprodujo | Corrección |
|---|---|---|---|
| 9 | **Escalada de privilegios en usuarios:** los permisos de ADMIN solo se revisaban en el frontend. Con su propio token, un alumno podía crear un ADMIN, darse rol ADMIN, cambiar la contraseña de otro usuario o eliminarlo | Con `curl` y el token de un alumno: las 4 operaciones respondieron 200/201 | `UsuarioController` toma el usuario del token: listar, crear, cambiar rol y eliminar solo ADMIN; editar datos y contraseña, ADMIN o el propio usuario; nadie cambia su propio rol ni elimina su cuenta. El rol se lee de la base (no del token) para que un cambio aplique de inmediato |
| 10 | **Cursos sin control de permisos:** `PUT`/`DELETE /cursos/{id}` y `DELETE /cursos/{id}/alumnos/{idAlumno}` no revisaban quién hacía la petición; `GET /cursos` sin `idUsuario` mostraba todos los cursos con sus códigos de invitación | Un alumno editó el curso y dio de baja a otro alumno (200/204) | Editar y eliminar: maestro del curso o ADMIN. Dar de baja: maestro, ADMIN o el propio alumno. Crear: solo a nombre propio (o ADMIN). `GET /cursos` sin `idUsuario` muestra solo los cursos del usuario; `GET /cursos/{id}` solo a miembros o ADMIN |
| 11 | Spring rechaza por defecto archivos mayores a 1 MB: las entregas con archivo fallaban con error 500 | Subir un archivo de más de 1 MB | Límite de 20 MB (`spring.servlet.multipart.*`) y respuesta `413` con mensaje claro si se excede |
| 12 | Las respuestas de error incluían el *stack trace* completo de Java | `GET /api/no-existe` regresaba el campo `trace` | `spring.web.error.include-stacktrace=never` (nombre de la propiedad en Spring Boot 4) y `ManejoErrores` para JSON mal formado, parámetros faltantes o inválidos (`400` con mensaje) |
| 13 | `PATCH /respuestas/{id}` aceptaba puntajes negativos o mayores que el valor de la pregunta, lo que alteraba la calificación final | Puntaje 50 en una pregunta de 5 puntos | Se rechaza si el puntaje no está entre 0 y el valor de la pregunta |
| 14 | El campo de calificación (y los de puntaje) no aceptaba decimales: el navegador pedía enteros aunque la base guarda 2 decimales | Calificar con 9.5 en la página de la tarea | `step="0.01"` en `tarea-detalle.html`, `calificar-examen.js` y `crear-examen.js` |
| 15 | La carga masiva de usuarios creaba en silencio las cuentas sin contraseña con la contraseña `"demo"` | Archivo CSV con la columna de contraseña vacía | La fila se reporta como error ("falta la contraseña"); también se exige mínimo 6 caracteres, como al editar |
| 16 | La página del curso pedía "mi resultado" de cada examen aunque entrara un maestro: 404 en consola por cada examen | Abrir el detalle de un curso como maestro | `Store.resultadoDeExamen` solo consulta si el usuario es ALUMNO |

### Defectos reportados por el equipo

| # | Defecto | Reportó | Corrección |
|---|---|---|---|
| 17 | El alumno veía **"Calificación final: 0 / 100"** aunque todas sus respuestas tuvieran puntaje completo. La calificación guardada era correcta; el frontend calculaba el porcentaje con el total de puntos del examen, que obtenía de `GET /examenes/{id}/preguntas`, y esa ruta le responde 403 al alumno cuando el examen ya cerró → total 0 → 0 % | Edgar | `Store.resultadoDeExamen` toma el total de las preguntas que vienen en las respuestas del resultado (igual que ya lo hacía `responderExamen`) |
| 18 | Mientras el examen seguía abierto, `POST /examenes/{id}/resultados`, `/resultados/mio` y `/resultados/{id}` le mandaban al alumno la **respuesta correcta** de cada pregunta: quien terminaba primero podía pasarlas | Revisión del defecto 17 | Hasta que el examen cierra, el alumno recibe las preguntas sin `respuestaCorrecta`; el maestro siempre la ve |
| 19 | 17 métodos PUT/PATCH/DELETE (tareas, exámenes, preguntas, foros, comentarios, equipos, prórrogas, notificaciones, respuestas, entregas) regresaban **200** con `null` o un texto aunque el registro no existiera o el usuario no tuviera permiso | Kendar (P-22 a P-26) | Regresan `ResponseEntity`: **404** si no existe, **403** sin permiso, **400** con datos inválidos (con mensaje). Los textos de éxito ("Tarea eliminada", "Prorroga aprobada"…) no cambiaron, así que el frontend sigue funcionando igual |
| 20 | `DELETE /cursos/{id}` daba **500** si el curso tenía tareas, foros o exámenes: las tablas que crea Hibernate no tienen `ON DELETE CASCADE`. Lo mismo al borrar tareas, exámenes, foros, preguntas o equipos con datos | Kendar (P-25) | `cascade = CascadeType.REMOVE` en las relaciones padre → hijo (curso, tarea, examen, foro, resultado, pregunta, equipo): Hibernate borra primero los hijos. Funciona también en bases ya creadas porque no depende de cambiar las llaves en MySQL |
| 21 | Eliminar un usuario con información relacionada (un maestro con cursos, un alumno con entregas) daba **500** | Revisión del defecto 20 | Responde **409** con un mensaje claro. No se borra en cascada a propósito: borrar a un maestro no debe eliminar en silencio todos sus cursos |

Verificado con `pruebas-api.sh`: 392 casos, 0 fallos (incluye cada defecto). El defecto 17 se reprodujo con el mismo examen de la captura (2 preguntas de 10 puntos) y ahora muestra 100 / 100.

## Pendientes (requieren decisión del equipo)

- **Prórrogas:** ahora una solicitud pendiente queda con `aprobada` vacía y una rechazada con `false`, pero el filtro `?aprobada=false` devuelve ambas.
- **Archivos subidos:** `/uploads/**` se sirve sin token; cualquiera con el enlace puede descargar el archivo.
