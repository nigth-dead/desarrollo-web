document.getElementById("formRegistro").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const nombre = document.getElementById("registroNombre").value;
    const email = document.getElementById("registroEmail").value;
    const password = document.getElementById("registroPassword").value;
    const password2 = document.getElementById("registroPassword2").value;
    const rol = document.getElementById("registroRol").value;

    if (password !== password2) {
        mostrarToast("Las contraseñas no coinciden.");
        return;
    }

    const resultado = await Store.registrarUsuario({ nombre, email, password, rol });

    if (!resultado.ok) {
        mostrarToast("No se pudo registrar: " + resultado.error);
        return;
    }

    mostrarToast("Usuario registrado exitosamente.");

    setTimeout(function () {
        window.location.href = "usuarios.html";
    }, 1500);
});
