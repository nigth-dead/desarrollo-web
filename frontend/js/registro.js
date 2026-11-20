document.getElementById("formRegistro").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const nombre = document.getElementById("registroNombre").value;
    const email = document.getElementById("registroEmail").value;
    const password = document.getElementById("registroPassword").value;
    const password2 = document.getElementById("registroPassword2").value;
    const rol = document.getElementById("registroRol").value;

    if (password !== password2) {
        return;
    }

    const resultado = await Store.registrarUsuario({ nombre, email, password, rol });
    if (resultado.ok) {
        window.location.href = "usuarios.html";
    }
});
