# Pruebas con curl — GET y POST

Servidor: `http://localhost:8081/api`
Ejecutar en Git Bash. En PowerShell usar `curl.exe` en lugar de `curl`.

> **Token:** todas las rutas, menos registro y login, piden el header `Authorization: Bearer <token>`.
> El token se obtiene con el login (dura 8 horas). Si la URL trae `?idUsuario=`, debe ser el mismo
> usuario del token; si no, la API responde 403. Sin token o con token inválido responde 401.
> Los ids de los ejemplos corresponden a una base vacía: 1 = maestro, 2 y 3 = alumnos.

```bash
# Iniciar sesión y guardar el token en una variable (Git Bash)
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"email":"juan@uv.mx","password":"123456"}' | python -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Sin token (401) / token de otro usuario (403)
curl -i http://localhost:8081/api/cursos
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos?idUsuario=2"
```

Cada ejemplo usa el token del usuario que aparece en `?idUsuario=`: para los de alumno, inicia sesión con `ana@uv.mx` y guarda ese token.

La batería completa (304 casos, incluidos los de error, los filtros, los PATCH y el token) está en `pruebas-api.sh`:
```bash
bash documentation/API_documentation/pruebas-api.sh
```
Se debe correr con la base vacía porque registra usuarios fijos.

---

## Auth

```bash
# Registrarse (201). ADMIN no se permite (400), email repetido (409)
curl -i -X POST http://localhost:8081/api/auth/registro -H "Content-Type: application/json" -d '{"nombre":"Profe Juan","email":"juan@uv.mx","password":"123456","rol":"MAESTRO"}'
curl -i -X POST http://localhost:8081/api/auth/registro -H "Content-Type: application/json" -d '{"nombre":"Ana","email":"ana@uv.mx","password":"123456","rol":"ALUMNO"}'
curl -i -X POST http://localhost:8081/api/auth/registro -H "Content-Type: application/json" -d '{"nombre":"Beto","email":"beto@uv.mx","password":"123456","rol":"ALUMNO"}'

# Iniciar sesión (200). Password incorrecta (401)
curl -i -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"email":"ana@uv.mx","password":"123456"}'

# Mis datos (200)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/auth/me?idUsuario=2"
```

## Usuarios

```bash
curl -i -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8081/api/usuarios -H "Content-Type: application/json" -d '{"nombre":"Admin","email":"admin@uv.mx","password":"123456","rol":"ADMIN"}'
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/usuarios
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/usuarios/1
```

## Cursos

```bash
# Crear curso (201): devuelve el codigoInvitacion
curl -i -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8081/api/cursos -H "Content-Type: application/json" -d '{"nombre":"Desarrollo Web","descripcion":"501","idMaestro":1}'

# Ver todos / mis cursos / uno
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/cursos
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/cursos/1

# Unirse con código (201). Ya inscrito (409), código inválido (404), maestro (403)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/cursos/unirse?idUsuario=2" -H "Content-Type: application/json" -d '{"codigoInvitacion":"ABC123"}'

# Generar nuevo código (200, solo el maestro)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/cursos/1/codigo?idUsuario=1"

# Ver alumnos inscritos (200, miembros del curso)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/alumnos?idUsuario=2"
```

## Tareas

```bash
# Crear tarea (201, solo el maestro). Avisa con una notificación a los alumnos
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/cursos/1/tareas?idUsuario=1" -H "Content-Type: application/json" -d '{"titulo":"Practica 1","descripcion":"Formulario","fechaLimite":"2026-12-01T23:59:00","esEquipo":false}'
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/cursos/1/tareas?idUsuario=1" -H "Content-Type: application/json" -d '{"titulo":"Proyecto","fechaLimite":"2026-12-10T23:59:00","esEquipo":true}'

# Ver tareas del curso (el alumno no ve las de fechaProgramada futura) / una tarea
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/tareas?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1?idUsuario=2"
```

## Equipos

```bash
# Crear equipo (201, solo en tareas por equipo)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/tareas/2/equipos?idUsuario=1" -H "Content-Type: application/json" -d '{"nombreEquipo":"Equipo Rojo"}'

# Agregar integrantes (201). Alumno en otro equipo de la tarea (409), no inscrito (400)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/equipos/1/integrantes?idUsuario=1" -H "Content-Type: application/json" -d '{"idsAlumnos":[2,3]}'

# Ver equipos de la tarea / un equipo
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/2/equipos?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/equipos/1?idUsuario=2"
```

