/* validacion.js — validaciones y mensajes del frontend (LMS desarrollo-web)
 * Nuevo archivo compartido: frontend/js/validacion.js
 * Cargar DESPUÉS de nav.js (usa mostrarToast / pedirConfirmacion) y ANTES
 * del script de cada página, en los 11 HTML (index.html + HTML/*.html).
 * No toca backend ni store.js: solo lee lo que Store ya devuelve
 * ({ok, error} o null cuando falla) y lo muestra al usuario.
 * Regla general: si una validación falla, se muestra el error en línea,
 * se marca el campo y NO se cierra el modal ni se redirige; el usuario
 * corrige y reintenta sin perder lo escrito.
 * Estilo del proyecto: IIFE, var/function, sin módulos ni dependencias.
 */
(function () {
  "use strict";

  function buscarError(form) {
    return form ? form.querySelector(".alerta-error") : null;
  }

  function crearError(form) {
    var p = document.createElement("p");
    p.className = "alerta-error";
    p.setAttribute("role", "alert");
    p.hidden = true;
    form.insertBefore(p, form.firstChild);
    return p;
  }

  function mostrarError(form, mensaje) {
    var el = buscarError(form) || crearError(form);
    el.textContent = mensaje || "Ocurrió un error. Inténtalo de nuevo.";
    el.hidden = false;
    if (el.scrollIntoView) el.scrollIntoView({ block: "nearest" });
    return false;
  }

  function limpiarError(form) {
    var el = buscarError(form);
    if (el) { el.textContent = ""; el.hidden = true; }
  }

  function mostrarExito(mensaje) {
    if (typeof window.mostrarToast === "function") window.mostrarToast(mensaje);
  }

  /* Normaliza las dos formas de fallo que usa Store:
   *  - oOk(): { ok: false, error: "..." }
   *  - oNull(): null (crearCurso, crearTarea, crearExamen, calificarEntrega...)
   * Devuelve { ok: true } o { ok: false, error }.
   */
  function normalizar(resultado, mensajeGenerico) {
    if (resultado && resultado.ok === false) {
      return { ok: false, error: resultado.error || mensajeGenerico };
    }
    if (resultado === null || resultado === undefined) {
      return { ok: false, error: mensajeGenerico };
    }
    return { ok: true, valor: resultado };
  }

  /* Muestra el error en el formulario si falló; true si todo bien. */
  function siFalloMostrar(form, resultado, mensajeGenerico) {
    var n = normalizar(resultado, mensajeGenerico);
    if (!n.ok) mostrarError(form, n.error);
    return n.ok;
  }

  function marcarCampo(input, mensaje) {
    if (!input) return;
    input.setAttribute("aria-invalid", "true");
    input.classList.add("campo-invalido");
    var viejo = input.parentNode.querySelector(".campo-error");
    if (viejo) viejo.remove();
    if (mensaje) {
      var s = document.createElement("small");
      s.className = "campo-error";
      s.textContent = mensaje;
      input.parentNode.insertBefore(s, input.nextSibling);
    }
    input.addEventListener("input", function limpiar() {
      input.removeAttribute("aria-invalid");
      input.classList.remove("campo-invalido");
      var e = input.parentNode.querySelector(".campo-error");
      if (e) e.remove();
      input.removeEventListener("input", limpiar);
    });
  }

  function esEmail(v) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(String(v || "").trim());
  }

  function esUrlHttp(v) {
    var t = String(v || "").trim();
    return t === "" || /^https?:\/\/.+\..+/.test(t);
  }

  /* fechaFin debe ser posterior a fechaInicio y ninguna puede ser pasada. */
  function validarRangoFechas(inicio, fin, nombreInicio, nombreFin) {
    var a = inicio ? new Date(inicio) : null;
    var b = fin ? new Date(fin) : null;
    var ahora = new Date();
    ahora.setSeconds(0, 0);
    if (!a || isNaN(a.getTime())) return "Elige una fecha de " + nombreInicio + " válida.";
    if (!b || isNaN(b.getTime())) return "Elige una fecha de " + nombreFin + " válida.";
    if (a.getTime() < ahora.getTime()) return "La fecha de " + nombreInicio + " no puede ser pasada.";
    if (b.getTime() <= a.getTime()) return "La fecha de " + nombreFin + " debe ser posterior a la de " + nombreInicio + ".";
    return null;
  }

  /* Prórroga: la nueva fecha debe ser futura y posterior al límite actual.
   * Devuelve el mensaje de error o null si es válida. */
  function validarProrroga(nueva, limiteActual) {
    var n = nueva ? new Date(nueva) : null;
    var ahora = new Date();
    ahora.setSeconds(0, 0);
    if (!n || isNaN(n.getTime())) return "Elige una nueva fecha límite válida.";
    if (n.getTime() < ahora.getTime()) return "La nueva fecha límite no puede ser una fecha pasada. Elige una fecha futura.";
    var limite = limiteActual ? new Date(limiteActual) : null;
    if (limite && !isNaN(limite.getTime()) && n.getTime() <= limite.getTime()) {
      return "La nueva fecha límite debe ser posterior a la fecha límite actual.";
    }
    return null;
  }

  function confirmar(texto, etiquetaBoton) {
    if (typeof window.pedirConfirmacion === "function") {
      return window.pedirConfirmacion(texto, etiquetaBoton || "Confirmar");
    }
    return Promise.resolve(false);
  }

  window.Validacion = {
    mostrarError: mostrarError,
    limpiarError: limpiarError,
    mostrarExito: mostrarExito,
    normalizar: normalizar,
    siFalloMostrar: siFalloMostrar,
    marcarCampo: marcarCampo,
    esEmail: esEmail,
    esUrlHttp: esUrlHttp,
    validarRangoFechas: validarRangoFechas,
    validarProrroga: validarProrroga,
    confirmar: confirmar
  };
})();
