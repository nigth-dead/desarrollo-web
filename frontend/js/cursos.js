const listaCursos = document.getElementById("listaCursos");
const usuario = Store.usuarioActual();
const esMaestro = Store.esMaestro();

if (!usuario) {
    window.location.href = "../index.html";
}

if (esMaestro) {
    document.getElementById("btnUnirseCodigo").style.display = "none";
} else {
    document.getElementById("btnCrearCurso").style.display = "none";
}

async function renderCursos() {
    const cursos = await Store.cursos();
    if (cursos.length === 0) {
        listaCursos.innerHTML = `<p class="vacio">Aún no tienes cursos. Crea uno o únete con un código de invitación.</p>`;
        return;
    }
    const filas = cursos.map((curso) => {
        const nombreMaestro = curso.maestro ? curso.maestro.nombre : "Sin maestro";
        return `
            <tr data-id="${curso.id_curso}">
                <td data-campo="nombre">${Store.escaparHTML(curso.nombre)}</td>
                <td class="celda-texto" data-campo="descripcion">${Store.escaparHTML(curso.descripcion)}</td>
                <td data-campo="maestro.nombre">${Store.escaparHTML(nombreMaestro)}</td>
                <td data-campo="codigoInvitacion"><span class="badge badge-azul">${Store.escaparHTML(curso.codigo_invitacion)}</span></td>
                <td class="acciones">
                    <a class="btn btn-primario btn-sm" href="curso-detalle.html?id=${curso.id_curso}">Entrar</a>
                    ${esMaestro ? `<button type="button" class="btn btn-secundario btn-sm" data-accion="editar" data-id="${curso.id_curso}">Editar</button>` : ""}
                    ${esMaestro ? `<button type="button" class="btn btn-peligro btn-sm" data-accion="eliminar" data-id="${curso.id_curso}">Eliminar</button>` : ""}
                </td>
            </tr>`;
    }).join("");
    listaCursos.innerHTML = `
        <div class="tabla-contenedor">
            <table class="tabla" data-endpoint="/api/cursos">
                <thead><tr>
                    <th data-campo="nombre">Nombre</th>
                    <th data-campo="descripcion">Descripción</th>
                    <th data-campo="maestro.nombre">Maestro</th>
                    <th data-campo="codigoInvitacion">Código</th>
                    <th class="acciones">Acciones</th>
                </tr></thead>
                <tbody>${filas}</tbody>
            </table>
        </div>`;
}

renderCursos();

// Diálogos
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

// Botones para abrir los diálogos
document.getElementById("btnCrearCurso").addEventListener("click", () => {
    abrirDialogo("dialogCrear");
});

document.getElementById("btnUnirseCodigo").addEventListener("click", () => {
    abrirDialogo("dialogUnirse");
});

// Editar y eliminar
listaCursos.addEventListener("click", async (evento) => {
    const botonEditar = evento.target.closest('[data-accion="editar"]');
    if (botonEditar) {
        const curso = await Store.cursoPorId(Number(botonEditar.dataset.id));
        if (!curso) return;
        document.getElementById("editarId").value = curso.id_curso;
        document.getElementById("editarNombre").value = curso.nombre;
        document.getElementById("editarDescripcion").value = curso.descripcion;
        abrirDialogo("dialogEditar");
        return;
    }
    const botonEliminar = evento.target.closest('[data-accion="eliminar"]');
    if (botonEliminar) {
        const curso = await Store.cursoPorId(Number(botonEliminar.dataset.id));
        if (!curso) return;
        await Store.eliminarCurso(curso.id_curso);
        await renderCursos();
        return;
    }
});

// Crear curso
document.getElementById("formCrearCurso").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const nombre = document.getElementById("crearNombre").value;
    const descripcion = document.getElementById("crearDescripcion").value;
    await Store.crearCurso({ nombre, descripcion });
    await renderCursos();
    evento.target.reset();
    cerrarDialogo("dialogCrear");
});

// Editar curso
document.getElementById("formEditarCurso").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const id = Number(document.getElementById("editarId").value);
    await Store.actualizarCurso(id, {
        nombre: document.getElementById("editarNombre").value,
        descripcion: document.getElementById("editarDescripcion").value
    });
    await renderCursos();
    cerrarDialogo("dialogEditar");
});

// Unirse con codigo
document.getElementById("formUnirseCodigo").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const codigo = document.getElementById("codigoInvitacion").value;
    const resultado = await Store.unirseConCodigo(codigo);
    if (!resultado.ok) {
        return;
    }
    await renderCursos();
    evento.target.reset();
    cerrarDialogo("dialogUnirse");
});