## Entregas

```bash
# Entregar (201). Repetida (409), fecha límite vencida sin prórroga aprobada (400)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/tareas/1/entregas?idUsuario=2" -H "Content-Type: application/json" -d '{"archivoUrl":"https://drive.google.com/archivo-ana"}'
# En tarea por equipo se guarda a nombre del equipo
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/tareas/2/entregas?idUsuario=3" -H "Content-Type: application/json" -d '{"archivoUrl":"https://drive.google.com/proyecto"}'

# Mi entrega / todas (maestro) / una / descargar archivo (302 a la URL)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1/entregas/mia?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1/entregas?idUsuario=1"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/entregas/1?idUsuario=1"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/entregas/1/archivo?idUsuario=2"
```

## Prórrogas

```bash
# Alumno solicita (201, queda aprobada=false)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/tareas/1/prorrogas?idUsuario=2" -H "Content-Type: application/json" -d '{"nuevaFechaLimite":"2026-12-05T23:59:00"}'
# Maestro otorga directo (201, aprobada=true). En tarea por equipo se manda idEquipo
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/tareas/1/prorrogas?idUsuario=1" -H "Content-Type: application/json" -d '{"nuevaFechaLimite":"2026-12-05T23:59:00","idAlumno":2}'

# Ver solicitudes (maestro)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1/prorrogas?idUsuario=1"
```

## Foros y comentarios

```bash
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/cursos/1/foros?idUsuario=1" -H "Content-Type: application/json" -d '{"titulo":"Dudas","descripcion":"Pregunten aqui"}'
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/foros?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/foros/1?idUsuario=2"

curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/foros/1/comentarios?idUsuario=2" -H "Content-Type: application/json" -d '{"contenido":"Se entrega en zip?"}'
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/foros/1/comentarios?idUsuario=2"
```

## Exámenes y preguntas

```bash
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/cursos/1/examenes?idUsuario=1" -H "Content-Type: application/json" -d '{"titulo":"Parcial 1","fechaInicio":"2026-11-16T08:00:00","fechaFin":"2026-12-31T23:59:00"}'
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/examenes?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1?idUsuario=2"

# Preguntas (201). Respuesta que no está en las opciones (400)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/examenes/1/preguntas?idUsuario=1" -H "Content-Type: application/json" -d '{"pregunta":"Metodo para crear?","tipo":"OPCION_MULTIPLE","opciones":["GET","POST","PUT"],"respuestaCorrecta":"POST","valorPuntos":2}'
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/examenes/1/preguntas?idUsuario=1" -H "Content-Type: application/json" -d '{"pregunta":"Que es REST?","tipo":"ABIERTA","valorPuntos":5}'

# El maestro ve la respuesta correcta; el alumno no, y solo durante el examen
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/preguntas?idUsuario=1"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/preguntas?idUsuario=2"
```

## Resultados

```bash
# Enviar examen (201). Opción múltiple se califica sola; con abiertas queda PENDIENTE_MANUAL. Repetido (409)
curl -i -H "Authorization: Bearer $TOKEN" -X POST "http://localhost:8081/api/examenes/1/resultados?idUsuario=2" -H "Content-Type: application/json" -d '{"respuestas":[{"idPregunta":1,"respuestaDada":"POST"},{"idPregunta":2,"respuestaDada":"Un estilo de arquitectura"}]}'

curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/resultados?idUsuario=1"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/resultados/mio?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/resultados/1?idUsuario=2"
```

## Notificaciones

```bash
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/notificaciones?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/notificaciones?idUsuario=2&leido=false"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/notificaciones/no-leidas/total?idUsuario=2"
```

---

## PATCH (cambios parciales)

