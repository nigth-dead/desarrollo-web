(async function () {
    const params = new URLSearchParams(window.location.search);
    const idExamen = Number(params.get("examen")) || 1;

    const usuario = Store.usuarioActual();
    if (!usuario) return;

    const examen = await Store.examenPorId(idExamen);
    const contenido = document.getElementById("contenidoCalificar");
    const accesoDenegado = document.getElementById("accesoDenegado");

    if (!examen) {
        contenido.hidden = true;
        accesoDenegado.hidden = false;
        accesoDenegado.innerHTML = `
            <div class="apartado">
                <h2>Examen no encontrado</h2>
                <p>No existe un examen con ese identificador.</p>
                <div class="acciones-form">
                    <a class="btn btn-primario" href="cursos.html">Volver a Mis cursos</a>
                </div>
            </div>`;
        return;
    }

    const curso = await Store.cursoPorId(examen.id_curso);
    document.getElementById("enlaceVolver").href = "curso-detalle.html?id=" + examen.id_curso;
    document.getElementById("enlaceVolver").textContent = "← Volver al curso";

    if (!Store.esMaestro()) {
        contenido.hidden = true;
        accesoDenegado.hidden = false;
        accesoDenegado.innerHTML = `
            <div class="apartado">
                <h2>Solo el maestro puede calificar este examen</h2>
                <p>Tu cuenta de alumno no tiene permiso para revisar las respuestas de este examen.</p>
                <div class="acciones-form">
                    <a class="btn btn-primario" href="curso-detalle.html?id=${examen.id_curso}">Volver al curso</a>
                </div>
            </div>`;
        return;
    }

    document.getElementById("nombreExamen").textContent = examen.titulo;
    document.getElementById("nombreCurso").textContent = curso ? curso.nombre : "";

    const listaResultados = document.getElementById("listaResultados");
    const mensajeVacio = document.getElementById("mensajeVacio");
    const contadorPendientes = document.getElementById("contadorPendientes");
    const contadorCalificadas = document.getElementById("contadorCalificadas");

    let filtroActual = "TODAS";
    let resultados = [];
    let respuestas = [];
    const expandidos = new Set();

    async function cargarDatos() {
        resultados = await Store.resultadosDeExamen(idExamen);
        respuestas = await Store.respuestasDeExamen(idExamen);
    }

    function badge(estado) {
        return estado === "CALIFICADO"
            ? `<span class="badge badge-verde">CALIFICADO</span>`
            : `<span class="badge badge-ambar">PENDIENTE_MANUAL</span>`;
    }

    function textoCalificacion(r) {
        return r.calificacion_final === null || r.calificacion_final === undefined
            ? "—"
            : r.calificacion_final + " / 100";
    }

    // Respuestas de un resultado
    function detalleRespuestas(r) {
        const delResultado = respuestas.filter((x) => x.id_resultado === r.id_resultado);
        const abiertas = delResultado.filter((x) => x.pregunta && x.pregunta.tipo === "ABIERTA");
        const multiples = delResultado.filter((x) => x.pregunta && x.pregunta.tipo !== "ABIERTA");
        if (abiertas.length === 0 && multiples.length === 0) {
            return `<p class="vacio">Este alumno aún no tiene respuestas en este examen.</p>`;
        }
        let html = "";
        if (abiertas.length > 0) {
            const filas = abiertas.map((x) => {
                const puntajeValor = x.puntaje_obtenido === null || x.puntaje_obtenido === undefined ? "" : x.puntaje_obtenido;
                const maxPuntos = x.pregunta ? x.pregunta.valor_puntos : "";
                return `
            <tr data-id="${x.id_respuesta}">
                <td class="celda-texto" data-campo="pregunta.pregunta">${Store.escaparHTML(x.pregunta ? x.pregunta.pregunta : "")}</td>
                <td class="celda-texto" data-campo="respuestaDada">${Store.escaparHTML(x.respuesta_dada)}</td>
                <td data-campo="puntajeObtenido"><input type="number" id="puntaje-${x.id_respuesta}" class="input-puntaje" min="0" max="${maxPuntos}" value="${puntajeValor}"> / ${maxPuntos}</td>
                <td data-campo="comentarioMaestro"><textarea id="comentario-${x.id_respuesta}" class="input-comentario" rows="2" placeholder="Comentario para el alumno...">${Store.escaparHTML(x.comentario_maestro || "")}</textarea></td>
                <td class="acciones">
                    <button type="button" class="btn btn-primario btn-sm" data-guardar-respuesta="${x.id_respuesta}">Guardar</button>
                </td>
            </tr>`;
            }).join("");
            html += `
            <p class="subtitulo-detalle">Respuestas abiertas</p>
            <div class="tabla-contenedor">
                <table class="tabla tabla-respuestas" data-endpoint="/api/examenes/${idExamen}/resultados">
                    <thead><tr><th data-campo="pregunta.pregunta">Pregunta</th><th data-campo="respuestaDada">Respuesta</th><th data-campo="puntajeObtenido">Puntaje</th><th data-campo="comentarioMaestro">Comentario</th><th class="acciones">Acciones</th></tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>`;
        } else {
            html += `<p class="vacio">Este alumno no tiene respuestas abiertas en este examen.</p>`;
        }
        if (multiples.length > 0) {
            const filas = multiples.map((x) => {
                const puntaje = x.puntaje_obtenido === null || x.puntaje_obtenido === undefined ? "—" : x.puntaje_obtenido;
                const maxPuntos = x.pregunta ? x.pregunta.valor_puntos : "";
                const correcta = x.pregunta ? (x.pregunta.respuesta_correcta || "—") : "—";
                return `
            <tr data-id="${x.id_respuesta}">
                <td class="celda-texto" data-campo="pregunta.pregunta">${Store.escaparHTML(x.pregunta ? x.pregunta.pregunta : "")}</td>
                <td class="celda-texto" data-campo="respuestaDada">${Store.escaparHTML(x.respuesta_dada)}</td>
                <td class="celda-texto" data-campo="respuestaCorrecta">${Store.escaparHTML(correcta)}</td>
                <td data-campo="puntajeObtenido">${puntaje} / ${maxPuntos}</td>
            </tr>`;
            }).join("");
            html += `
            <p class="subtitulo-detalle">Opción múltiple</p>
            <div class="tabla-contenedor">
                <table class="tabla tabla-respuestas" data-endpoint="/api/examenes/${idExamen}/resultados">
                    <thead><tr><th data-campo="pregunta.pregunta">Pregunta</th><th data-campo="respuestaDada">Respuesta</th><th data-campo="respuestaCorrecta">Correcta</th><th data-campo="puntajeObtenido">Puntaje</th></tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>`;
        }
        return html;
    }

    function filaResultado(r) {
        const expandido = expandidos.has(r.id_resultado);
        let html = `
        <tr data-id="${r.id_resultado}">
            <td data-campo="alumno.nombre">${Store.escaparHTML(r.alumno ? r.alumno.nombre : "Alumno")}</td>
            <td data-campo="calificacionFinal">${textoCalificacion(r)}</td>
            <td data-campo="estadoRevision">${badge(r.estado_revision)}</td>
            <td data-campo="fechaRealizacion">${r.fecha_realizacion ? Store.formatearFecha(r.fecha_realizacion) : "—"}</td>
            <td class="acciones">
                <button type="button" class="btn btn-secundario btn-sm" data-ver-respuestas="${r.id_resultado}">${expandido ? "Ocultar respuestas" : "Ver respuestas"}</button>
            </td>
        </tr>`;
        if (expandido) {
            html += `
        <tr class="fila-detalle">
            <td colspan="5">${detalleRespuestas(r)}</td>
        </tr>`;
        }
        return html;
    }

    async function renderizarResultados() {
        const filtrados = resultados.filter((r) => filtroActual === "TODAS" || r.estado_revision === filtroActual);
        if (filtrados.length === 0) {
            listaResultados.innerHTML = "";
        } else {
            const filas = filtrados.map(filaResultado).join("");
            listaResultados.innerHTML = `
                <div class="tabla-contenedor">
                    <table class="tabla" data-endpoint="/api/examenes/${idExamen}/resultados">
                        <thead><tr><th data-campo="alumno.nombre">Alumno</th><th data-campo="calificacionFinal">Calif. final</th><th data-campo="estadoRevision">Estado</th><th data-campo="fechaRealizacion">Fecha</th><th class="acciones">Acciones</th></tr></thead>
                        <tbody>${filas}</tbody>
                    </table>
                </div>`;
        }
        mensajeVacio.hidden = filtrados.length > 0;
        contadorPendientes.textContent = resultados.filter((r) => r.estado_revision === "PENDIENTE_MANUAL").length;
        contadorCalificadas.textContent = resultados.filter((r) => r.estado_revision === "CALIFICADO").length;
    }

    // Tabla de preguntas del examen
    async function renderizarPreguntas() {
        const contenedor = document.getElementById("tablaPreguntas");
        if (!contenedor) return;
        const preguntas = await Store.preguntasDeExamen(idExamen);
        if (preguntas.length === 0) {
            contenedor.innerHTML = `<p class="vacio">Este examen aún no tiene preguntas.</p>`;
            return;
        }
        const filas = preguntas.map((p) => {
            const opciones = p.tipo === "ABIERTA" ? "—" : ((p.opciones_json || p.opciones || []).map((o) => Store.escaparHTML(String(o))).join(", ") || "—");
            return `
            <tr>
                <td class="celda-texto" data-campo="pregunta">${Store.escaparHTML(p.pregunta)}</td>
                <td data-campo="tipo">${p.tipo === "ABIERTA" ? "Abierta" : "Opción múltiple"}</td>
                <td class="celda-texto" data-campo="opcionesJson">${opciones}</td>
                <td class="celda-texto" data-campo="respuestaCorrecta">${p.tipo === "ABIERTA" ? "—" : Store.escaparHTML(p.respuesta_correcta || "")}</td>
                <td data-campo="valorPuntos">${p.valor_puntos}</td>
            </tr>`;
        }).join("");
        contenedor.innerHTML = `
            <div class="tabla-contenedor">
                <table class="tabla" data-endpoint="/api/examenes/${idExamen}/preguntas">
                    <thead><tr><th data-campo="pregunta">Pregunta</th><th data-campo="tipo">Tipo</th><th data-campo="opcionesJson">Opciones</th><th data-campo="respuestaCorrecta">Respuesta correcta</th><th data-campo="valorPuntos">Puntos</th></tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>`;
    }

    document.querySelectorAll(".btn-filtro").forEach((btn) => {
        btn.addEventListener("click", async () => {
            document.querySelectorAll(".btn-filtro").forEach((b) => b.classList.remove("activo"));
            btn.classList.add("activo");
            filtroActual = btn.dataset.filtro;
            await renderizarResultados();
        });
    });

    listaResultados.addEventListener("click", async (evento) => {
        const botonVer = evento.target.closest("[data-ver-respuestas]");
        if (botonVer) {
            const id = Number(botonVer.dataset.verRespuestas);
            if (expandidos.has(id)) expandidos.delete(id);
            else expandidos.add(id);
            await renderizarResultados();
            return;
        }
        const boton = evento.target.closest("[data-guardar-respuesta]");
        if (!boton) return;
        const id = Number(boton.dataset.guardarRespuesta);
        const respuesta = respuestas.find((r) => r.id_respuesta === id);
        if (!respuesta) return;
        const puntajeInput = document.getElementById("puntaje-" + id);
        const comentarioInput = document.getElementById("comentario-" + id);
        const max = respuesta.pregunta ? Number(respuesta.pregunta.valor_puntos) : 0;
        const puntaje = Number(puntajeInput.value);

        if (puntajeInput.value === "" || Number.isNaN(puntaje) || puntaje < 0 || puntaje > max) {
            return;
        }

        await Store.calificarRespuesta(id, puntaje, comentarioInput.value.trim());
        await cargarDatos();
        await renderizarResultados();
    });

    await renderizarPreguntas();
    await cargarDatos();
    await renderizarResultados();
})();
