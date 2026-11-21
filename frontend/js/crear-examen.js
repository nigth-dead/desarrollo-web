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

// Valida las preguntas en el formulario: texto, puntos y opciones.
// Si algo falla marca el campo, muestra el error y devuelve el mensaje.
function validarPreguntas() {
    const V = window.Validacion;
    const bloques = contenedorPreguntas.querySelectorAll(".bloque-pregunta");
    if (bloques.length === 0) return "Agrega al menos una pregunta al examen.";
    for (let i = 0; i < bloques.length; i++) {
        const bloque = bloques[i];
        const n = i + 1;
        const texto = bloque.querySelector(".input-pregunta");
        if (!texto.value.trim()) {
            V.marcarCampo(texto, "Escribe la pregunta " + n + ".");
            return "La pregunta " + n + " está vacía. Escríbela antes de guardar.";
        }
        const puntos = bloque.querySelector(".input-puntos");
        if (puntos.value === "" || Number.isNaN(Number(puntos.value)) || Number(puntos.value) < 1) {
            V.marcarCampo(puntos, "El valor debe ser de al menos 1 punto.");
            return "La pregunta " + n + " debe valer al menos 1 punto.";
        }
        if (bloque.querySelector(".select-tipo").value === "OPCION_MULTIPLE") {
            const opciones = bloque.querySelectorAll(".input-opcion");
            for (let j = 0; j < opciones.length; j++) {
                if (!opciones[j].value.trim()) {
                    V.marcarCampo(opciones[j], "Escribe esta opción.");
                    return "La pregunta " + n + " tiene una opción vacía. Complétala antes de guardar.";
                }
            }
            const letra = bloque.querySelector(".select-correcta").value;
            const correcta = opciones[["A", "B", "C", "D"].indexOf(letra)];
            if (!correcta || !correcta.value.trim()) {
                V.marcarCampo(bloque.querySelector(".select-correcta"), "Elige una respuesta correcta con texto.");
                return "La respuesta correcta de la pregunta " + n + " está vacía.";
            }
        }
    }
    return null;
}

// Guardar el examen en el Store y volver al detalle del curso
formExamen.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const V = window.Validacion;
    V.limpiarError(formExamen);

    const tituloInput = document.getElementById("examenTitulo");
    const inicioInput = document.getElementById("examenInicio");
    const finInput = document.getElementById("examenFin");
    const titulo = tituloInput.value.trim();
    if (!titulo) {
        V.marcarCampo(tituloInput, "Escribe el título del examen.");
        V.mostrarError(formExamen, "Escribe el título del examen.");
        return;
    }
    // En edición el inicio puede estar en el pasado (examen ya abierto);
    // solo se exige que el fin sea posterior al inicio.
    const errorFechas = examenEditar
        ? (inicioInput.value && finInput.value && new Date(finInput.value).getTime() <= new Date(inicioInput.value).getTime()
            ? "La fecha de fin debe ser posterior a la de inicio." : null)
        : V.validarRangoFechas(inicioInput.value, finInput.value, "inicio", "fin");
    if (errorFechas) {
        V.marcarCampo(finInput, errorFechas);
        V.mostrarError(formExamen, errorFechas);
        return;
    }
    const preguntas = leerPreguntas();
    const errorPreguntas = validarPreguntas();
    if (errorPreguntas) {
        V.mostrarError(formExamen, errorPreguntas);
        return;
    }

    const datosExamen = {
        titulo: titulo,
        fecha_inicio: inicioInput.value,
        fecha_fin: finInput.value
    };

    if (examenEditar) {
        // actualizar el examen y sincronizar sus preguntas.
        const actualizado = await Store.actualizarExamen(idExamen, datosExamen);
        if (!V.siFalloMostrar(formExamen, actualizado, "No se pudo guardar los cambios del examen. Inténtalo de nuevo.")) {
            return;
        }
        const idsEnviados = new Set();
        let falloPregunta = false;
        for (const p of preguntas) {
            if (p.id_pregunta) {
                idsEnviados.add(p.id_pregunta);
                const guardada = await Store.actualizarPregunta(p.id_pregunta, p);
                if (!guardada) falloPregunta = true;
            } else {
                const creada = await Store.agregarPregunta(idExamen, p);
                if (creada) idsEnviados.add(creada.id_pregunta);
                else falloPregunta = true;
            }
        }
        if (falloPregunta) {
            V.mostrarError(formExamen, "El examen se guardó, pero una pregunta no se pudo guardar. Revisa las preguntas e inténtalo de nuevo.");
            return;
        }
        const guardadas = await Store.preguntasDeExamen(idExamen);
        for (const p of guardadas) {
            if (!idsEnviados.has(p.id_pregunta)) await Store.eliminarPregunta(p.id_pregunta);
        }
    } else {
        const creado = await Store.crearExamen(idCurso, {
            titulo: datosExamen.titulo,
            fecha_inicio: datosExamen.fecha_inicio,
            fecha_fin: datosExamen.fecha_fin,
            preguntas: preguntas
        });
        if (!V.siFalloMostrar(formExamen, creado, "No se pudo crear el examen. Revisa las fechas y las preguntas e inténtalo de nuevo.")) {
            return;
        }
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
