(async function () {
const params = new URLSearchParams(window.location.search);
const idCurso = Number(params.get("id"));
const usuario = Store.usuarioActual();
const esMaestro = Store.esMaestro();

if (!usuario) {
    window.location.href = "../index.html";
    return;
}

const curso = await Store.cursoPorId(idCurso);
if (!curso) {
    window.location.href = "cursos.html";
    return;
}

const maestro = curso.maestro;

// Encabezado del curso
function renderEncabezado() {
    const botonRegenerar = esMaestro ? `
        <button type="button" class="btn btn-secundario btn-sm" id="btnRegenerarCodigo">Regenerar código</button>` : "";
    document.getElementById("encabezadoCurso").innerHTML = `
        <h1>${Store.escaparHTML(curso.nombre)}</h1>
        <p class="codigo-invitacion"><strong>Código de invitación:</strong> <span class="codigo">${Store.escaparHTML(curso.codigo_invitacion)}</span>${botonRegenerar}</p>
        <p>${Store.escaparHTML(curso.descripcion)}</p>
        <p><strong>Maestro:</strong> ${Store.escaparHTML(maestro ? maestro.nombre : "Sin maestro")}</p>
    `;
    const btnRegenerar = document.getElementById("btnRegenerarCodigo");
    if (btnRegenerar) {
        btnRegenerar.addEventListener("click", async () => {
            const codigo = await Store.regenerarCodigo(idCurso);
            if (!codigo) {
                return;
            }
            curso.codigo_invitacion = codigo;
            renderEncabezado();
        });
    }
}
renderEncabezado();

// Solo el maestro ve los botones de crear y los de administración.
if (!esMaestro) {
    document.getElementById("btnAbrirDialogoTarea").style.display = "none";
    document.getElementById("btnAbrirDialogoTema").style.display = "none";
    document.getElementById("btnCrearExamen").style.display = "none";
    document.getElementById("btnInscribirAlumno").style.display = "none";
    document.getElementById("btnCargaMasiva").style.display = "none";
}
document.getElementById("btnCrearExamen").href = "crear-examen.html?curso=" + idCurso;

//  Tareas
async function renderTareas() {
    const contenedor = document.getElementById("listaTareas");
    const tareas = await Store.tareasDeCurso(idCurso);
    if (tareas.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay tareas.</p>`;
        return;
    }
    const encabezados = esMaestro
        ? `<th data-campo="titulo">Título</th><th data-campo="descripcion">Descripción</th><th data-campo="esEquipo">Tipo</th><th data-campo="fechaProgramada">Programada</th><th data-campo="fechaLimite">Límite</th><th>Entregas</th><th>Sin calificar</th><th>Equipos</th><th>Prórrogas</th><th class="acciones">Acciones</th>`
        : `<th data-campo="titulo">Título</th><th data-campo="descripcion">Descripción</th><th data-campo="esEquipo">Tipo</th><th data-campo="fechaProgramada">Programada</th><th data-campo="fechaLimite">Límite</th><th>Entregas</th><th>Equipos</th><th class="acciones">Acciones</th>`;
    const filas = (await Promise.all(tareas.map(async (tarea) => {
        const badge = tarea.es_equipo
            ? `<span class="badge badge-azul">En equipo</span>`
            : `<span class="badge badge-gris">Individual</span>`;
        const entregas = await Store.entregasDeTarea(tarea.id_tarea);
        const sinCalificar = entregas.filter((e) => e.calificacion === null).length;
        const equipos = tarea.es_equipo ? await Store.equiposDeTarea(tarea.id_tarea) : [];
        const prorrogasPendientes = (await Store.prorrogasDeTarea(tarea.id_tarea)).filter((p) => p.aprobada === null).length;
        const celdasContadores = esMaestro
            ? `<td>${entregas.length}</td><td>${sinCalificar}</td><td>${tarea.es_equipo ? equipos.length : "—"}</td><td>${prorrogasPendientes}</td>`
            : `<td>${entregas.length}</td><td>${tarea.es_equipo ? equipos.length : "—"}</td>`;
        return `
            <tr>
                <td data-campo="titulo">${Store.escaparHTML(tarea.titulo)}</td>
                <td class="celda-texto" data-campo="descripcion">${Store.escaparHTML(tarea.descripcion)}</td>
                <td data-campo="esEquipo">${badge}</td>
                <td data-campo="fechaProgramada">${Store.formatearFecha(tarea.fecha_programada)}</td>
                <td data-campo="fechaLimite">${Store.formatearFecha(tarea.fecha_limite)}</td>
                ${celdasContadores}
                <td class="acciones">
                    <a class="btn btn-primario btn-sm" href="tarea-detalle.html?id=${tarea.id_tarea}">Ver detalles</a>
                </td>
            </tr>`;
    }))).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/cursos/${idCurso}/tareas">
                <thead><tr>${encabezados}</tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

// Foros
const forosExpandidos = new Set();

async function renderForos() {
    const contenedor = document.getElementById("listaForos");
    const foros = await Store.forosDeCurso(idCurso);
    if (foros.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay temas de foro.</p>`;
        return;
    }
    const filas = (await Promise.all(foros.map(async (foro) => {
        const comentarios = await Store.comentariosDeForo(foro.id_foro);
        const expandido = forosExpandidos.has(foro.id_foro);
        const accionesForo = esMaestro ? `
                <button type="button" class="btn btn-secundario btn-sm" data-editar-foro="${foro.id_foro}">Editar</button>
                <button type="button" class="btn btn-peligro btn-sm" data-eliminar-foro="${foro.id_foro}">Eliminar</button>` : "";
        const fila = `
            <tr>
                <td data-campo="titulo">${Store.escaparHTML(foro.titulo)}</td>
                <td class="celda-texto" data-campo="descripcion">${Store.escaparHTML(foro.descripcion)}</td>
                <td data-campo="fechaCreacion">${Store.formatearFecha(foro.fecha_creacion)}</td>
                <td>${comentarios.length}</td>
                <td class="acciones">
                    <button type="button" class="btn btn-secundario btn-sm" data-ver-foro="${foro.id_foro}">${expandido ? "Ocultar comentarios" : `Ver comentarios (${comentarios.length})`}</button>
                    ${accionesForo}
                </td>
            </tr>`;
        if (!expandido) return fila;
        const filasComentarios = comentarios.map((c) => {
            const puedeEditar = esMaestro || c.id_usuario === usuario.id_usuario;
            const accionesComentario = puedeEditar ? `
                    <button type="button" class="btn btn-secundario btn-sm" data-editar-comentario="${c.id_comentario}">Editar</button>
                    <button type="button" class="btn btn-peligro btn-sm" data-eliminar-comentario="${c.id_comentario}">Eliminar</button>` : "";
            return `
                <tr>
                    <td data-campo="usuario.nombre">${Store.escaparHTML(c.autor ? c.autor.nombre : "Usuario")}</td>
                    <td class="celda-texto" data-campo="contenido">${Store.escaparHTML(c.contenido)}</td>
                    <td data-campo="fechaComentario">${Store.formatearFecha(c.fecha_comentario)}</td>
                    <td class="acciones">${accionesComentario}</td>
                </tr>`;
        }).join("");
        const tablaComentarios = comentarios.length === 0
            ? `<p class="vacio">Aún no hay comentarios.</p>`
            : `
            <div class="tabla-contenedor">
                <table class="tabla" data-endpoint="/api/foros/${foro.id_foro}/comentarios">
                    <thead><tr><th data-campo="usuario.nombre">Usuario</th><th data-campo="contenido">Contenido</th><th data-campo="fechaComentario">Fecha</th><th class="acciones">Acciones</th></tr></thead>
                    <tbody>${filasComentarios}</tbody>
                </table>
            </div>`;
        return fila + `
            <tr class="fila-detalle" data-foro-id="${foro.id_foro}">
                <td colspan="5">
                    ${tablaComentarios}
                    <form class="form-comentario" data-foro-id="${foro.id_foro}">
                        <textarea placeholder="Escribe un comentario..." rows="2" required></textarea>
                        <div class="acciones-form">
                            <button class="btn btn-primario btn-sm" type="submit">Comentar</button>
                        </div>
                    </form>
                </td>
            </tr>`;
    }))).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/cursos/${idCurso}/foros">
                <thead><tr><th data-campo="titulo">Título</th><th data-campo="descripcion">Descripción</th><th data-campo="fechaCreacion">Fecha</th><th>Comentarios</th><th class="acciones">Acciones</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

//  Examenes
async function renderExamenes() {
    const contenedor = document.getElementById("listaExamenes");
    const examenes = await Store.examenesDeCurso(idCurso);
    if (examenes.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay exámenes en este curso.</p>`;
        return;
    }
    const filas = (await Promise.all(examenes.map(async (examen) => {
        const resultado = usuario ? await Store.resultadoDeExamen(examen.id_examen, usuario.id_usuario) : null;
        let acciones = "";
        if (esMaestro) {
            acciones = `
                <a class="btn btn-secundario btn-sm" href="calificar-examen.html?examen=${examen.id_examen}">Calificar</a>
                <a class="btn btn-secundario btn-sm" href="crear-examen.html?examen=${examen.id_examen}">Editar</a>
                <button type="button" class="btn btn-peligro btn-sm" data-eliminar-examen="${examen.id_examen}">Eliminar</button>`;
        } else if (resultado) {
            acciones = `<a class="btn btn-secundario btn-sm" href="responder-examen.html?examen=${examen.id_examen}">Ver resultado</a>`;
        } else if (!await Store.examenAbierto(examen)) {
            acciones = `<span class="badge badge-gris">Aún no abre</span>`;
        } else if (await Store.examenCerrado(examen)) {
            acciones = `<span class="badge badge-gris">Cerrado</span>`;
        } else {
            acciones = `
                <a class="btn btn-primario btn-sm" href="responder-examen.html?examen=${examen.id_examen}">Responder</a>`;
        }
        return `
            <tr>
                <td data-campo="titulo">${Store.escaparHTML(examen.titulo)}</td>
                <td data-campo="fechaInicio">${Store.formatearFecha(examen.fecha_inicio)}</td>
                <td data-campo="fechaFin">${Store.formatearFecha(examen.fecha_fin)}</td>
                <td class="acciones">${acciones}</td>
            </tr>`;
    }))).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/cursos/${idCurso}/examenes">
                <thead><tr><th data-campo="titulo">Título</th><th data-campo="fechaInicio">Inicio</th><th data-campo="fechaFin">Fin</th><th class="acciones">Acciones</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

// Alumnos
async function renderAlumnos() {
    const contenedor = document.getElementById("listaAlumnos");
    const alumnos = await Store.alumnosDeCurso(idCurso);
    if (alumnos.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay alumnos inscritos.</p>`;
        return;
    }
    const filas = alumnos.map((alumno) => `
        <tr>
            <td data-campo="alumno.nombre">${Store.escaparHTML(alumno.nombre)}</td>
            <td data-campo="alumno.email">${Store.escaparHTML(alumno.email)}</td>
            <td data-campo="fechaInscripcion">${alumno.fecha_inscripcion ? Store.formatearFecha(alumno.fecha_inscripcion) : "—"}</td>
            <td class="acciones">${esMaestro ? `<button type="button" class="btn btn-peligro btn-sm" data-dar-baja="${alumno.id_usuario}">Dar de baja</button>` : ""}</td>
        </tr>`).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/cursos/${idCurso}/alumnos">
                <thead><tr><th data-campo="alumno.nombre">Nombre</th><th data-campo="alumno.email">Correo</th><th data-campo="fechaInscripcion">Inscrito</th><th class="acciones">Acciones</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

async function renderTodo() {
    await renderTareas();
    await renderForos();
    await renderExamenes();
    await renderAlumnos();
}

await renderTodo();

// Pestañas
function mostrarPestana(nombre) {
    document.querySelectorAll("section[data-pestana]").forEach((seccion) => {
        seccion.hidden = seccion.dataset.pestana !== nombre;
    });
    document.querySelectorAll("#barraPestanas button").forEach((boton) => {
        boton.classList.toggle("activo", boton.dataset.pestana === nombre);
    });
}

document.querySelectorAll("#barraPestanas button").forEach((boton) => {
    boton.addEventListener("click", () => mostrarPestana(boton.dataset.pestana));
});

mostrarPestana("tareas");

// Dialogos
function abrirDialogo(id) {
    document.getElementById(id).showModal();
}

function cerrarDialogo(id) {
    document.getElementById(id).close();
}

document.querySelectorAll("[data-cerrar]").forEach((boton) => {
    boton.addEventListener("click", () => {
        cerrarDialogo(boton.dataset.cerrar);
    });
});

document.querySelectorAll("dialog.dialogo").forEach((dialogo) => {
    dialogo.addEventListener("click", (evento) => {
        if (evento.target === dialogo) {
            dialogo.close();
        }
    });
});

document.getElementById("btnAbrirDialogoTarea").addEventListener("click", () => abrirDialogo("dialogoTarea"));
document.getElementById("btnAbrirDialogoTema").addEventListener("click", () => abrirDialogo("dialogoTema"));
document.getElementById("btnInscribirAlumno").addEventListener("click", () => abrirDialogo("dialogoInscribir"));
document.getElementById("btnCargaMasiva").addEventListener("click", () => abrirDialogo("dialogoCarga"));

// Inscribir alumno
document.getElementById("formInscribirAlumno").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const email = document.getElementById("emailAlumno").value;
    const resultado = await Store.inscribirAlumno(idCurso, email);
    if (!resultado.ok) {
        return;
    }
    await renderAlumnos();
    evento.target.reset();
    cerrarDialogo("dialogoInscribir");
});

// Carga masiva
const archivoXlsx = document.getElementById("archivoXlsx");
const nombreArchivo = document.getElementById("nombreArchivo");

archivoXlsx.addEventListener("change", () => {
    if (archivoXlsx.files.length > 0) {
        nombreArchivo.textContent = "Archivo seleccionado: " + archivoXlsx.files[0].name;
    } else {
        nombreArchivo.textContent = "Ningún archivo seleccionado";
    }
});

document.getElementById("formCargaMasiva").addEventListener("submit", (evento) => {
    evento.preventDefault();
    const nombre = archivoXlsx.files.length > 0 ? archivoXlsx.files[0].name : "";
    evento.target.reset();
    nombreArchivo.textContent = "Ningún archivo seleccionado";
    cerrarDialogo("dialogoCarga");
});

// Crear tarea
document.getElementById("formTarea").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    await Store.crearTarea(idCurso, {
        titulo: document.getElementById("tareaTitulo").value,
        descripcion: document.getElementById("tareaDescripcion").value,
        fecha_programada: document.getElementById("tareaFechaProgramada").value,
        fecha_limite: document.getElementById("tareaFechaLimite").value,
        es_equipo: document.getElementById("tareaEsEquipo").checked
    });
    await renderTodo();
    evento.target.reset();
    cerrarDialogo("dialogoTarea");
});

