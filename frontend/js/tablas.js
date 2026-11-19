/*
 * tablas.js - Llena con Fetch las tablas del LMS.
 *
 * Cada <table data-endpoint="/api/..."> se llena sola:
 * - Las columnas salen de los <th data-campo="...">. Se aceptan campos anidados: "maestro.nombre".
 * - Los {parametros} del endpoint se toman de la URL de la pagina: tareas.html?idCurso=1
 *   Si falta alguno, se muestra un campo para escribirlo.
 * - Los demas parametros de la URL se mandan como filtros: tareas.html?idCurso=1&estado=pendiente
 * - data-lista="respuestas" indica que la lista viene dentro de ese campo de la respuesta.
 *
 * Requiere api.js. Si no hay sesion, manda a login.html.
 */
(function () {
  'use strict';

  // carpeta raiz del frontend (donde esta login.html), calculada a partir de este script
  const RAIZ = new URL('..', document.currentScript.src).href;

  // ===== Formato de valores =====
  const FECHA = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/;

  function leerCampo(objeto, ruta) {
    return ruta.split('.').reduce((o, k) => (o == null ? undefined : o[k]), objeto);
  }

  function formatear(valor) {
    if (valor === null || valor === undefined || valor === '') {
      return '—';
    }
    if (typeof valor === 'boolean') {
      return valor ? 'Sí' : 'No';
    }
    if (Array.isArray(valor)) {
      // ej. integrantes de un equipo: se muestran sus nombres
      return valor.length === 0 ? '—'
        : valor.map((v) => (v && typeof v === 'object' ? v.nombre || v.titulo || JSON.stringify(v) : v)).join(', ');
    }
    if (typeof valor === 'object') {
      return valor.nombre || valor.titulo || JSON.stringify(valor);
    }
    if (typeof valor === 'string' && FECHA.test(valor)) {
      const f = new Date(valor);
      if (!isNaN(f)) {
        return f.toLocaleString('es-MX', { dateStyle: 'short', timeStyle: 'short' });
      }
    }
    return String(valor);
  }

  // ===== Mensajes dentro de la tabla =====
  function filaMensaje(tbody, columnas, texto, clase) {
    tbody.innerHTML = '';
    const tr = document.createElement('tr');
    tr.className = clase;
    const td = document.createElement('td');
    td.colSpan = columnas;
    td.textContent = texto;
    tr.appendChild(td);
    tbody.appendChild(tr);
  }

  function pintarFilas(tbody, campos, lista) {
    tbody.innerHTML = '';
    for (const item of lista) {
      const tr = document.createElement('tr');
      for (const campo of campos) {
        const td = document.createElement('td');
        td.textContent = formatear(leerCampo(item, campo)); // textContent evita inyectar HTML
        tr.appendChild(td);
      }
      tbody.appendChild(tr);
    }
  }

  // ===== Parametros de la ruta =====
  function parametrosDeRuta(endpoint) {
    return [...endpoint.matchAll(/\{(\w+)\}/g)].map((m) => m[1]);
  }

  /** Formulario pequeno para escribir los {parametros} que falten en la URL. */
  function formularioParametros(tabla, faltantes) {
    const form = document.createElement('form');
    form.className = 'parametros-tabla';
    for (const nombre of faltantes) {
      const label = document.createElement('label');
      label.textContent = nombre + ' ';
      const input = document.createElement('input');
      input.name = nombre;
      input.type = 'number';
      input.min = '1';
      input.required = true;
      label.appendChild(input);
      form.appendChild(label);
    }
    const boton = document.createElement('button');
    boton.type = 'submit';
    boton.textContent = 'Cargar';
    form.appendChild(boton);
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const url = new URL(location.href);
      for (const [k, v] of new FormData(form)) {
        url.searchParams.set(k, v);
      }
      location.href = url.href;
    });
    tabla.closest('.tabla-contenedor').before(form);
  }

  // ===== Llenar una tabla =====
  async function cargarTabla(tabla) {
    const endpoint = tabla.dataset.endpoint;
    const campos = [...tabla.querySelectorAll('thead th')].map((th) => th.dataset.campo);
    const tbody = tabla.querySelector('tbody');
    const query = new URLSearchParams(location.search);

    const nombres = parametrosDeRuta(endpoint);
    const faltantes = nombres.filter((n) => !query.get(n));
    if (faltantes.length) {
      filaMensaje(tbody, campos.length, 'Escribe ' + faltantes.join(' y ') + ' para cargar la tabla.', 'sin-datos');
      formularioParametros(tabla, faltantes);
      return;
    }

    let ruta = endpoint.replace(/^\/api/, '');
    for (const n of nombres) {
      ruta = ruta.replace('{' + n + '}', encodeURIComponent(query.get(n)));
    }
    // el resto de parametros de la pagina se manda como filtros (?estado=pendiente, ?q=...)
    const filtros = {};
    for (const [k, v] of query) {
      if (!nombres.includes(k)) {
        filtros[k] = v;
      }
    }

    filaMensaje(tbody, campos.length, 'Cargando…', 'sin-datos');
    try {
      let datos = await api.peticion('GET', ruta, { params: filtros });
      if (tabla.dataset.lista) {
        datos = leerCampo(datos, tabla.dataset.lista);
      }
      const lista = Array.isArray(datos) ? datos : datos ? [datos] : [];
      if (lista.length === 0) {
        filaMensaje(tbody, campos.length, 'Sin registros.', 'sin-datos');
      } else {
        pintarFilas(tbody, campos, lista);
      }
    } catch (e) {
      if (e.status === 401) {
        irALogin();
        return;
      }
      const codigo = e.status ? 'Error ' + e.status + ': ' : '';
      filaMensaje(tbody, campos.length, codigo + e.mensaje, 'error');
    }
  }

  // ===== Sesion =====
  function irALogin() {
    location.href = RAIZ + 'login.html?volver=' + encodeURIComponent(location.href);
  }

  function barraSesion() {
    const usuario = api.sesion.usuario();
    const barra = document.createElement('div');
    barra.className = 'barra-sesion';
    barra.append('Sesión: ');
    const nombre = document.createElement('strong');
    nombre.textContent = usuario.nombre;
    barra.append(nombre, ' (' + usuario.rol + ', id ' + usuario.idUsuario + ') · ');
    const salir = document.createElement('a');
    salir.href = '#';
    salir.textContent = 'Cerrar sesión';
    salir.addEventListener('click', (e) => {
      e.preventDefault();
      api.auth.logout();
      irALogin();
    });
    barra.appendChild(salir);
    (document.querySelector('header') || document.body).prepend(barra);
  }

  document.addEventListener('DOMContentLoaded', () => {
    if (!api.sesion.activa()) {
      irALogin();
      return;
    }
    barraSesion();
    document.querySelectorAll('table[data-endpoint]').forEach(cargarTabla);
  });

  window.tablas = { cargarTabla, formatear };
})();
