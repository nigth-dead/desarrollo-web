(function () {
    const usuario = Store.usuarioActual();
    if (!usuario || usuario.rol !== "ADMIN") return;

    const lista = document.getElementById("listaUsuarios");
    const dialogo = document.getElementById("dialogoMasiva");
    const form = document.getElementById("formMasiva");
    const archivoInput = document.getElementById("archivoMasiva");
    const nombreArchivo = document.getElementById("nombreArchivoMasiva");

    async function renderUsuarios() {
        const usuarios = await Store.usuarios();
        if (usuarios.length === 0) {
            lista.innerHTML = `<p class="vacio">Aún no hay usuarios.</p>`;
            return;
        }
        const filas = usuarios.map((u) => {
            const badge = u.rol === "ADMIN" ? "badge-rojo" : (u.rol === "MAESTRO" ? "badge-azul" : "badge-verde");
            return `
                <tr>
                    <td data-campo="nombre">${Store.escaparHTML(u.nombre)}</td>
                    <td data-campo="email">${Store.escaparHTML(u.email)}</td>
                    <td data-campo="rol"><span class="badge ${badge}">${u.rol}</span></td>
                    <td data-campo="fechaRegistro">${Store.formatearFecha(u.fecha_registro)}</td>
                    <td class="acciones">
                        <button type="button" class="btn btn-secundario btn-sm" data-editar-id="${u.id_usuario}">Editar</button>
                        ${u.id_usuario === usuario.id_usuario ? "" : `<button type="button" class="btn btn-peligro btn-sm" data-eliminar-id="${u.id_usuario}">Eliminar</button>`}
                    </td>
                </tr>`;
        }).join("");
        lista.innerHTML = `
            <div class="tabla-contenedor">
                <table class="tabla" data-endpoint="/api/usuarios">
                    <thead><tr><th data-campo="nombre">Nombre</th><th data-campo="email">Correo</th><th data-campo="rol">Rol</th><th data-campo="fechaRegistro">Registrado</th><th class="acciones">Acciones</th></tr></thead>
                    <tbody>${filas}</tbody>
                </table>
            </div>`;
    }
    renderUsuarios();

    // Editar usuario
    const dialogoEditar = document.getElementById("dialogoEditarUsuario");
    const formEditar = document.getElementById("formEditarUsuario");

    lista.addEventListener("click", async (evento) => {
        const botonEliminar = evento.target.closest("[data-eliminar-id]");
        if (botonEliminar) {
            const id = Number(botonEliminar.dataset.eliminarId);
            const confirmado = await window.Validacion.confirmar(
                "¿Eliminar este usuario? Esta acción no se puede deshacer.", "Eliminar");
            if (!confirmado) return;
            const resultado = await Store.eliminarUsuario(id);
            if (!resultado.ok) {
                window.mostrarToast(resultado.error || "No se pudo eliminar el usuario.");
                return;
            }
            window.Validacion.mostrarExito("Usuario eliminado.");
            await renderUsuarios();
            return;
        }
        const boton = evento.target.closest("[data-editar-id]");
        if (!boton) return;
        const usuarioEditar = await Store.usuarioPorId(Number(boton.dataset.editarId));
        if (!usuarioEditar) return;
        const esYo = usuarioEditar.id_usuario === usuario.id_usuario;
        document.getElementById("editarUsuarioId").value = usuarioEditar.id_usuario;
        document.getElementById("editarUsuarioNombre").value = usuarioEditar.nombre;
        document.getElementById("editarUsuarioEmail").value = usuarioEditar.email;
        document.getElementById("editarUsuarioPassword").value = "";
        document.getElementById("editarUsuarioRol").value = usuarioEditar.rol;
        document.getElementById("editarUsuarioRol").disabled = esYo;
        document.getElementById("notaRolPropio").hidden = !esYo;
        dialogoEditar.showModal();
    });

    dialogoEditar.querySelectorAll("[data-cerrar]").forEach((b) => b.addEventListener("click", () => dialogoEditar.close()));
    dialogoEditar.addEventListener("click", (evento) => { if (evento.target === dialogoEditar) dialogoEditar.close(); });

    formEditar.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const V = window.Validacion;
        V.limpiarError(formEditar);
        const nombreInput = document.getElementById("editarUsuarioNombre");
        const emailInput = document.getElementById("editarUsuarioEmail");
        const passwordInput = document.getElementById("editarUsuarioPassword");
        const nombre = nombreInput.value.trim();
        const email = emailInput.value.trim();
        const password = passwordInput.value;
        if (nombre.length < 2) {
            V.marcarCampo(nombreInput, "El nombre debe tener al menos 2 caracteres.");
            V.mostrarError(formEditar, "Revisa el nombre: debe tener al menos 2 caracteres.");
            return;
        }
        if (!V.esEmail(email)) {
            V.marcarCampo(emailInput, "Escribe un correo válido.");
            V.mostrarError(formEditar, "Revisa el correo: debe tener un formato válido.");
            return;
        }
        if (password && password.length < 6) {
            V.marcarCampo(passwordInput, "La contraseña nueva debe tener al menos 6 caracteres.");
            V.mostrarError(formEditar, "La contraseña nueva debe tener al menos 6 caracteres.");
            return;
        }
        const id = Number(document.getElementById("editarUsuarioId").value);
        const resultado = await Store.actualizarUsuario(id, {
            nombre: nombre,
            email: email,
            password: password,
            rol: document.getElementById("editarUsuarioRol").value
        });
        if (!V.siFalloMostrar(formEditar, resultado, "No se pudo guardar los cambios del usuario. Inténtalo de nuevo.")) {
            return;
        }
        dialogoEditar.close();
        await renderUsuarios();
        V.mostrarExito("Usuario actualizado.");
    });

    // Diálogo de carga masiva
    document.getElementById("btnAbrirMasiva").addEventListener("click", () => {
        archivoInput.value = "";
        nombreArchivo.textContent = "";
        dialogo.showModal();
    });
    dialogo.querySelectorAll("[data-cerrar]").forEach((b) => b.addEventListener("click", () => dialogo.close()));
    dialogo.addEventListener("click", (evento) => { if (evento.target === dialogo) dialogo.close(); });

    archivoInput.addEventListener("change", () => {
        const archivo = archivoInput.files[0];
        nombreArchivo.textContent = archivo ? "Archivo elegido: " + archivo.name : "";
    });

    function limpiar(valor) {
        return String(valor || "").trim().replace(/^"|"$/g, "");
    }

    function normalizarEncabezado(texto) {
        return String(texto || "").trim().toLowerCase()
            .normalize("NFD").replace(/[̀-ͯ]/g, "");
    }

    // Descargar plantilla CSV de ejemplo
    document.getElementById("btnPlantilla").addEventListener("click", () => {
        const csv = "nombre,email,password,rol\nNombre Apellido,correo@uv.mx,contraseña,ALUMNO\nOtro Usuario,otro@uv.mx,contraseña,MAESTRO\n";
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
        const a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = "plantilla-usuarios.csv";
        a.click();
        URL.revokeObjectURL(a.href);
    });

    async function importarFilas(filas) {
        let creados = 0;
        const errores = [];
        for (let i = 0; i < filas.length; i++) {
            const partes = filas[i];
            const [nombre, email, password, rol] = partes.map(limpiar);
            if (!nombre || !email) {
                errores.push(`Fila ${i + 1}: faltan datos.`);
                continue;
            }
            if (nombre.length < 2) {
                errores.push(`Fila ${i + 1} (${email}): el nombre debe tener al menos 2 caracteres.`);
                continue;
            }
            if (!window.Validacion.esEmail(email)) {
                errores.push(`Fila ${i + 1} (${email}): correo no válido.`);
                continue;
            }
            if (rol && !["ALUMNO", "MAESTRO", "ADMIN"].includes(rol.toUpperCase())) {
                errores.push(`Fila ${i + 1} (${email}): rol no válido (usa ALUMNO, MAESTRO o ADMIN).`);
                continue;
            }
            // Sin contraseña no se crea la cuenta (antes se ponía "demo" a todas, una contraseña adivinable)
            if (!password) {
                errores.push(`Fila ${i + 1} (${email}): falta la contraseña.`);
                continue;
            }
            if (password.length < 6) {
                errores.push(`Fila ${i + 1} (${email}): la contraseña debe tener al menos 6 caracteres.`);
                continue;
            }
            const resultado = await Store.registrarUsuario({ nombre, email, password, rol: rol ? rol.toUpperCase() : rol });
            if (resultado.ok) creados++;
            else errores.push(`Fila ${i + 1} (${email}): ${resultado.error}`);
        }
        return { creados, errores };
    }

    async function mostrarResumen(creados, errores) {
        await renderUsuarios();
        let resumen = document.getElementById("resumenMasiva");
        if (!resumen) {
            resumen = document.createElement("div");
            resumen.id = "resumenMasiva";
            form.querySelector(".dialogo-cuerpo").appendChild(resumen);
        }
        const conError = (errores || []).length;
        resumen.innerHTML = "<p><strong>" + creados + " creados, " + conError + " con error.</strong></p>" +
            (conError > 0
                ? "<ul>" + errores.map((e) => "<li>" + Store.escaparHTML(e) + "</li>").join("") + "</ul>"
                : "<p>Todos los usuarios se crearon sin errores.</p>");
        if (conError === 0 && creados > 0) window.Validacion.mostrarExito("Se crearon " + creados + " usuarios.");
    }

    async function procesarCSV(texto) {
        const lineas = String(texto).split(/\r?\n/).map((l) => l.trim()).filter((l) => l !== "");
        const filas = [];
        lineas.forEach((linea, i) => {
            const partes = linea.split(",").map(limpiar);
            if (i === 0 && partes[1] && ["email", "correo"].includes(partes[1].toLowerCase())) return;
            filas.push(partes);
        });
        const { creados, errores } = await importarFilas(filas);
        await mostrarResumen(creados, errores);
    }

    async function procesarExcel(buffer) {
        if (typeof XLSX === "undefined") {
            window.Validacion.mostrarError(form, "No se pudo cargar el lector de Excel (SheetJS). Guarda el archivo como CSV e inténtalo de nuevo.");
            return;
        }
        const libro = XLSX.read(buffer, { type: "array" });
        const hoja = libro.Sheets[libro.SheetNames[0]];
        const filasCrudas = XLSX.utils.sheet_to_json(hoja, { header: 1, defval: "" });
        let idx = { nombre: 0, email: 1, password: 2, rol: 3 };
        const filas = [];
        filasCrudas.forEach((fila, i) => {
            const celdas = (fila || []).map((c) => String(c == null ? "" : c));
            if (i === 0) {
                const encabezados = celdas.map(normalizarEncabezado);
                const buscar = (...nombres) => {
                    for (const n of nombres) { const j = encabezados.indexOf(n); if (j >= 0) return j; }
                    return -1;
                };
                const eNombre = buscar("nombre");
                const eEmail = buscar("email", "correo");
                const ePass = buscar("contrasena", "password");
                const eRol = buscar("rol");
                if (eEmail >= 0) {
                    if (eNombre >= 0) idx.nombre = eNombre;
                    idx.email = eEmail;
                    if (ePass >= 0) idx.password = ePass;
                    if (eRol >= 0) idx.rol = eRol;
                    return;
                }
            }
            if (celdas.every((c) => String(c).trim() === "")) return;
            filas.push([celdas[idx.nombre] || "", celdas[idx.email] || "", celdas[idx.password] || "", celdas[idx.rol] || ""]);
        });
        const { creados, errores } = await importarFilas(filas);
        await mostrarResumen(creados, errores);
    }

    form.addEventListener("submit", (evento) => {
        evento.preventDefault();
        window.Validacion.limpiarError(form);
        const archivo = archivoInput.files[0];
        if (!archivo) {
            window.Validacion.mostrarError(form, "Selecciona un archivo .xlsx, .xls o .csv para importar.");
            return;
        }
        if (!/\.(xlsx|xls|csv)$/i.test(archivo.name)) {
            window.Validacion.mostrarError(form, "El archivo debe ser .xlsx, .xls o .csv.");
            return;
        }
        const nombre = archivo.name.toLowerCase();
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            archivo.arrayBuffer().then(procesarExcel);
            return;
        }
        const lector = new FileReader();
        lector.onload = () => procesarCSV(String(lector.result));
        lector.readAsText(archivo);
    });
})();
