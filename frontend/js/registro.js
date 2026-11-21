document.getElementById("formRegistro").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const form = evento.target;
    const V = window.Validacion;
    V.limpiarError(form);

    const nombreInput = document.getElementById("registroNombre");
    const emailInput = document.getElementById("registroEmail");
    const passwordInput = document.getElementById("registroPassword");
    const password2Input = document.getElementById("registroPassword2");
    const nombre = nombreInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const password2 = password2Input.value;
    const rol = document.getElementById("registroRol").value;

    if (nombre.length < 2) {
        V.marcarCampo(nombreInput, "El nombre debe tener al menos 2 caracteres.");
        V.mostrarError(form, "Revisa el nombre: debe tener al menos 2 caracteres.");
        return;
    }
    if (!V.esEmail(email)) {
        V.marcarCampo(emailInput, "Escribe un correo válido, por ejemplo tucorreo@uv.mx.");
        V.mostrarError(form, "Revisa el correo: debe tener un formato válido.");
        return;
    }
    if (password.length < 6) {
        V.marcarCampo(passwordInput, "La contraseña debe tener al menos 6 caracteres.");
        V.mostrarError(form, "La contraseña debe tener al menos 6 caracteres.");
        return;
    }
    if (password !== password2) {
        V.marcarCampo(password2Input, "Las contraseñas no coinciden.");
        V.mostrarError(form, "Las contraseñas no coinciden. Escríbelas de nuevo.");
        return;
    }

    const resultado = await Store.registrarUsuario({ nombre, email, password, rol });
    if (!V.siFalloMostrar(form, resultado, "No se pudo registrar el usuario. Inténtalo de nuevo.")) {
        return;
    }
    // Cambio del repo (Héctor, commit 77606d9, 21/11/2026):
    // mensaje de éxito "Usuario registrado exitosamente." y espera
    // 1.5 s antes de redirigir, para que el toast sí se vea.
    V.mostrarExito("Usuario registrado exitosamente.");
    setTimeout(function () {
        window.location.href = "usuarios.html";
    }, 1500);
});
