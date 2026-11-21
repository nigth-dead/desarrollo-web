# Reporte de IA Desarrollo en el Proyecto

**Fecha:** 20 de noviembre de 2026
**Número de reporte:** 1

---

# 1. Breve descripción de uso

Se le pidió a la IA únicamente darle más diseño con CSS al sistema, sin cambiar nada de lógica.

Alcance limitado solo a diseño visual en el frontend:
- Solo archivos en `frontend/CSS/`: `main.css`, `login.css`, `registro.css`, `cursos.css`, `curso-detalle.css`, `tarea-detalle.css`, `crear-examen.css`, `calificar-examen.css`, `responder-examen.css` y `usuarios.css`.
- No se pidió ni se hizo ningún cambio en JavaScript, HTML estructural, backend ni base de datos.
- No se pidieron validaciones, mensajes de error, ni cambios de funcionalidad.

---

# 2. Evidencias de uso

| Evidencia | Formato |
|------------|------------------------|
| Herramienta | Muse (Meta) |
| Modelo/temperatura | Muse Spark 1.3 |
| Prompt original | "Dale más diseño con CSS al sistema, sin cambiar nada de lógica" |
| Respuesta de IA | Propuesta de mejoras solo con CSS: variables de color en `main.css`, espaciado y bordes redondeados consistentes en tarjetas y formularios, sombras suaves, estados hover y focus en botones, consistencia en modales, tablas y encabezados, y ajustes responsive para que no se rompa el layout en pantallas chicas. |
| Código anterior | Commit `0940ffe565e43288b2e18e135c7c7bc2c8795a59` |
| Código posterior | https://github.com/nigth-dead/desarrollo-web/commit/0940ffe565e43288b2e18e135c7c7bc2c8795a59 |
| Diferencias entre versiones | Solo reglas de estilo en `frontend/CSS/*.css`. No hay cambios en `frontend/js/`, `frontend/HTML/`, `backend/` ni `database/`. |
| Prueba de funcionamiento | Revisión visual en el navegador: abrir login, cursos, curso-detalle y tarea-detalle, verificar que botones, modales y formularios se ven consistentes y siguen funcionando igual, sin errores de layout. Captura de antes y después. |
| Código de uso | Diseño |
| Reflexión del estudiante | La IA ayudó a unificar el diseño y a proponer variables y espaciados consistentes, pero el equipo revisó cada cambio en el navegador para que no se rompiera ninguna vista y para mantener los colores y la estructura que ya tenía el sistema. |

> El prompt y la respuesta pueden ser copiados como una subsección y enlazados

---

## Observaciones adicionales

- Este reporte cubre únicamente el uso de IA para diseño con CSS. No se usó IA en este reporte para lógica, validaciones, backend, base de datos ni documentación..
