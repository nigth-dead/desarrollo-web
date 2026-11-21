(function () {
  var toastEl = document.createElement("div");
  toastEl.className = "toast";
  toastEl.hidden = true;
  document.body.appendChild(toastEl);
  var toastTimer = null;
  window.mostrarToast = function (mensaje) {
    toastEl.textContent = mensaje;
    toastEl.hidden = false;
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { toastEl.hidden = true; }, 3500);
  };

  var dialogoConfirmar = document.createElement("dialog");
  dialogoConfirmar.className = "dialogo";
  dialogoConfirmar.innerHTML = `
    <form method="dialog">
      <div class="dialogo-encabezado">
        <h3>Confirmar</h3>
        <button type="button" class="btn-cerrar" data-cerrar-confirmar aria-label="Cerrar">X</button>
      </div>
      <div class="dialogo-cuerpo"><p id="textoConfirmar"></p></div>
      <div class="dialogo-pie">
        <button type="button" class="btn btn-secundario" data-cerrar-confirmar>Cancelar</button>
        <button type="submit" class="btn btn-peligro" id="btnConfirmarSi">Eliminar</button>
      </div>
    </form>`;
  document.body.appendChild(dialogoConfirmar);
  var resolverConfirmacion = null;
  function cerrarConfirmacion(valor) {
    if (resolverConfirmacion) {
      var resolver = resolverConfirmacion;
      resolverConfirmacion = null;
      resolver(valor);
    }
    if (dialogoConfirmar.open) dialogoConfirmar.close();
  }
  window.pedirConfirmacion = function (texto) {
    document.getElementById("textoConfirmar").textContent = texto;
    dialogoConfirmar.showModal();
    return new Promise(function (resolver) { resolverConfirmacion = resolver; });
  };
  dialogoConfirmar.addEventListener("click", function (evento) {
    if (evento.target === dialogoConfirmar) cerrarConfirmacion(false);
  });
  dialogoConfirmar.querySelectorAll("[data-cerrar-confirmar]").forEach(function (b) {
    b.addEventListener("click", function () { cerrarConfirmacion(false); });
  });
  document.getElementById("btnConfirmarSi").addEventListener("click", function () { cerrarConfirmacion(true); });

  var EN_VISTAS = window.location.pathname.indexOf("/HTML/") !== -1;
  var A_RAIZ = EN_VISTAS ? "../" : "";
  var A_VISTAS = EN_VISTAS ? "" : "HTML/";
  var RUTA_LOGIN = A_RAIZ + "index.html";
  var RUTA_CURSOS = A_VISTAS + "cursos.html";
  var RUTA_REGISTRO = A_VISTAS + "registro.html";
  var RUTA_USUARIOS = A_VISTAS + "usuarios.html";
  var RUTA_INDEX = A_RAIZ + "index.html";

  function esPaginaPublica() {
    var ruta = window.location.pathname;
    return ruta === "/" || ruta.endsWith("/index.html");
  }

  function esPaginaAuth() {
    var ruta = window.location.pathname;
    return ruta === "/" || ruta.endsWith("/index.html");
  }

  function esPaginaAdmin() {
    var ruta = window.location.pathname;
    return ruta === "/" || ruta.endsWith("/index.html") || ruta.endsWith("/usuarios.html") || ruta.endsWith("/registro.html");
  }

  function ir(url) {
    window.location.href = url;
  }

  var usuario = Store.usuarioActual();

  Store.revisarExamenesCerrados();

  if (!usuario && !esPaginaPublica()) {
    ir(RUTA_LOGIN);
    return;
  }
  if (usuario && esPaginaAuth()) {
    ir(usuario.rol === "ADMIN" ? RUTA_USUARIOS : RUTA_CURSOS);
    return;
  }

  if (window.location.pathname.endsWith("/registro.html") && (!usuario || usuario.rol !== "ADMIN")) {
    ir(usuario ? RUTA_CURSOS : RUTA_LOGIN);
    return;
  }

  if (usuario && usuario.rol === "ADMIN" && !esPaginaAdmin()) {
    ir(RUTA_USUARIOS);
    return;
  }

  var encabezado = document.getElementById("encabezadoSistema");
  if (!encabezado) return;

  var rutaActual = window.location.pathname;
  function activo(ruta) {
    return rutaActual.endsWith(ruta) ? " activo" : "";
  }

  var enlaces = "";
  if (usuario) {
    if (usuario.rol === "ADMIN") {
      enlaces = `
      <nav class="navegacion-principal">
        <a href="${RUTA_USUARIOS}" class="${activo("/usuarios.html")}">Usuarios</a>
      </nav>`;
    } else {
      enlaces = `
      <nav class="navegacion-principal">
        <a href="${RUTA_CURSOS}" class="${activo("/cursos.html")}">Mis cursos</a>
        <a href="${A_VISTAS}notificaciones.html" class="${activo("/notificaciones.html")}">Notificaciones</a>
      </nav>`;
    }
  }

  var derecha = "";
  if (usuario) {
    derecha = `
      <div class="encabezado-derecha">
        <span class="usuario-actual"><strong>${Store.escaparHTML(usuario.nombre)}</strong> · ${usuario.rol}</span>
        <button type="button" class="btn btn-secundario btn-sm" id="btnSalir">Salir</button>
      </div>`;
  } else {
    derecha = `
      <div class="encabezado-derecha">
        <a class="btn btn-primario btn-sm" href="${RUTA_LOGIN}">Iniciar sesión</a>
      </div>`;
  }

  encabezado.innerHTML = `
    <div class="encabezado-sistema-interno">
      <a class="marca" href="${usuario ? (usuario.rol === "ADMIN" ? RUTA_USUARIOS : RUTA_CURSOS) : RUTA_INDEX}"><span class="marca-logo">L</span>LMS · Desarrollo Web</a>
      ${enlaces}
      ${derecha}
    </div>`;

  // Cerrar sesión
  var btnSalir = document.getElementById("btnSalir");
  if (btnSalir) {
    btnSalir.addEventListener("click", function () {
      Store.cerrarSesion();
      ir(RUTA_INDEX);
    });
  }

})();
