/*
 * api.js - Funciones Fetch para consumir la API del LMS.
 *
 * Uso:
 *   const cursos = await api.cursos.listar({ q: 'web' });
 *   await api.tareas.crear(idCurso, { titulo: 'Practica 1', fechaLimite: '2026-12-01T23:59:00' });
 *
 * - Todas las funciones regresan una Promise con la respuesta ya convertida (JSON o texto).
 * - El token de la sesion se manda solo en el header Authorization.
 * - El idUsuario de la sesion se agrega solo a la URL (?idUsuario=).
 * - Si la API responde con error (400, 401, 403, 404, 409, 500...) se lanza un ApiError
 *   con { status, mensaje } para que la pagina muestre el mensaje.
 */
(function () {
  'use strict';

  // ===== Configuracion =====
  const API_URL = 'http://localhost:8081/api';
  const CLAVE_SESION = 'lms.sesion';

  // ===== Error de la API =====
  class ApiError extends Error {
    constructor(status, mensaje) {
      super(mensaje);
      this.name = 'ApiError';
      this.status = status;   // codigo HTTP (0 si no hubo conexion)
      this.mensaje = mensaje; // texto que regreso el servidor
    }
  }

  // ===== Sesion (token + usuario) en localStorage =====
  const sesion = {
    guardar(datos) {
      localStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
    },
    obtener() {
      try {
        return JSON.parse(localStorage.getItem(CLAVE_SESION));
      } catch (e) {
        return null;
      }
    },
    cerrar() {
      localStorage.removeItem(CLAVE_SESION);
    },
    token() {
      const s = sesion.obtener();
      return s ? s.token : null;
    },
    usuario() {
      const s = sesion.obtener();
      return s ? s.usuario : null;
    },
    activa() {
      return Boolean(sesion.token());
    },
  };

  // ===== Funcion base =====
  /**
   * Hace la peticion a la API.
   * @param {string} metodo  GET, POST, PUT, PATCH o DELETE
   * @param {string} ruta    ruta despues de /api, ej. '/cursos/1/tareas'
   * @param {object} [opciones]
   * @param {object} [opciones.params]       filtros para la URL (los null/'' se omiten)
   * @param {object} [opciones.body]         datos que se mandan como JSON
   * @param {boolean} [opciones.sinUsuario]  no agregar ?idUsuario= de la sesion
   */
  async function peticion(metodo, ruta, opciones = {}) {
    const url = new URL(API_URL + ruta);
    const params = Object.assign({}, opciones.params);
    const usuario = sesion.usuario();
    if (usuario && !opciones.sinUsuario && params.idUsuario === undefined) {
      params.idUsuario = usuario.idUsuario;
    }
    for (const [clave, valor] of Object.entries(params)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        url.searchParams.set(clave, valor);
      }
    }

    const headers = {};
    const token = sesion.token();
    if (token) {
      headers.Authorization = 'Bearer ' + token;
    }
    let body;
    if (opciones.body !== undefined) {
      headers['Content-Type'] = 'application/json';
      body = JSON.stringify(opciones.body);
    }

    let respuesta;
    try {
      respuesta = await fetch(url, { method: metodo, headers, body });
    } catch (e) {
      throw new ApiError(0, 'No se pudo conectar con el servidor (' + API_URL + ')');
    }

    const texto = await respuesta.text();
    let datos = texto;
    if (texto && (respuesta.headers.get('Content-Type') || '').includes('json')) {
      datos = JSON.parse(texto);
    }

    if (!respuesta.ok) {
      // token vencido o invalido: se cierra la sesion local
      if (respuesta.status === 401 && token) {
        sesion.cerrar();
      }
      const mensaje = typeof datos === 'string' && datos ? datos
        : (datos && (datos.mensaje || datos.message || datos.error)) || respuesta.statusText;
      throw new ApiError(respuesta.status, mensaje);
    }
    return texto ? datos : null;
  }

  const get = (ruta, params) => peticion('GET', ruta, { params });
  const post = (ruta, body, params) => peticion('POST', ruta, { body, params });
  const put = (ruta, body, params) => peticion('PUT', ruta, { body, params });
  const patch = (ruta, body, params) => peticion('PATCH', ruta, { body, params });
  const del = (ruta, params) => peticion('DELETE', ruta, { params });

  // ===== Endpoints =====
  const api = {
    ApiError,
    sesion,
    peticion,
    API_URL,

    auth: {
      registro: (datos) => peticion('POST', '/auth/registro', { body: datos, sinUsuario: true }),
      /** Inicia sesion y guarda el token. Regresa el usuario. */
      async login(email, password) {
        const r = await peticion('POST', '/auth/login', { body: { email, password }, sinUsuario: true });
        sesion.guardar({ token: r.token, usuario: r.usuario });
        return r.usuario;
      },
      logout() {
        sesion.cerrar();
      },
      me: () => get('/auth/me'),
    },

    usuarios: {
      listar: (filtros) => get('/usuarios', filtros),               // { rol, q }
      obtener: (id) => get(`/usuarios/${id}`),
      crear: (datos) => post('/usuarios', datos),                   // { nombre, email, password, rol }
      editar: (id, datos) => put(`/usuarios/${id}`, datos),         // { nombre, email, rol }
      cambiarPassword: (id, password) => patch(`/usuarios/${id}/password`, { password }),
      cambiarRol: (id, rol) => patch(`/usuarios/${id}/rol`, { rol }),
      eliminar: (id) => del(`/usuarios/${id}`),
    },

    cursos: {
      listar: (filtros) => get('/cursos', filtros),                 // { q, idMaestro } (mis cursos)
      obtener: (id) => get(`/cursos/${id}`),
      crear: (datos) => post('/cursos', datos),                     // { nombre, descripcion, idMaestro }
      editar: (id, datos) => put(`/cursos/${id}`, datos),
      eliminar: (id) => del(`/cursos/${id}`),
      unirse: (codigoInvitacion) => post('/cursos/unirse', { codigoInvitacion }),
      regenerarCodigo: (id) => post(`/cursos/${id}/codigo`),
      alumnos: (id, filtros) => get(`/cursos/${id}/alumnos`, filtros), // { q }
      darDeBaja: (id, idAlumno) => del(`/cursos/${id}/alumnos/${idAlumno}`),
    },

    tareas: {
      listar: (idCurso, filtros) => get(`/cursos/${idCurso}/tareas`, filtros), // { q, estado, esEquipo }
      obtener: (id) => get(`/tareas/${id}`),
      crear: (idCurso, datos) => post(`/cursos/${idCurso}/tareas`, datos),
      editar: (id, datos) => put(`/tareas/${id}`, datos),
      eliminar: (id) => del(`/tareas/${id}`),
    },

    equipos: {
      listar: (idTarea) => get(`/tareas/${idTarea}/equipos`),
      obtener: (id) => get(`/equipos/${id}`),
      crear: (idTarea, nombreEquipo) => post(`/tareas/${idTarea}/equipos`, { nombreEquipo }),
      editar: (id, datos) => put(`/equipos/${id}`, datos),
      eliminar: (id) => del(`/equipos/${id}`),
      agregarIntegrantes: (id, idsAlumnos) => post(`/equipos/${id}/integrantes`, { idsAlumnos }),
      quitarIntegrante: (id, idAlumno) => del(`/equipos/${id}/integrantes/${idAlumno}`),
    },

    entregas: {
      listar: (idTarea, filtros) => get(`/tareas/${idTarea}/entregas`, filtros), // { calificada, tarde }
      mia: (idTarea) => get(`/tareas/${idTarea}/entregas/mia`),
      obtener: (id) => get(`/entregas/${id}`),
      entregar: (idTarea, archivoUrl) => post(`/tareas/${idTarea}/entregas`, { archivoUrl }),
      reemplazar: (id, archivoUrl) => put(`/entregas/${id}`, { archivoUrl }),
      calificar: (id, calificacion, comentarioMaestro) =>
        patch(`/entregas/${id}/calificacion`, { calificacion, comentarioMaestro }),
      /** URL para abrir el archivo (redirige a archivoUrl). */
      urlArchivo: (id) => `${API_URL}/entregas/${id}/archivo`,
    },

    prorrogas: {
      listar: (idTarea, filtros) => get(`/tareas/${idTarea}/prorrogas`, filtros), // { aprobada }
      solicitar: (idTarea, datos) => post(`/tareas/${idTarea}/prorrogas`, datos), // { nuevaFechaLimite, idAlumno?, idEquipo? }
      resolver: (id, aprobada, nuevaFechaLimite) => patch(`/prorrogas/${id}`, { aprobada, nuevaFechaLimite }),
      eliminar: (id) => del(`/prorrogas/${id}`),
    },

    foros: {
      listar: (idCurso, filtros) => get(`/cursos/${idCurso}/foros`, filtros), // { q }
      obtener: (id) => get(`/foros/${id}`),
      crear: (idCurso, datos) => post(`/cursos/${idCurso}/foros`, datos),  // { titulo, descripcion }
      editar: (id, datos) => put(`/foros/${id}`, datos),
      eliminar: (id) => del(`/foros/${id}`),
    },

    comentarios: {
      listar: (idForo, filtros) => get(`/foros/${idForo}/comentarios`, filtros), // { idAutor, q }
      crear: (idForo, contenido) => post(`/foros/${idForo}/comentarios`, { contenido }),
      editar: (id, contenido) => put(`/comentarios/${id}`, { contenido }),
      eliminar: (id) => del(`/comentarios/${id}`),
    },

    examenes: {
      listar: (idCurso, filtros) => get(`/cursos/${idCurso}/examenes`, filtros), // { estado, q }
      obtener: (id) => get(`/examenes/${id}`),
      crear: (idCurso, datos) => post(`/cursos/${idCurso}/examenes`, datos),  // { titulo, fechaInicio, fechaFin }
      editar: (id, datos) => put(`/examenes/${id}`, datos),
      eliminar: (id) => del(`/examenes/${id}`),
    },

    preguntas: {
      listar: (idExamen, filtros) => get(`/examenes/${idExamen}/preguntas`, filtros), // { tipo }
      crear: (idExamen, datos) => post(`/examenes/${idExamen}/preguntas`, datos),
      editar: (id, datos) => put(`/preguntas/${id}`, datos),
      eliminar: (id) => del(`/preguntas/${id}`),
    },

    resultados: {
      enviar: (idExamen, respuestas) => post(`/examenes/${idExamen}/resultados`, { respuestas }), // [{ idPregunta, respuestaDada }]
      listar: (idExamen, filtros) => get(`/examenes/${idExamen}/resultados`, filtros), // { estado, minCalificacion, maxCalificacion }
      mio: (idExamen) => get(`/examenes/${idExamen}/resultados/mio`),
      obtener: (id) => get(`/resultados/${id}`),
      calificarRespuesta: (idRespuesta, puntajeObtenido, comentarioMaestro) =>
        patch(`/respuestas/${idRespuesta}`, { puntajeObtenido, comentarioMaestro }),
    },

    notificaciones: {
      listar: (filtros) => get('/notificaciones', filtros),         // { leido }
      totalNoLeidas: () => get('/notificaciones/no-leidas/total'),
      marcarLeida: (id) => patch(`/notificaciones/${id}/leida`),
      marcarTodasLeidas: () => patch('/notificaciones/leidas'),
      eliminar: (id) => del(`/notificaciones/${id}`),
    },
  };

  window.api = api;
})();
