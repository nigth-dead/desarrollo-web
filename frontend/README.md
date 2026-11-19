# Frontend del LMS

## Cómo abrirlo

1. Levantar el backend (`http://localhost:8081`).
2. Servir esta carpeta con un servidor local. No abrir los HTML con doble clic (`file://`): el navegador bloquea el fetch.
   - VS Code: extensión **Live Server** → clic derecho en `index.html` → *Open with Live Server*.
   - O desde una terminal en esta carpeta:
     ```bash
     python -m http.server 5500
     ```
     y abrir `http://localhost:5500`.
3. Iniciar sesión en `login.html` con un usuario registrado.

El backend acepta peticiones de `localhost` y `127.0.0.1` en cualquier puerto (`app.cors.origenes` en `application.properties`).

## Archivos

| Archivo | Qué hace |
|---|---|
| `js/api.js` | Funciones fetch para **todos** los endpoints, manejo del token y de errores |
| `js/tablas.js` | Llena las tablas de `tablas/` a partir de `data-endpoint` y `data-campo`, y muestra la barra de sesión |
| `login.html` | Inicio de sesión; guarda el token en el navegador |
| `tablas/*.html` | Una tabla por recurso |

## Usar `api.js` (formularios, mensajes, etc.)

Incluir el script y llamar las funciones de `api`. Todas regresan una Promise:

```html
<script src="../js/api.js"></script>
<script>
  async function crearTarea() {
    try {
      const tarea = await api.tareas.crear(1, {
        titulo: 'Practica 1',
        fechaLimite: '2026-12-01T23:59:00',
        esEquipo: false,
      });
      // confirmación: tarea creada
    } catch (e) {
      // e.status = código HTTP (400, 403, 404, 409...; 0 si no hay conexión)
      // e.mensaje = texto que regresó el servidor
    }
  }
</script>
```

- El token se manda solo en `Authorization: Bearer <token>`.
- El `idUsuario` de la sesión se agrega solo a la URL.
- Grupos disponibles: `api.auth`, `api.usuarios`, `api.cursos`, `api.tareas`, `api.equipos`, `api.entregas`, `api.prorrogas`, `api.foros`, `api.comentarios`, `api.examenes`, `api.preguntas`, `api.resultados`, `api.notificaciones`.
- Sesión: `api.sesion.usuario()` regresa `{ idUsuario, nombre, email, rol }`; `api.auth.logout()` la cierra.

## Tablas

Los `{parámetros}` del endpoint y los filtros se pasan en la URL de la página:

```
tablas/tareas.html?idCurso=1
tablas/tareas.html?idCurso=1&estado=pendiente
tablas/resultados.html?idExamen=1&id=3
```

Si falta un parámetro, la página muestra un campo para escribirlo.
