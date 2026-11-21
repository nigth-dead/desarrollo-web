(function () {
  var usuario = Store.usuarioActual();

  if (usuario) {
    window.location.href = usuario.rol === "ADMIN" ? "HTML/usuarios.html" : "HTML/cursos.html";
    return;
  }

  document.getElementById("formLogin").addEventListener("submit", async function (evento) {
    evento.preventDefault();

    var email = document.getElementById("loginEmail").value;
    var password = document.getElementById("loginPassword").value;

    var resultado = await Store.iniciarSesion(email, password);
    if (resultado.ok) {
      window.location.href = resultado.usuario.rol === "ADMIN" ? "HTML/usuarios.html" : "HTML/cursos.html";
    }
  });
})();
