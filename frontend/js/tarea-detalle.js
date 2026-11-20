(async function () {
const params = new URLSearchParams(window.location.search);
const idTarea = Number(params.get("id"));
const usuario = Store.usuarioActual();
const esMaestro = Store.esMaestro();

if (!usuario) {
    window.location.href = "../index.html";
    return;
}

const tarea = await Store.tareaPorId(idTarea);
if (!tarea) {
    window.location.href = "cursos.html";
    return;
}

const curso = await Store.cursoPorId(tarea.id_curso);
if (!curso || (!esMaestro && !(await Store.alumnosDeCurso(curso.id_curso)).some((a) => a.id_usuario === usuario.id_usuario))) {
    window.location.href = "cursos.html";
    return;
}

document.getElementById("volverCurso").href = "curso-detalle.html?id=" + curso.id_curso;

let entregaACalificarId = null;
let prorrogasActuales = [];
let entregasActuales = [];
let descripcionActualId = null;

// Encabezado de la tarea
async function renderEncabezado() {
    const badge = tarea.es_equipo
        ? `<span class="badge badge-azul">En equipo</span>`
        : `<span class="badge badge-gris">Individual</span>`;
    const acciones = esMaestro ? `
        <div class="acciones">
            <button type="button" class="btn btn-secundario" id="btnEditarTarea">Editar tarea</button>
            <button type="button" class="btn btn-peligro" id="btnEliminarTarea">Eliminar tarea</button>
        </div>` : "";
    const limiteEfectivo = await Store.fechaLimiteEfectiva(tarea.id_tarea, usuario.id_usuario);
    const limiteMostrado = limiteEfectivo || tarea.fecha_limite;
    const extendido = limiteMostrado && tarea.fecha_limite &&
        new Date(limiteMostrado).getTime() > new Date(tarea.fecha_limite).getTime();
    const lineaLimite = extendido
        ? `<p><strong>Límite:</strong> ${Store.formatearFecha(limiteMostrado)} (prórroga aprobada; original: ${Store.formatearFecha(tarea.fecha_limite)})</p>`
        : `<p><strong>Límite:</strong> ${Store.formatearFecha(tarea.fecha_limite)}</p>`;
    document.getElementById("encabezadoTarea").innerHTML = `
        <h1>${Store.escaparHTML(tarea.titulo)}</h1>
        ${badge}
        <p>${Store.escaparHTML(tarea.descripcion)}</p>
        <p><strong>Curso:</strong> ${Store.escaparHTML(curso.nombre)}</p>
        <p><strong>Programada:</strong> ${Store.formatearFecha(tarea.fecha_programada)}</p>
        ${lineaLimite}
        ${acciones}
    `;
    if (esMaestro) {
        document.getElementById("btnEditarTarea").addEventListener("click", () => {
            document.getElementById("editarTareaTitulo").value = tarea.titulo;
            document.getElementById("editarTareaDescripcion").value = tarea.descripcion;
            document.getElementById("editarTareaFechaProgramada").value = String(tarea.fecha_programada || "").slice(0, 16);
            document.getElementById("editarTareaFechaLimite").value = String(tarea.fecha_limite || "").slice(0, 16);
            document.getElementById("editarTareaEsEquipo").checked = !!tarea.es_equipo;
            abrirDialogo("dialogoEditarTarea");
        });
        document.getElementById("btnEliminarTarea").addEventListener("click", async () => {
            await Store.eliminarTarea(tarea.id_tarea);
            window.location.href = "curso-detalle.html?id=" + curso.id_curso;
        });
    }
}
await renderEncabezado();

if (esMaestro) {
    document.getElementById("btnEntregar").style.display = "none";
    document.getElementById("btnSolicitarProrroga").style.display = "none";
}

// Pestañas
function mostrarPestana(nombre) {
    document.querySelectorAll("section[data-pestana]").forEach((seccion) => {
        seccion.hidden = seccion.dataset.pestana !== nombre;
    });
    document.querySelectorAll("#barraPestanasTarea button").forEach((boton) => {
        boton.classList.toggle("activo", boton.dataset.pestana === nombre);
    });
}

document.querySelectorAll("#barraPestanasTarea button").forEach((boton) => {
    boton.addEventListener("click", () => mostrarPestana(boton.dataset.pestana));
});

// Sin equipos
if (!tarea.es_equipo) {
    document.querySelector('#barraPestanasTarea button[data-pestana="equipos"]').style.display = "none";
}
mostrarPestana("entregas");

// Entregas
async function equiposMios() {
    return (await Store.equiposDeTarea(tarea.id_tarea))
        .filter((e) => e.integrantes.some((u) => u.id_usuario === usuario.id_usuario));
}

async function miEntrega() {
    const entregas = await Store.entregasDeTarea(tarea.id_tarea);
    const misEquipos = (await equiposMios()).map((e) => e.id_equipo);
    return entregas.find((e) => e.id_alumno === usuario.id_usuario || (e.id_equipo && misEquipos.includes(e.id_equipo)));
}

async function miProrrogaPendiente() {
    const misEquipos = (await Store.equiposDeTarea(tarea.id_tarea))
        .filter((e) => e.integrantes.some((u) => u.id_usuario === usuario.id_usuario))
        .map((e) => e.id_equipo);
    return (await Store.prorrogasDeTarea(tarea.id_tarea)).find((p) =>
        p.aprobada === null && (p.id_alumno === usuario.id_usuario || (p.id_equipo && misEquipos.includes(p.id_equipo))));
}

async function renderEntregas() {
    const contenedor = document.getElementById("listaEntregas");
    const entregas = await Store.entregasDeTarea(tarea.id_tarea);
    entregasActuales = entregas;
    const btnEntregar = document.getElementById("btnEntregar");
    const mia = esMaestro ? null : await miEntrega();
    const vencida = !esMaestro && await Store.entregaVencida(tarea.id_tarea, usuario.id_usuario);
    const anticipada = !esMaestro && await Store.entregaAnticipada(tarea.id_tarea);
    const sinEquipo = !esMaestro && tarea.es_equipo && (await equiposMios()).length === 0;
    if (!esMaestro) btnEntregar.style.display = (mia || vencida || anticipada || sinEquipo) ? "none" : "";
    contenedor.innerHTML = "";
    if (!esMaestro && !mia) {
        if (anticipada) {
            contenedor.insertAdjacentHTML("beforeend", `<p class="nota">Esta tarea aún no abre (abre el ${Store.formatearFecha(tarea.fecha_programada)}). Podrás entregar a partir de esa fecha.</p>`);
        } else if (vencida) {
            if (!await miProrrogaPendiente()) {
                contenedor.insertAdjacentHTML("beforeend", `
                    <button type="button" class="btn btn-primario" id="btnIrProrroga">Solicitar prórroga</button>`);
                document.getElementById("btnIrProrroga").addEventListener("click", () => mostrarPestana("prorrogas"));
            }
        } else {
            contenedor.insertAdjacentHTML("beforeend", `<p class="vacio">Aún no has entregado esta tarea.</p>`);
        }
        return;
    }
    if (entregas.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay entregas.</p>`;
        return;
    }
    const visibles = esMaestro ? entregas : [mia];
    const sinCalificar = entregas.filter((e) => e.calificacion === null).length;
    const contador = esMaestro ? `<p class="contador-comentarios">Entregas: ${entregas.length} · Sin calificar: ${sinCalificar}</p>` : "";
    const filas = visibles.map((entrega) => {
        const nombreAlumno = entrega.alumno ? entrega.alumno.nombre : "Alumno";
        const nombreEquipo = entrega.equipo ? entrega.equipo.nombre_equipo : "—";
        const badge = entrega.calificacion === null
            ? `<span class="badge badge-ambar">Sin calificar</span>`
            : `<span class="badge badge-verde">Calificado: ${entrega.calificacion}</span>`;
        const botonesCalificar = esMaestro ? `
                <button type="button" class="btn btn-secundario btn-sm" data-calificar-id="${entrega.id_entrega}">Calificar</button>` : "";

        const desc = entrega.descripcion || "";
        const descCorta = desc.length > 80
            ? Store.escaparHTML(desc.slice(0, 80)) + "…"
            : (desc ? Store.escaparHTML(desc) : "—");
        const btnVerDesc = desc.length > 80
            ? ` <button type="button" class="btn btn-secundario btn-sm" data-ver-descripcion="${entrega.id_entrega}">Ver</button>` : "";
        const botonesAlumno = !esMaestro && !vencida && !anticipada ? `
                <button type="button" class="btn btn-secundario btn-sm" data-editar-entrega="${entrega.id_entrega}">Editar</button>
                <button type="button" class="btn btn-peligro btn-sm" data-eliminar-entrega="${entrega.id_entrega}">Eliminar</button>` : "";
        return `
            <tr>
                <td data-campo="alumno.nombre">${Store.escaparHTML(nombreAlumno)}</td>
                <td data-campo="equipo.nombreEquipo">${Store.escaparHTML(nombreEquipo)}</td>
                <td data-campo="archivoUrl"><a href="${Store.escaparHTML(entrega.archivo_url)}" target="_blank" rel="noopener">${Store.escaparHTML(entrega.archivo_url)}</a></td>
                <td class="celda-texto" data-campo="descripcion">${descCorta}${btnVerDesc}</td>
                <td data-campo="fechaEntrega">${Store.formatearFecha(entrega.fecha_entrega)}</td>
                <td data-campo="calificacion">${badge}</td>
                <td class="celda-texto" data-campo="comentarioMaestro">${entrega.comentario_maestro ? Store.escaparHTML(entrega.comentario_maestro) : "—"}</td>
                <td class="acciones">${botonesCalificar}${botonesAlumno}</td>
            </tr>`;
    }).join("");
    contenedor.innerHTML = `
        ${contador}
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/tareas/${tarea.id_tarea}/entregas">
                <thead><tr><th data-campo="alumno.nombre">Alumno</th><th data-campo="equipo.nombreEquipo">Equipo</th><th data-campo="archivoUrl">Archivo</th><th data-campo="descripcion">Descripción</th><th data-campo="fechaEntrega">Fecha</th><th data-campo="calificacion">Calificación</th><th data-campo="comentarioMaestro">Comentario</th><th class="acciones">Acciones</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

// Equipos
async function renderEquipos() {
    const contenedor = document.getElementById("listaEquipos");
    const equipos = await Store.equiposDeTarea(tarea.id_tarea);
    if (!esMaestro) {
        const estoyEnEquipo = equipos.some((e) => e.integrantes.some((u) => u.id_usuario === usuario.id_usuario));
        document.getElementById("btnCrearEquipo").style.display = estoyEnEquipo ? "none" : "";
        if (tarea.es_equipo) {
            document.getElementById("btnSolicitarProrroga").style.display = estoyEnEquipo ? "" : "none";
        }
    }
    if (equipos.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay equipos.</p>`;
        return;
    }
    const filas = equipos.map((equipo) => {
        const chips = equipo.integrantes.map((u) => {
            const quitar = esMaestro ? ` <button type="button" class="btn-cerrar" data-quitar-integrante="${u.id_usuario}" data-equipo-id="${equipo.id_equipo}" aria-label="Quitar ${Store.escaparHTML(u.nombre)} del equipo">X</button>` : "";
            return `<span class="chip">${Store.escaparHTML(u.nombre)}${quitar}</span>`;
        }).join(" ");
        const soyIntegrante = equipo.integrantes.some((u) => u.id_usuario === usuario.id_usuario);
        const estoyEnAlgunEquipo = equipos.some((e) => e.integrantes.some((u) => u.id_usuario === usuario.id_usuario));
        const acciones = esMaestro ? `
                <button type="button" class="btn btn-secundario btn-sm" data-agregar-integrante="${equipo.id_equipo}">Agregar integrante</button>
                <button type="button" class="btn btn-secundario btn-sm" data-editar-equipo="${equipo.id_equipo}">Editar</button>
                <button type="button" class="btn btn-peligro btn-sm" data-eliminar-equipo="${equipo.id_equipo}">Eliminar</button>` : (soyIntegrante
            ? `<button type="button" class="btn btn-peligro btn-sm" data-salir-equipo="${equipo.id_equipo}">Salirse</button>`
            : (!estoyEnAlgunEquipo
                ? `<button type="button" class="btn btn-primario btn-sm" data-unirse-equipo="${equipo.id_equipo}">Unirse</button>`
                : ""));
        return `
            <tr>
                <td data-campo="nombreEquipo">${Store.escaparHTML(equipo.nombre_equipo)}</td>
                <td data-campo="integrantes">${chips || "<span class=\"vacio\">Sin integrantes</span>"}</td>
                <td class="acciones">${acciones}</td>
            </tr>`;
    }).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/tareas/${tarea.id_tarea}/equipos">
                <thead><tr><th data-campo="nombreEquipo">Nombre</th><th data-campo="integrantes">Integrantes</th><th class="acciones">Acciones</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

// Prorrogas
async function renderProrrogas() {
    const contenedor = document.getElementById("listaProrrogas");
    const prorrogas = await Store.prorrogasDeTarea(tarea.id_tarea);
    prorrogasActuales = prorrogas;
    if (prorrogas.length === 0) {
        contenedor.innerHTML = `<p class="vacio">Aún no hay prórrogas.</p>`;
        return;
    }
    const filas = prorrogas.map((prorroga) => {
        const nombreAlumno = prorroga.alumno ? prorroga.alumno.nombre : "—";
        const nombreEquipo = prorroga.equipo ? prorroga.equipo.nombre_equipo : "—";
        let badge = `<span class="badge badge-ambar">Pendiente</span>`;
        if (prorroga.aprobada === true) badge = `<span class="badge badge-verde">Aprobada</span>`;
        if (prorroga.aprobada === false) badge = `<span class="badge badge-rojo">Rechazada</span>`;
        const botones = esMaestro && prorroga.aprobada === null ? `
            <button type="button" class="btn btn-primario btn-sm" data-accion-prorroga="aprobar" data-id="${prorroga.id_prorroga}">Aprobar</button>
            <button type="button" class="btn btn-peligro btn-sm" data-accion-prorroga="rechazar" data-id="${prorroga.id_prorroga}">Rechazar</button>` : "";
        const puedeBorrar = esMaestro || prorroga.id_alumno === usuario.id_usuario;
        const botonBorrar = puedeBorrar ? `
            <button type="button" class="btn btn-peligro btn-sm" data-eliminar-prorroga="${prorroga.id_prorroga}">Eliminar</button>` : "";
        return `
            <tr>
                <td data-campo="alumno.nombre">${Store.escaparHTML(nombreAlumno)}</td>
                <td data-campo="equipo.nombreEquipo">${Store.escaparHTML(nombreEquipo)}</td>
                <td data-campo="nuevaFechaLimite">${Store.formatearFecha(prorroga.nueva_fecha_limite)}</td>
                <td class="celda-texto" data-campo="justificacion">${prorroga.justificacion ? Store.escaparHTML(prorroga.justificacion) : "—"}</td>
                <td data-campo="aprobada">${badge}</td>
                <td class="acciones">${botones}${botonBorrar}</td>
            </tr>`;
    }).join("");
    contenedor.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/tareas/${tarea.id_tarea}/prorrogas">
                <thead><tr><th data-campo="alumno.nombre">Alumno</th><th data-campo="equipo.nombreEquipo">Equipo</th><th data-campo="nuevaFechaLimite">Nueva fecha límite</th><th data-campo="justificacion">Justificación</th><th data-campo="aprobada">Estado</th><th class="acciones">Acciones</th></tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

async function renderTodo() {
    await renderEntregas();
    await renderEquipos();
    await renderProrrogas();
}
await renderTodo();

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

// Entregar tarea
async function actualizarGrupoEquipo(grupoId, selectEquipoId) {
    const grupo = document.getElementById(grupoId);
    const selectEquipo = document.getElementById(selectEquipoId);
    if (!tarea.es_equipo) {
        grupo.hidden = true;
        selectEquipo.innerHTML = "";
        return;
    }
    const misEquipos = (await Store.equiposDeTarea(tarea.id_tarea)).filter((e) =>
        e.integrantes.some((u) => u.id_usuario === usuario.id_usuario)
    );
    selectEquipo.innerHTML = "";
    misEquipos.forEach((equipo) => {
        selectEquipo.insertAdjacentHTML("beforeend", `<option value="${equipo.id_equipo}">${Store.escaparHTML(equipo.nombre_equipo)}</option>`);
    });
    grupo.hidden = misEquipos.length === 0;
}

document.getElementById("btnEntregar").addEventListener("click", async () => {
    document.getElementById("entregaTareaId").value = tarea.id_tarea;
    await actualizarGrupoEquipo("grupoEquipoEntrega", "entregaEquipo");
    abrirDialogo("dialogoEntrega");
});

document.getElementById("formEntrega").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const archivo = document.getElementById("entregaArchivo");
    const archivoUrl = document.getElementById("entregaArchivoUrl").value.trim();
    const file = archivo.files.length > 0 ? archivo.files[0] : null;
    if (!file && !archivoUrl) {
        return;
    }
    const grupoEquipo = document.getElementById("grupoEquipoEntrega");
    const idEquipo = !grupoEquipo.hidden && document.getElementById("entregaEquipo").value
        ? Number(document.getElementById("entregaEquipo").value)
        : null;
    const resultado = await Store.entregarTarea({
        id_tarea: tarea.id_tarea,
        id_equipo: idEquipo,
        archivo: file,
        archivo_url: archivoUrl,
        descripcion: document.getElementById("entregaDescripcion").value.trim()
    });
    if (resultado && resultado.ok === false) {
        return;
    }
    await renderEntregas();
    evento.target.reset();
    cerrarDialogo("dialogoEntrega");
});

// Calificar entrega / editar y eliminar
document.getElementById("listaEntregas").addEventListener("click", async (evento) => {
    const botonVerDesc = evento.target.closest("[data-ver-descripcion]");
    if (botonVerDesc) {
        const entrega = entregasActuales.find((e) => e.id_entrega === Number(botonVerDesc.dataset.verDescripcion));
        if (!entrega) return;
        descripcionActualId = entrega.id_entrega;
        document.getElementById("descripcionTexto").textContent = entrega.descripcion || "";
        abrirDialogo("dialogoDescripcion");
        return;
    }
    const botonEditar = evento.target.closest("[data-editar-entrega]");
    if (botonEditar) {
        const entregas = await Store.entregasDeTarea(tarea.id_tarea);
        const entrega = entregas.find((e) => e.id_entrega === Number(botonEditar.dataset.editarEntrega));
        if (!entrega) return;
        document.getElementById("editarEntregaId").value = entrega.id_entrega;
        document.getElementById("editarEntregaArchivoUrl").value = entrega.archivo_url;
        document.getElementById("editarEntregaDescripcion").value = entrega.descripcion || "";
        abrirDialogo("dialogoEditarEntrega");
        return;
    }
    const botonEliminar = evento.target.closest("[data-eliminar-entrega]");
    if (botonEliminar) {
        const resultado = await Store.eliminarEntrega(Number(botonEliminar.dataset.eliminarEntrega));
        if (!resultado.ok) {
            return;
        }
        await renderEntregas();
        return;
    }
    const botonCalificar = evento.target.closest("[data-calificar-id]");
    if (!botonCalificar) return;
    entregaACalificarId = Number(botonCalificar.dataset.calificarId);
    const entregas = await Store.entregasDeTarea(tarea.id_tarea);
    const entrega = entregas.find((e) => e.id_entrega === entregaACalificarId);
    document.getElementById("calificarEntregaId").value = entregaACalificarId;
    document.getElementById("calificarPuntaje").value = entrega && entrega.calificacion !== null ? entrega.calificacion : "";
    document.getElementById("calificarComentario").value = entrega ? entrega.comentario_maestro : "";
    abrirDialogo("dialogoCalificar");
});

document.getElementById("formCalificar").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    await Store.calificarEntrega(
        entregaACalificarId,
        Number(document.getElementById("calificarPuntaje").value),
        document.getElementById("calificarComentario").value
    );
    await renderEntregas();
    evento.target.reset();
    cerrarDialogo("dialogoCalificar");
});

// Editar entrega
document.getElementById("formEditarEntrega").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const archivo = document.getElementById("editarEntregaArchivo");
    const archivoUrl = document.getElementById("editarEntregaArchivoUrl").value.trim();
    const file = archivo.files.length > 0 ? archivo.files[0] : null;
    if (!file && !archivoUrl) {
        return;
    }
    const resultado = await Store.actualizarEntrega(Number(document.getElementById("editarEntregaId").value), { archivo: file, archivo_url: archivoUrl, descripcion: document.getElementById("editarEntregaDescripcion").value.trim() });
    if (!resultado.ok) {
        return;
    }
    await renderEntregas();
    evento.target.reset();
    cerrarDialogo("dialogoEditarEntrega");
});

// Descargar la descripcion como .txt
document.getElementById("btnDescargarDescripcion").addEventListener("click", () => {
    const texto = document.getElementById("descripcionTexto").textContent || "";
    const blob = new Blob([texto], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "descripcion-entrega-" + (descripcionActualId || "") + ".txt";
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
});

// Crear equipo
async function opcionesAlumnos() {
    return (await Store.alumnosDeCurso(curso.id_curso)).map((a) =>
        `<option value="${a.id_usuario}">${Store.escaparHTML(a.nombre)}</option>`
    ).join("");
}

async function filaIntegrante() {
    return `
        <div class="fila-integrante">
            <select class="select-integrante">${await opcionesAlumnos()}</select>
            <button type="button" class="btn btn-secundario btn-sm btn-quitar-integrante">Quitar</button>
        </div>
    `;
}

async function resetearIntegrantes() {
    const contenedor = document.getElementById("contenedorIntegrantes");
    contenedor.innerHTML = await filaIntegrante();
}

document.getElementById("btnCrearEquipo").addEventListener("click", async () => {
    await resetearIntegrantes();
    abrirDialogo("dialogoEquipo");
});

document.getElementById("btnAnadirIntegrante").addEventListener("click", async () => {
    document.getElementById("contenedorIntegrantes").insertAdjacentHTML("beforeend", await filaIntegrante());
});

document.getElementById("contenedorIntegrantes").addEventListener("click", (evento) => {
    if (evento.target.classList.contains("btn-quitar-integrante")) {
        evento.target.closest(".fila-integrante").remove();
    }
});

document.getElementById("formEquipo").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const ids = Array.from(document.querySelectorAll(".select-integrante"))
        .map((select) => Number(select.value))
        .filter((id, i, arr) => !Number.isNaN(id) && arr.indexOf(id) === i);
    await Store.crearEquipo(tarea.id_tarea, document.getElementById("equipoNombre").value, ids);
    await renderEquipos();
    evento.target.reset();
    cerrarDialogo("dialogoEquipo");
});

// Equipos: editar, agregar/quitar integrante, eliminar
document.getElementById("listaEquipos").addEventListener("click", async (evento) => {
    const botonUnirse = evento.target.closest("[data-unirse-equipo]");
    if (botonUnirse) {
        await Store.agregarIntegrante(Number(botonUnirse.dataset.unirseEquipo), usuario.id_usuario);
        await renderEquipos();
        await renderEntregas();
        return;
    }
    const botonSalir = evento.target.closest("[data-salir-equipo]");
    if (botonSalir) {
        await Store.quitarIntegrante(Number(botonSalir.dataset.salirEquipo), usuario.id_usuario);
        await renderEquipos();
        await renderEntregas();
        return;
    }
    const botonQuitar = evento.target.closest("[data-quitar-integrante]");
    if (botonQuitar) {
        await Store.quitarIntegrante(Number(botonQuitar.dataset.equipoId), Number(botonQuitar.dataset.quitarIntegrante));
        await renderEquipos();
        return;
    }
    const botonAgregar = evento.target.closest("[data-agregar-integrante]");
    if (botonAgregar) {
        const idEquipo = Number(botonAgregar.dataset.agregarIntegrante);
        const equipos = await Store.equiposDeTarea(tarea.id_tarea);
        const equipo = equipos.find((e) => e.id_equipo === idEquipo);
        const idsActuales = equipo ? equipo.integrantes.map((u) => u.id_usuario) : [];
        const select = document.getElementById("agregarIntegranteAlumno");
        select.innerHTML = (await Store.alumnosDeCurso(curso.id_curso))
            .filter((a) => !idsActuales.includes(a.id_usuario))
            .map((a) => `<option value="${a.id_usuario}">${Store.escaparHTML(a.nombre)}</option>`)
            .join("");
        document.getElementById("agregarIntegranteEquipoId").value = idEquipo;
        abrirDialogo("dialogoAgregarIntegrante");
        return;
    }
    const botonEditar = evento.target.closest("[data-editar-equipo]");
    if (botonEditar) {
        const idEquipo = Number(botonEditar.dataset.editarEquipo);
        const equipos = await Store.equiposDeTarea(tarea.id_tarea);
        const equipo = equipos.find((e) => e.id_equipo === idEquipo);
        if (!equipo) return;
        document.getElementById("editarEquipoId").value = idEquipo;
        document.getElementById("editarEquipoNombre").value = equipo.nombre_equipo;
        abrirDialogo("dialogoEditarEquipo");
        return;
    }
    const botonEliminar = evento.target.closest("[data-eliminar-equipo]");
    if (botonEliminar) {
        const idEquipo = Number(botonEliminar.dataset.eliminarEquipo);
        await Store.eliminarEquipo(idEquipo);
        await renderTodo();
        return;
    }
});

document.getElementById("formEditarEquipo").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    await Store.actualizarEquipo(Number(document.getElementById("editarEquipoId").value), {
        nombre_equipo: document.getElementById("editarEquipoNombre").value
    });
    await renderEquipos();
    cerrarDialogo("dialogoEditarEquipo");
});

document.getElementById("formAgregarIntegrante").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const resultado = await Store.agregarIntegrante(
        Number(document.getElementById("agregarIntegranteEquipoId").value),
        Number(document.getElementById("agregarIntegranteAlumno").value)
    );
    if (!resultado.ok) {
        return;
    }
    await renderEquipos();
    cerrarDialogo("dialogoAgregarIntegrante");
});

// Solicitar prorroga
document.getElementById("btnSolicitarProrroga").addEventListener("click", async () => {
    document.getElementById("prorrogaTareaId").value = tarea.id_tarea;
    await actualizarGrupoEquipo("grupoEquipoProrroga", "prorrogaEquipo");
    abrirDialogo("dialogoProrroga");
});

document.getElementById("formProrroga").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const grupoEquipo = document.getElementById("grupoEquipoProrroga");
    const idEquipo = !grupoEquipo.hidden && document.getElementById("prorrogaEquipo").value
        ? Number(document.getElementById("prorrogaEquipo").value)
        : null;
    await Store.solicitarProrroga({
        id_tarea: tarea.id_tarea,
        id_equipo: idEquipo,
        nueva_fecha_limite: document.getElementById("prorrogaNuevaFecha").value,
        justificacion: document.getElementById("prorrogaJustificacion").value.trim()
    });
    await renderProrrogas();
    evento.target.reset();
    cerrarDialogo("dialogoProrroga");
});

// Prorrogas: aprobar, rechazar, eliminar
document.getElementById("listaProrrogas").addEventListener("click", async (evento) => {
    const botonAccion = evento.target.closest("[data-accion-prorroga]");
    if (botonAccion) {
        const aprobada = botonAccion.dataset.accionProrroga === "aprobar";
        const idProrroga = Number(botonAccion.dataset.id);
        const prorroga = prorrogasActuales.find((p) => p.id_prorroga === idProrroga);
        await Store.resolverProrroga(idProrroga, aprobada, prorroga ? prorroga.nueva_fecha_limite : undefined);
        await renderProrrogas();
        return;
    }
    const botonEliminar = evento.target.closest("[data-eliminar-prorroga]");
    if (botonEliminar) {
        const resultado = await Store.eliminarProrroga(Number(botonEliminar.dataset.eliminarProrroga));
        if (!resultado.ok) {
            return;
        }
        await renderProrrogas();
        return;
    }
});

// Editar tarea
document.getElementById("formEditarTarea").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    await Store.actualizarTarea(tarea.id_tarea, {
        titulo: document.getElementById("editarTareaTitulo").value,
        descripcion: document.getElementById("editarTareaDescripcion").value,
        fecha_programada: document.getElementById("editarTareaFechaProgramada").value,
        fecha_limite: document.getElementById("editarTareaFechaLimite").value,
        es_equipo: document.getElementById("editarTareaEsEquipo").checked
    });
    window.location.href = "tarea-detalle.html?id=" + tarea.id_tarea;
});
})();