// Crear tema de foro
document.getElementById("formTema").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    await Store.crearTema(idCurso, {
        titulo: document.getElementById("temaTitulo").value,
        descripcion: document.getElementById("temaDescripcion").value
    });
    await renderForos();
    evento.target.reset();
    cerrarDialogo("dialogoTema");
});

// Comentar en un foro
document.getElementById("listaForos").addEventListener("submit", async (evento) => {
    if (!evento.target.classList.contains("form-comentario")) return;
    evento.preventDefault();
    const tarjeta = evento.target.closest("[data-foro-id]");
    const idForo = Number(tarjeta.dataset.foroId);
    const textarea = evento.target.querySelector("textarea");
    const contenido = textarea.value.trim();
    if (!contenido) return;
    await Store.comentarForo(idForo, contenido);
    await renderForos();
});

// Foros: ver comentarios, editar/eliminar tema y comentario
document.getElementById("listaForos").addEventListener("click", async (evento) => {
    const botonVer = evento.target.closest("[data-ver-foro]");
    if (botonVer) {
        const idForo = Number(botonVer.dataset.verForo);
        if (forosExpandidos.has(idForo)) forosExpandidos.delete(idForo);
        else forosExpandidos.add(idForo);
        await renderForos();
        return;
    }
    const botonEditarForo = evento.target.closest("[data-editar-foro]");
    if (botonEditarForo) {
        const foros = await Store.forosDeCurso(idCurso);
        const foro = foros.find((f) => f.id_foro === Number(botonEditarForo.dataset.editarForo));
        if (!foro) return;
        document.getElementById("editarForoId").value = foro.id_foro;
        document.getElementById("editarForoTitulo").value = foro.titulo;
        document.getElementById("editarForoDescripcion").value = foro.descripcion;
        abrirDialogo("dialogoEditarTema");
        return;
    }
    const botonEliminarForo = evento.target.closest("[data-eliminar-foro]");
    if (botonEliminarForo) {
        await Store.eliminarForo(Number(botonEliminarForo.dataset.eliminarForo));
        await renderForos();
        return;
    }
    const botonEditarComentario = evento.target.closest("[data-editar-comentario]");
    if (botonEditarComentario) {
        const idComentario = Number(botonEditarComentario.dataset.editarComentario);
        const comentarios = await Store.comentariosDeForo(Number(evento.target.closest("[data-foro-id]").dataset.foroId));
        const comentario = comentarios.find((c) => c.id_comentario === idComentario);
        if (!comentario) return;
        document.getElementById("editarComentarioId").value = idComentario;
        document.getElementById("editarComentarioContenido").value = comentario.contenido;
        abrirDialogo("dialogoEditarComentario");
        return;
    }
    const botonEliminarComentario = evento.target.closest("[data-eliminar-comentario]");
    if (botonEliminarComentario) {
        const resultado = await Store.eliminarComentario(Number(botonEliminarComentario.dataset.eliminarComentario));
        if (!resultado.ok) {
            return;
        }
        await renderForos();
        return;
    }
});

