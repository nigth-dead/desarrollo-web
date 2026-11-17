# Reporte de Avance del Proyecto

**Equipo:** 2
**Fecha:** 16/11/2026
**Número de reporte:** 3
**Repositorio GitHub:** https://github.com/nigth-dead/desarrollo-web.git 

---

# 1. Actividades realizadas

Describir las actividades desarrolladas desde el último reporte.

En esta tercera entrega se implementaron los endpoints PUT y DELETE de los recursos del sistema, así como los endpoints GET y POST que faltaban (autenticación, inscripción a cursos, tareas, equipos, entregas, prórrogas, foros, comentarios, exámenes, preguntas, resultados y notificaciones). Se incorporaron filtros de búsqueda mediante parámetros en la URL en los endpoints que devuelven listas y se atendieron las observaciones detectadas en la revisión del código: se ocultó la contraseña en las respuestas, se corrigieron las fechas que se guardaban vacías, se ajustaron los endpoints PATCH conforme a la especificación (aunque no se vio en clase, se decidió en equipo implementarlo para lograr una arquitectura moderna y limpia) , se agregó la calificación de entregas, se unificaron repositorios duplicados y se implementó el recálculo de la calificación final de los exámenes. Además, se realizaron pruebas de persistencia y validación de respuestas HTTP, y se documentaron las pruebas con curl.

---

# 2. Evidencias de trabajo

| Actividad | Integrante responsable | Enlace a commit |
|------------|------------------------|-----------------|
|Implementar PUT y DELETE|Edgar Alejandro Cortes Martinez|https://github.com/nigth-dead/desarrollo-web/commit/bc08c755edb15bb6f3c26305e16a3ef998932f02|
|Probar persistencia|Kendar Emmanuel Strop Garcia|[enlace al commit]|
|Validar respuestas HTTP|Héctor Manuel Toral Huerta|[enlace al commit]|
|Incorporar filtros|Luis David Sosa Fernández|https://github.com/nigth-dead/desarrollo-web/commit/d19afdf96c0922eda5a65715fb6151a53eb11de9|
|Implementar observaciones|Luis David Sosa Fernández|https://github.com/nigth-dead/desarrollo-web/commit/d19afdf96c0922eda5a65715fb6151a53eb11de9|

---

# 3. Problemas encontrados

Describir los problemas técnicos encontrados y cómo fueron solucionados.

- **Contraseña expuesta en las respuestas:** al consultar cursos, la respuesta incluía el objeto del maestro con su `passwordHash`. Se solucionó con `@JsonIgnore` en la entidad `Usuario`.
- **Fechas guardadas en NULL:** las fechas automáticas (registro, inscripción, creación de foro, comentario, envío de notificación y realización de examen) quedaban vacías, porque las entidades esperaban un valor por defecto de MySQL que Hibernate no define al crear las tablas. Se solucionó con `@CreationTimestamp`.
- **Diferencias con la especificación:** 6 endpoints que la especificación define como PATCH estaban implementados como PUT, y no existía el endpoint para calificar entregas. Se cambiaron a `@PatchMapping` y se implementó `PATCH /entregas/{id}/calificacion`.
- **Repositorios duplicados:** existían dos repositorios para las tablas de cursos y usuarios. Se dejaron solo `ICurso` e `IUsuario`.
- **Examen sin calificación final:** al calificar manualmente las respuestas abiertas, el resultado se quedaba en `PENDIENTE_MANUAL`. Ahora se recalcula y pasa a `CALIFICADO` cuando todas las respuestas tienen puntaje.

---

# 4. Avance funcional

Indicar qué funcionalidades ya se encuentran operativas y cuáles están pendientes.

**Operativas:**
- Registro e inicio de sesión de usuarios.
- Creación de cursos e inscripción de alumnos con código de invitación.
- Tareas individuales y por equipo, entregas (con control de fecha límite y prórrogas) y calificación de entregas.
- Foros y comentarios.
- Exámenes con preguntas de opción múltiple (calificación automática) y abiertas (calificación manual).
- Notificaciones (nueva tarea y entrega calificada).
- Filtros de búsqueda en los endpoints que devuelven listas.
- Operaciones GET, POST, PUT, PATCH y DELETE de todos los recursos de la especificación, verificadas con 289 pruebas mediante curl (`documentation/API_documentation/pruebas-api.sh`).

**Pendientes:**
- Autenticación con token (por ahora el usuario se identifica con el parámetro `idUsuario`).
- Distinguir prórrogas pendientes de rechazadas.
- Validar que el puntaje de una respuesta abierta no supere el valor de la pregunta.
- Frontend.

---

# 5. Uso de IA generativa

Documentar el uso de herramientas de inteligencia artificial durante el desarrollo del proyecto.

- **Herramienta utilizada:** Claude Opus 5.5 (Anthropic)
- **Actividad para la que se utilizó:** Identificación y Corrección de las observaciones del código y elaboración de las pruebas con curl.
- **Código o contenido generado:** `pruebas-curl.md`, `pruebas-api.sh` y `observaciones.md`.
- **Modificaciones realizadas por el equipo: Ninguna
- **Forma en que se verificó el resultado:** se compiló el proyecto y se ejecutó el backend contra MySQL con 289 pruebas mediante curl (casos correctos y de error), todas exitosas. También se consultaron las tablas en MySQL para confirmar que los datos y las fechas se guardan correctamente.

> Si no se utilizó IA, indicarlo expresamente.

---

# 6. Participación individual

Describir brevemente la contribución de cada integrante.

- **Integrante 1:** Edgar Alejandro Cortes Martinez - Implementación de los métodos PUT y DELETE de los recursos del sistema.
- **Integrante 2:** Kendar Emmanuel Strop Garcia - Pruebas de persistencia de los datos en MySQL. [completar]
- **Integrante 3:** Luis David Sosa Fernández - Implementación de los GET y POST faltantes, incorporación de filtros de búsqueda, atención de las observaciones del código y documentación de las pruebas con curl.
- **Integrante 4:** Héctor Manuel Toral Huerta - Validación de las respuestas HTTP de la API. [completar]

---

# 7. Próximas actividades

Indicar las tareas que se realizarán antes del siguiente reporte.

- Actividad 1 Luis David Sosa Fernández: Implementar Funciones Fetch +  Extra Token Auth
- Actividad 2 [Integrante]:
- Actividad 3 [Integrante]: 
- Actividad 4 [Integrante]:
- Actividad 5 [Integrante]: 

---

## Observaciones adicionales

Agregar cualquier comentario, acuerdo, decisión técnica o evidencia complementaria relevante para el proyecto.

- Las observaciones atendidas y las pendientes están documentadas en `documentation/API_documentation/observaciones.md`.
- Las pruebas con curl están en `documentation/API_documentation/pruebas-curl.md` (ejemplos) y `pruebas-api.sh` (batería completa).
- Mientras no exista autenticación con token, el usuario que hace cada petición se indica con el parámetro `?idUsuario=`.