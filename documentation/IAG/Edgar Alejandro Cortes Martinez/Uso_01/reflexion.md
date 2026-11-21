# Reflexión del estudiante — Uso_01

En este uso la IA generó todo el CSS, porque los elementos se mostraban sin CSS. Solo dio estilo al código, no cambió lógica.

Le pedí que le diera diseño a todo el frontend y generó los 10 archivos de `frontend/CSS/` completos, 972 líneas: el `main.css` con las variables, el encabezado, las tarjetas, los botones, los formularios, los diálogos y las tablas, y un CSS por vista para login, registro, cursos, curso-detalle, tarea-detalle, crear-examen, calificar-examen, responder-examen y usuarios.

Yo revisé cada vista en el navegador y comprobé que ya se ven con diseño y que nada se rompió: los botones siguen haciendo lo mismo, los formularios y los modales funcionan igual, solo que ahora sí se ven con colores, espaciado y tarjetas.

Lo que aprendí fue que con variables en `:root` y un `main.css` común se puede dar estilo a todo el sistema sin tocar HTML ni JavaScript, y que cada vista solo necesita su propio CSS pequeño para lo que es distinto en esa vista.