document.getElementById("formEditarTema").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    await Store.actualizarForo(Number(document.getElementById("editarForoId").value), {
        titulo: document.getElementById("editarForoTitulo").value,
        descripcion: document.getElementById("editarForoDescripcion").value
    });
    await renderForos();
    cerrarDialogo("dialogoEditarTema");
});

document.getElementById("formEditarComentario").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const resultado = await Store.actualizarComentario(
        Number(document.getElementById("editarComentarioId").value),
        document.getElementById("editarComentarioContenido").value
    );
    if (!resultado.ok) {
        return;
    }
    await renderForos();
    cerrarDialogo("dialogoEditarComentario");
});

// Exámenes: eliminar
document.getElementById("listaExamenes").addEventListener("click", async (evento) => {
    const botonEliminar = evento.target.closest("[data-eliminar-examen]");
    if (!botonEliminar) return;
    await Store.eliminarExamen(Number(botonEliminar.dataset.eliminarExamen));
    await renderExamenes();
});

// Alumnos: dar de baja
document.getElementById("listaAlumnos").addEventListener("click", async (evento) => {
    const botonBaja = evento.target.closest("[data-dar-baja]");
    if (!botonBaja) return;
    const resultado = await Store.darDeBajaAlumno(idCurso, Number(botonBaja.dataset.darBaja));
    if (!resultado.ok) {
        return;
    }
    await renderAlumnos();
});
})();
