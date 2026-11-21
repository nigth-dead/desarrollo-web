(async function () {
if (!Store.esMaestro()) {
    window.location.href = "cursos.html";
    return;
}

const parametros = new URLSearchParams(window.location.search);
const idExamen = Number(parametros.get("examen")) || null;
let idCurso = Number(parametros.get("curso"));
const cursosDisponibles = await Store.cursos();

// En modo edición el curso es el del examen.
let examenEditar = null;
if (idExamen) {
    examenEditar = await Store.examenPorId(idExamen);
    if (examenEditar) idCurso = examenEditar.id_curso;
}
if (!idCurso || !await Store.cursoPorId(idCurso)) {
    idCurso = cursosDisponibles.length > 0 ? cursosDisponibles[0].id_curso : null;
}

const curso = idCurso ? await Store.cursoPorId(idCurso) : null;
const contenedorPreguntas = document.getElementById("contenedorPreguntas");
const formExamen = document.getElementById("formExamen");
const btnCancelar = document.getElementById("btnCancelarExamen");

if (idExamen && !examenEditar) {
    window.location.href = "cursos.html";
    return;
}
if (curso) {
    document.getElementById("nombreCurso").textContent = curso.nombre;
    btnCancelar.href = "curso-detalle.html?id=" + curso.id_curso;
    if (examenEditar) {
        document.getElementById("tituloExamen").textContent = "Editar examen";
        document.getElementById("btnGuardarExamen").textContent = "Guardar cambios";
        document.getElementById("examenTitulo").value = examenEditar.titulo;
        document.getElementById("examenInicio").value = String(examenEditar.fecha_inicio || "").slice(0, 16);
        document.getElementById("examenFin").value = String(examenEditar.fecha_fin || "").slice(0, 16);
    }
} else {
    window.location.href = "cursos.html";
    return;
}

// Activa o desactiva las opciones segun el tipo
function actualizarZonaOpciones(bloque) {
    const esAbierta = bloque.querySelector(".select-tipo").value === "ABIERTA";
    const zona = bloque.querySelector(".zona-opciones");
    zona.hidden = esAbierta;
    zona.querySelectorAll(".input-opcion").forEach((input) => {
        input.required = !esAbierta;
        input.disabled = esAbierta;
    });
    zona.querySelector(".select-correcta").disabled = esAbierta;
}

// Agrega un bloque de pregunta
function agregarPregunta(datos) {
    const letras = ["A", "B", "C", "D"];
    const opciones = (datos && datos.opciones) || ["", "", "", ""];
    const indiceCorrecta = datos && datos.respuesta_correcta
        ? opciones.indexOf(datos.respuesta_correcta)
        : 0;
    contenedorPreguntas.insertAdjacentHTML("beforeend", `
        <div class="apartado bloque-pregunta"${datos && datos.id_pregunta ? ` data-pregunta-id="${datos.id_pregunta}"` : ""}>
            <span class="num-pregunta">Pregunta 1</span>
            <label>Pregunta</label>
            <input type="text" class="input-pregunta" placeholder="Escribe la pregunta..." value="${datos ? Store.escaparHTML(datos.pregunta) : ""}" required>
            <div class="fila-doble">
                <div>
                    <label>Tipo</label>
                    <select class="select-tipo">
                        <option value="OPCION_MULTIPLE"${datos && datos.tipo === "OPCION_MULTIPLE" ? " selected" : ""}>Opción múltiple</option>
                        <option value="ABIERTA"${datos && datos.tipo === "ABIERTA" ? " selected" : ""}>Abierta</option>
                    </select>
                </div>
                <div>
                    <label>Valor en puntos</label>
                    <input type="number" class="input-puntos" min="1" value="${datos ? datos.valor_puntos : 10}" required>
                </div>
            </div>
            <div class="zona-opciones"${datos && datos.tipo === "ABIERTA" ? " hidden" : ""}>
                <label>Opciones</label>
                <input type="text" class="input-opcion" placeholder="Opción A" value="${Store.escaparHTML(opciones[0] || "")}" required>
                <input type="text" class="input-opcion" placeholder="Opción B" value="${Store.escaparHTML(opciones[1] || "")}" required>
                <input type="text" class="input-opcion" placeholder="Opción C" value="${Store.escaparHTML(opciones[2] || "")}" required>
                <input type="text" class="input-opcion" placeholder="Opción D" value="${Store.escaparHTML(opciones[3] || "")}" required>
                <label>Respuesta correcta</label>
                <select class="select-correcta">
                    ${letras.map((letra, i) => `<option value="${letra}"${i === (indiceCorrecta < 0 ? 0 : indiceCorrecta) ? " selected" : ""}>${letra}</option>`).join("")}
                </select>
            </div>
            <button type="button" class="btn-quitar">Quitar pregunta</button>
        </div>
    `);
    actualizarZonaOpciones(contenedorPreguntas.lastElementChild);
    renumerarPreguntas();
}

// Renumera las preguntas después de agregar o quitar una
function renumerarPreguntas() {
    contenedorPreguntas.querySelectorAll(".bloque-pregunta").forEach((bloque, indice) => {
        bloque.querySelector(".num-pregunta").textContent = "Pregunta " + (indice + 1);
    });
}

// Mostrar u ocultar la zona de opciones según el tipo
contenedorPreguntas.addEventListener("change", (evento) => {
    if (!evento.target.classList.contains("select-tipo")) return;
    actualizarZonaOpciones(evento.target.closest(".bloque-pregunta"));
});

// Quitar pregunta 
contenedorPreguntas.addEventListener("click", (evento) => {
    const boton = evento.target.closest(".btn-quitar");
    if (!boton) return;
    boton.closest(".bloque-pregunta").remove();
    renumerarPreguntas();
});

document.getElementById("btnAddPregunta").addEventListener("click", () => agregarPregunta());

// Lee los bloques del formulario y devuelve las preguntas.
function leerPreguntas() {
    const preguntas = [];
    contenedorPreguntas.querySelectorAll(".bloque-pregunta").forEach((bloque) => {
        const tipo = bloque.querySelector(".select-tipo").value;
        const pregunta = {
            pregunta: bloque.querySelector(".input-pregunta").value.trim(),
            tipo: tipo,
            valor_puntos: Number(bloque.querySelector(".input-puntos").value)
        };
        const id = bloque.dataset.preguntaId ? Number(bloque.dataset.preguntaId) : null;
        if (id) pregunta.id_pregunta = id;
        if (tipo === "OPCION_MULTIPLE") {
            const opciones = Array.from(bloque.querySelectorAll(".input-opcion")).map((input) => input.value.trim());
            const letra = bloque.querySelector(".select-correcta").value;
            pregunta.opciones = opciones;
            pregunta.respuesta_correcta = opciones[["A", "B", "C", "D"].indexOf(letra)] || "";
        } else {
            pregunta.opciones = [];
            pregunta.respuesta_correcta = "";
        }
        preguntas.push(pregunta);
    });
    return preguntas;
}

// Guardar el examen en el Store y volver al detalle del curso
formExamen.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const preguntas = leerPreguntas();
    if (preguntas.length === 0) {
        return;
    }

    const datosExamen = {
        titulo: document.getElementById("examenTitulo").value.trim(),
        fecha_inicio: document.getElementById("examenInicio").value,
        fecha_fin: document.getElementById("examenFin").value
    };

    if (examenEditar) {
        // actualizar el examen y sincronizar sus preguntas.
        await Store.actualizarExamen(idExamen, datosExamen);
        const idsEnviados = new Set();
        for (const p of preguntas) {
            if (p.id_pregunta) {
                idsEnviados.add(p.id_pregunta);
                await Store.actualizarPregunta(p.id_pregunta, p);
            } else {
                const creada = await Store.agregarPregunta(idExamen, p);
                if (creada) idsEnviados.add(creada.id_pregunta);
            }
        }
        const guardadas = await Store.preguntasDeExamen(idExamen);
        for (const p of guardadas) {
            if (!idsEnviados.has(p.id_pregunta)) await Store.eliminarPregunta(p.id_pregunta);
        }
    } else {
        await Store.crearExamen(idCurso, {
            titulo: datosExamen.titulo,
            fecha_inicio: datosExamen.fecha_inicio,
            fecha_fin: datosExamen.fecha_fin,
            preguntas: preguntas
        });
    }
    window.location.href = "curso-detalle.html?id=" + idCurso;
});

// Se cargan las preguntas guardadas
if (examenEditar) {
    const preguntasGuardadas = await Store.preguntasDeExamen(idExamen);
    preguntasGuardadas.forEach((p) => {
        agregarPregunta({
            id_pregunta: p.id_pregunta,
            pregunta: p.pregunta,
            tipo: p.tipo,
            valor_puntos: p.valor_puntos,
            opciones: p.opciones_json || [],
            respuesta_correcta: p.respuesta_correcta
        });
    });
    if (contenedorPreguntas.querySelectorAll(".bloque-pregunta").length === 0) agregarPregunta();
} else {
    agregarPregunta();
}
})();