```bash
# Cambiar contraseña / rol
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH http://localhost:8081/api/usuarios/3/password -H "Content-Type: application/json" -d '{"password":"nueva123"}'
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH http://localhost:8081/api/usuarios/3/rol -H "Content-Type: application/json" -d '{"rol":"MAESTRO"}'

# Calificar entrega (200, solo el maestro). Fuera de 0-100 (400). Avisa al alumno o al equipo
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH "http://localhost:8081/api/entregas/1/calificacion?idUsuario=1" -H "Content-Type: application/json" -d '{"calificacion":9.5,"comentarioMaestro":"Bien"}'

# Aprobar o rechazar prórroga
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH "http://localhost:8081/api/prorrogas/1?idUsuario=1" -H "Content-Type: application/json" -d '{"aprobada":true,"nuevaFechaLimite":"2026-12-05T23:59:00"}'

# Calificar respuesta abierta. Cuando ya están todas, el resultado pasa a CALIFICADO
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH "http://localhost:8081/api/respuestas/2?idUsuario=1" -H "Content-Type: application/json" -d '{"puntajeObtenido":4,"comentarioMaestro":"Incompleta"}'

# Marcar notificación como leída / todas
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH "http://localhost:8081/api/notificaciones/1/leida?idUsuario=2"
curl -i -H "Authorization: Bearer $TOKEN" -X PATCH "http://localhost:8081/api/notificaciones/leidas?idUsuario=2"
```

---

## Filtros

Los GET que devuelven listas aceptan filtros opcionales como parámetros en la URL. Se pueden combinar entre sí.
La búsqueda `q` no distingue mayúsculas ni acentos ("practica" encuentra "Práctica").
Un valor no permitido en `estado`, `rol` o `tipo` regresa `400`.

| Endpoint | Filtros |
|---|---|
| `GET /usuarios` | `rol` (MAESTRO, ALUMNO, ADMIN), `q` (nombre o email) |
| `GET /cursos` | `q` (nombre o descripción), `idMaestro` |
| `GET /cursos/{id}/alumnos` | `q` (nombre o email) |
| `GET /cursos/{id}/tareas` | `q` (título o descripción), `estado` (pendiente, vencida), `esEquipo` (true, false). Ordenadas por fecha límite |
| `GET /tareas/{id}/entregas` | `calificada` (true, false), `tarde` (true, false) |
| `GET /tareas/{id}/prorrogas` | `aprobada` (true, false) |
| `GET /cursos/{id}/foros` | `q` (título o descripción) |
| `GET /foros/{id}/comentarios` | `idAutor`, `q` (texto) |
| `GET /cursos/{id}/examenes` | `estado` (proximo, disponible, finalizado), `q` (título). Ordenados por fecha de inicio |
| `GET /examenes/{id}/preguntas` | `tipo` (OPCION_MULTIPLE, ABIERTA) |
| `GET /examenes/{id}/resultados` | `estado` (PENDIENTE_MANUAL, CALIFICADO), `minCalificacion`, `maxCalificacion` |
| `GET /notificaciones` | `leido` (true, false) |

```bash
# Usuarios con rol ALUMNO / buscar por nombre o email
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/usuarios?rol=ALUMNO"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/usuarios?q=ana"

# Buscar cursos
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos?q=web"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos?idUsuario=2&q=web"

# Tareas pendientes / vencidas / por equipo / búsqueda
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/tareas?idUsuario=2&estado=pendiente"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/tareas?idUsuario=1&estado=vencida"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/tareas?idUsuario=1&esEquipo=true"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/tareas?idUsuario=1&q=practica"

# Entregas sin calificar / entregadas tarde
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1/entregas?idUsuario=1&calificada=false"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1/entregas?idUsuario=1&tarde=true"

# Prórrogas pendientes de revisar
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/tareas/1/prorrogas?idUsuario=1&aprobada=false"

# Foros y comentarios
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/foros?idUsuario=2&q=dudas"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/foros/1/comentarios?idUsuario=2&idAutor=1"

# Exámenes disponibles ahora / preguntas abiertas / resultados
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/examenes?idUsuario=2&estado=disponible"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/preguntas?idUsuario=1&tipo=ABIERTA"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/resultados?idUsuario=1&estado=PENDIENTE_MANUAL"
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/examenes/1/resultados?idUsuario=1&minCalificacion=6"

# Filtro inválido (400)
curl -i -H "Authorization: Bearer $TOKEN" "http://localhost:8081/api/cursos/1/tareas?idUsuario=1&estado=xx"
```
