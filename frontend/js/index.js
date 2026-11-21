(function () {
  var usuario = Store.usuarioActual();

  if (usuario) {
    window.location.href = usuario.rol === "ADMIN" ? "HTML/usuarios.html" : "HTML/cursos.html";
    return;
  }

  document.getElementById("formLogin").addEventListener("submit", async function (evento) {
    evento.preventDefault();
    var form = evento.target;
    var V = window.Validacion;
    V.limpiarError(form);

    var emailInput = document.getElementById("loginEmail");
    var passwordInput = document.getElementById("loginPassword");
    var email = emailInput.value.trim();
    var password = passwordInput.value;

    if (!V.esEmail(email)) {
      V.marcarCampo(emailInput, "Escribe un correo válido, por ejemplo tucorreo@uv.mx.");
      V.mostrarError(form, "Revisa el correo: debe tener un formato válido.");
      return;
    }
    if (!password) {
      V.marcarCampo(passwordInput, "Escribe tu contraseña.");
      V.mostrarError(form, "Escribe tu contraseña para iniciar sesión.");
      return;
    }

    var resultado = await Store.iniciarSesion(email, password);
    if (!V.siFalloMostrar(form, resultado, "Correo o contraseña incorrectos. Inténtalo de nuevo.")) {
      return;
    }
    window.location.href = resultado.usuario.rol === "ADMIN" ? "HTML/usuarios.html" : "HTML/cursos.html";
  });
})();
