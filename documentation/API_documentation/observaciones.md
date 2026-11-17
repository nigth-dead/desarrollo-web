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

Verificado con `pruebas-api.sh`: 289 casos, 0 fallos.

## Pendientes (requieren decisión del equipo)

- **Respuestas HTTP de PUT/DELETE:** varios regresan `null` o un texto con `200` cuando hay error (no existe, sin permiso). Corresponde a la actividad "Validar respuestas HTTP".
- **Prórrogas:** `aprobada` es booleano, no distingue *pendiente* de *rechazada*. Se propone un campo `estado` (PENDIENTE / APROBADA / RECHAZADA).
- **Puntaje de respuestas abiertas:** `PATCH /respuestas/{id}` no valida que el puntaje no supere el valor de la pregunta.
- **`SCRIPT-BASE.sql`:** ya no crea las tablas (las genera Hibernate con `ddl-auto=update`); conviene documentarlo o volver a incluirlas.
- **Login:** todavía no hay token; el usuario se identifica con `?idUsuario=`.
