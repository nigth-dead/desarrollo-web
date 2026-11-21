(async function () {
    const params = new URLSearchParams(window.location.search);
    const idExamen = Number(params.get("examen")) || 1;

    const usuario = Store.usuarioActual();
    if (!usuario) return;

    const examen = await Store.examenPorId(idExamen);
    const encabezado = document.getElementById("encabezadoExamen");
    const form = document.getElementById("formResponder");
    const resultadoDiv = document.getElementById("resultadoExamen");
    const progreso = document.getElementById("progreso");
    const tiempoRestante = document.getElementById("tiempoRestante");

    if (!examen) {
        encabezado.hidden = true;
        form.hidden = true;
        resultadoDiv.hidden = false;
        resultadoDiv.innerHTML = `
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
    document.getElementById("tituloExamen").textContent = examen.titulo;
    document.getElementById("nombreCurso").textContent = curso ? curso.nombre : "";
    document.getElementById("fechaLimite").textContent = Store.formatearFecha(examen.fecha_fin);
    document.getElementById("enlaceVolver").href = "curso-detalle.html?id=" + examen.id_curso;
    document.getElementById("enlaceVolver").textContent = "← Volver al curso";

    // Si ya respondio no se muestra el formulario
    const resultadoPrevio = await Store.resultadoDeExamen(idExamen, usuario.id_usuario);
    if (resultadoPrevio) {
        encabezado.hidden = true;
        form.hidden = true;
        resultadoDiv.hidden = false;
        const cerrado = await Store.examenCerrado(examen);
        let detalle;
        if (resultadoPrevio.estado_revision === "CALIFICADO" && cerrado) {
            detalle = `<p><strong>Calificación final:</strong> ${resultadoPrevio.calificacion_final} / 100</p>`;
        } else if (resultadoPrevio.estado_revision === "CALIFICADO") {
            detalle = `<p><strong>Estado:</strong> <span class="badge badge-azul">Calificado</span></p>
            <p>Tu calificación se mostrará al cerrar el examen, el ${Store.formatearFecha(examen.fecha_fin)}.</p>`;
        } else if (resultadoPrevio.estado_revision === "PENDIENTE_CIERRE") {
            detalle = `<p><strong>Estado:</strong> <span class="badge badge-azul">En espera del cierre</span></p>
            <p>Tu examen se calificará automáticamente al cerrar, el ${Store.formatearFecha(examen.fecha_fin)}.</p>`;
        } else {
            detalle = `<p><strong>Estado:</strong> <span class="badge badge-ambar">PENDIENTE_MANUAL</span></p>
            <p>Tus respuestas abiertas están pendientes de revisión manual por el maestro.</p>`;
        }
        let revision = "";
        if (cerrado) {
            const respuestas = await Store.respuestasDeResultado(resultadoPrevio.id_resultado);
            if (respuestas.length > 0) {
                const filas = respuestas.map((r) => {
                    const esAbierta = r.pregunta && r.pregunta.tipo === "ABIERTA";
                    const correcta = esAbierta ? "—" : (r.pregunta ? (r.pregunta.respuesta_correcta || "—") : "—");
                    const maxPuntos = r.pregunta ? r.pregunta.valor_puntos : "";
                    const puntaje = r.puntaje_obtenido === null || r.puntaje_obtenido === undefined
                        ? `<span class="badge badge-ambar">Pendiente</span>`
                        : r.puntaje_obtenido + " / " + maxPuntos;
                    return `
            <tr>
                <td class="celda-texto" data-campo="pregunta.pregunta">${Store.escaparHTML(r.pregunta ? r.pregunta.pregunta : "")}</td>
                <td class="celda-texto" data-campo="respuestaDada">${Store.escaparHTML(r.respuesta_dada)}</td>
                <td class="celda-texto" data-campo="respuestaCorrecta">${Store.escaparHTML(correcta)}</td>
                <td data-campo="puntajeObtenido">${puntaje}</td>
                <td class="celda-texto" data-campo="comentarioMaestro">${r.comentario_maestro ? Store.escaparHTML(r.comentario_maestro) : "—"}</td>
            </tr>`;
                }).join("");
                revision = `
            <h3>Revisión de tus respuestas</h3>
            <div class="tabla-contenedor">
                <table class="tabla" data-endpoint="/api/resultados/${resultadoPrevio.id_resultado}">
                    <thead><tr><th data-campo="pregunta.pregunta">Pregunta</th><th data-campo="respuestaDada">Tu respuesta</th><th data-campo="respuestaCorrecta">Respuesta correcta</th><th data-campo="puntajeObtenido">Puntaje</th><th data-campo="comentarioMaestro">Comentario</th></tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>`;
            }
        }
        resultadoDiv.innerHTML = `
            <div class="apartado">
                <h2>Ya respondiste este examen</h2>
                ${detalle}
                <p class="texto-ayuda">Respondido el ${Store.formatearFecha(resultadoPrevio.fecha_realizacion)}.</p>
                ${revision}
                <div class="acciones-form">
                    <a class="btn btn-primario" href="curso-detalle.html?id=${examen.id_curso}">Volver al curso</a>
                </div>
            </div>`;
        return;
    }

    const preguntas = await Store.preguntasDeExamen(idExamen);
    const contenedor = document.getElementById("contenedorPreguntas");

    const ahora = Date.now();
    const inicioMs = new Date(examen.fecha_inicio).getTime();
    const finMs = new Date(examen.fecha_fin).getTime();
    if (!Number.isNaN(finMs) && ahora > finMs) {
        encabezado.hidden = true;
        form.hidden = true;
        resultadoDiv.hidden = false;
        resultadoDiv.innerHTML = `
            <div class="apartado">
                <h2>Este examen ya cerró</h2>
                <p>La fecha límite era el ${Store.formatearFecha(examen.fecha_fin)}.</p>
                <div class="acciones-form">
                    <a class="btn btn-primario" href="curso-detalle.html?id=${examen.id_curso}">Volver al curso</a>
                </div>
            </div>`;
        return;
    }
    if (!Number.isNaN(inicioMs) && ahora < inicioMs) {
        encabezado.hidden = true;
        form.hidden = true;
        resultadoDiv.hidden = false;
        resultadoDiv.innerHTML = `
            <div class="apartado">
                <h2>Este examen aún no abre</h2>
                <p>Abre el ${Store.formatearFecha(examen.fecha_inicio)}.</p>
                <div class="acciones-form">
                    <a class="btn btn-primario" href="curso-detalle.html?id=${examen.id_curso}">Volver al curso</a>
                </div>
            </div>`;
        return;
    }

    preguntas.forEach((pregunta, index) => {
        let cuerpo = "";
        if (pregunta.tipo === "OPCION_MULTIPLE") {
            const letras = ["A", "B", "C", "D", "E", "F"];
            cuerpo = (pregunta.opciones_json || []).map((opcion, i) => `
                <label class="opcion">
                    <input type="radio" name="pregunta-${pregunta.id_pregunta}" value="${Store.escaparHTML(opcion)}">
                    <span><strong>${letras[i]}.</strong> ${Store.escaparHTML(opcion)}</span>
                </label>`).join("");
        } else {
            cuerpo = `
                <textarea name="pregunta-${pregunta.id_pregunta}" placeholder="Escribe tu respuesta aquí..."></textarea>
            `;
        }

        contenedor.insertAdjacentHTML("beforeend", `
            <div class="apartado">
                <div class="pregunta-encabezado">
                    <h3>Pregunta ${index + 1}</h3>
                    <span class="puntos">${pregunta.valor_puntos} puntos</span>
                </div>
                <span class="badge badge-azul">${pregunta.tipo === "OPCION_MULTIPLE" ? "Opción múltiple" : "Abierta"}</span>
                <p>${Store.escaparHTML(pregunta.pregunta)}</p>
                ${cuerpo}
            </div>
        `);
    });

    function contarRespondidas() {
        let respondidas = 0;
        preguntas.forEach((pregunta) => {
            if (pregunta.tipo === "OPCION_MULTIPLE") {
                if (document.querySelector(`input[name="pregunta-${pregunta.id_pregunta}"]:checked`)) {
                    respondidas++;
                }
            } else {
                const area = document.querySelector(`textarea[name="pregunta-${pregunta.id_pregunta}"]`);
                if (area && area.value.trim() !== "") {
                    respondidas++;
                }
            }
        });
        return respondidas;
    }

    function actualizarProgreso() {
        progreso.textContent = `Respondidas ${contarRespondidas()} de ${preguntas.length}`;
    }

    contenedor.addEventListener("change", actualizarProgreso);
    contenedor.addEventListener("input", actualizarProgreso);
    actualizarProgreso();

    let segundosRestantes = 90 * 60;
    if (!Number.isNaN(finMs)) {
        const hastaCierre = Math.floor((finMs - Date.now()) / 1000);
        if (hastaCierre > 0 && hastaCierre < segundosRestantes) {
            segundosRestantes = hastaCierre;
        }
    }
    let enviado = false;

    function mostrarTiempo() {
        const minutos = Math.floor(segundosRestantes / 60);
        const segundos = segundosRestantes % 60;
        tiempoRestante.textContent = `${String(minutos).padStart(2, "0")}:${String(segundos).padStart(2, "0")}`;
        if (segundosRestantes <= 600) {
            tiempoRestante.classList.add("poco-tiempo");
        }
    }

    mostrarTiempo();
    const intervalo = setInterval(() => {
        segundosRestantes--;
        if (segundosRestantes <= 0) {
            segundosRestantes = 0;
            mostrarTiempo();
            clearInterval(intervalo);
            enviarRespuestas(true);
            return;
        }
        mostrarTiempo();
    }, 1000);

    function recogerRespuestas() {
        return preguntas.map((pregunta) => {
            let texto = "";
            if (pregunta.tipo === "OPCION_MULTIPLE") {
                const marcado = document.querySelector(`input[name="pregunta-${pregunta.id_pregunta}"]:checked`);
                texto = marcado ? marcado.value : "";
            } else {
                const area = document.querySelector(`textarea[name="pregunta-${pregunta.id_pregunta}"]`);
                texto = area ? area.value.trim() : "";
            }
            return { id_pregunta: pregunta.id_pregunta, respuesta_dada: texto };
        });
    }

    async function enviarRespuestas(automatico) {
        if (enviado) return;
        if (!automatico) {
            const sinResponder = preguntas.length - contarRespondidas();
            const texto = sinResponder > 0
                ? "Tienes " + sinResponder + " pregunta(s) sin responder. ¿Enviar el examen de todas formas?"
                : "¿Enviar el examen? Ya no podrás modificar tus respuestas.";
            const confirmado = await window.Validacion.confirmar(texto, "Enviar");
            if (!confirmado) return;
        }
        enviado = true;
        clearInterval(intervalo);
        const resultado = await Store.responderExamen(idExamen, recogerRespuestas());
        form.hidden = true;
        encabezado.hidden = true;
        resultadoDiv.hidden = false;

        if (!resultado.ok) {
            resultadoDiv.innerHTML = `
                <div class="apartado">
                    <h2>No se pudo enviar el examen</h2>
                    <p>${Store.escaparHTML(resultado.error || "Ocurrió un error.")}</p>
                    <div class="acciones-form">
                        <a class="btn btn-primario" href="curso-detalle.html?id=${examen.id_curso}">Volver al curso</a>
                    </div>
                </div>`;
            return;
        }

        const puntosMultipla = preguntas
            .filter((p) => p.tipo === "OPCION_MULTIPLE")
            .reduce((s, p) => s + Number(p.valor_puntos), 0);
        let estadoHtml;
        if (resultado.resultado.estado_revision === "CALIFICADO") {
            estadoHtml = `<p><strong>Calificación final:</strong> ${resultado.resultado.calificacion_final} / 100</p>`;
        } else if (resultado.resultado.estado_revision === "PENDIENTE_CIERRE") {
            estadoHtml = `<p><strong>Estado:</strong> <span class="badge badge-azul">En espera del cierre</span></p>
            <p>Tu examen se calificará automáticamente al cerrar, el ${Store.formatearFecha(examen.fecha_fin)}.</p>`;
        } else {
            estadoHtml = `<p><strong>Estado:</strong> <span class="badge badge-ambar">PENDIENTE_MANUAL</span></p>
            <p>Tus respuestas abiertas están pendientes de revisión manual por el maestro.</p>`;
        }
        resultadoDiv.innerHTML = `
            <div class="apartado">
                <h2>${automatico ? "Se acabó el tiempo. Examen enviado" : "Examen enviado"}</h2>
                <p><strong>Puntaje automático (opción múltiple):</strong> ${resultado.puntosObtenidos} de ${puntosMultipla} puntos.</p>
                ${estadoHtml}
                <div class="acciones-form">
                    <a class="btn btn-primario" href="curso-detalle.html?id=${examen.id_curso}">Volver al curso</a>
                </div>
            </div>`;
    }

    form.addEventListener("submit", (evento) => {
        evento.preventDefault();
        enviarRespuestas(false);
    });
})();
