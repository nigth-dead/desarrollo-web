const Store = (() => {
  // Traducción camelCase -> snake_case
  function traducirUsuario(u) {
    if (!u) return null;
    return {
      id_usuario: u.idUsuario,
      nombre: u.nombre,
      email: u.email,
      rol: u.rol,
      fecha_registro: u.fechaRegistro || null
    };
  }

  function traducirCurso(c) {
    if (!c) return null;
    return {
      id_curso: c.idCurso,
      nombre: c.nombre,
      descripcion: c.descripcion || "",
      codigo_invitacion: c.codigoInvitacion,
      id_maestro: c.maestro ? c.maestro.idUsuario : (c.idMaestro || null),
      maestro: traducirUsuario(c.maestro)
    };
  }

  function traducirTarea(t) {
    if (!t) return null;
    return {
      id_tarea: t.idTarea,
      id_curso: t.curso ? t.curso.idCurso : (t.idCurso || null),
      titulo: t.titulo,
      descripcion: t.descripcion || "",
      fecha_programada: t.fechaProgramada || null,
      fecha_limite: t.fechaLimite || null,
      es_equipo: !!t.esEquipo
    };
  }

  function traducirEquipo(e) {
    if (!e) return null;
    return {
      id_equipo: e.idEquipo,
      id_tarea: e.tarea ? e.tarea.idTarea : (e.idTarea || null),
      nombre_equipo: e.nombreEquipo,
      integrantes: Array.isArray(e.integrantes) ? e.integrantes.map(traducirUsuario) : []
    };
  }

  function traducirEntrega(e) {
    if (!e) return null;
    return {
      id_entrega: e.idEntrega,
      id_tarea: e.tarea ? e.tarea.idTarea : (e.idTarea || null),
      id_alumno: e.alumno ? e.alumno.idUsuario : (e.idAlumno || null),
      id_equipo: e.equipo ? e.equipo.idEquipo : (e.idEquipo || null),
      archivo_url: e.archivoUrl || "",
      descripcion: e.descripcion || "",
      fecha_entrega: e.fechaEntrega || null,
      calificacion: e.calificacion === undefined || e.calificacion === null ? null : Number(e.calificacion),
      comentario_maestro: e.comentarioMaestro || "",
      alumno: traducirUsuario(e.alumno),
      equipo: traducirEquipo(e.equipo)
    };
  }

  function traducirProrroga(p) {
    if (!p) return null;
    return {
      id_prorroga: p.idProrroga,
      id_tarea: p.tarea ? p.tarea.idTarea : (p.idTarea || null),
      id_alumno: p.alumno ? p.alumno.idUsuario : (p.idAlumno || null),
      id_equipo: p.equipo ? p.equipo.idEquipo : (p.idEquipo || null),
      nueva_fecha_limite: p.nuevaFechaLimite || null,
      justificacion: p.justificacion || null,
      aprobada: p.aprobada === undefined ? null : p.aprobada,
      alumno: traducirUsuario(p.alumno),
      equipo: traducirEquipo(p.equipo)
    };
  }

  function traducirForo(f) {
    if (!f) return null;
    return {
      id_foro: f.idForo,
      id_curso: f.curso ? f.curso.idCurso : (f.idCurso || null),
      titulo: f.titulo,
      descripcion: f.descripcion || "",
      fecha_creacion: f.fechaCreacion || null
    };
  }

  function traducirComentario(c) {
    if (!c) return null;
    return {
      id_comentario: c.idComentario,
      id_foro: c.foro ? c.foro.idForo : (c.idForo || null),
      id_usuario: c.usuario ? c.usuario.idUsuario : (c.idUsuario || null),
      contenido: c.contenido,
      fecha_comentario: c.fechaComentario || null,
      autor: traducirUsuario(c.usuario)
    };
  }

  function traducirExamen(e) {
    if (!e) return null;
    return {
      id_examen: e.idExamen,
      id_curso: e.curso ? e.curso.idCurso : (e.idCurso || null),
      titulo: e.titulo,
      fecha_inicio: e.fechaInicio || null,
      fecha_fin: e.fechaFin || null
    };
  }

  function parseOpciones(valor) {
    if (Array.isArray(valor)) return valor.slice();
    if (typeof valor === "string" && valor.trim() !== "") {
      try {
        const parsed = JSON.parse(valor);
        return Array.isArray(parsed) ? parsed : [];
      } catch (e) {
        return [];
      }
    }
    return [];
  }

  function traducirPregunta(p) {
    if (!p) return null;
    return {
      id_pregunta: p.idPregunta,
      id_examen: p.examen ? p.examen.idExamen : (p.idExamen || null),
      pregunta: p.pregunta,
      tipo: p.tipo,
      opciones_json: parseOpciones(p.opcionesJson),
      opciones: parseOpciones(p.opcionesJson),
      respuesta_correcta: p.respuestaCorrecta || "",
      valor_puntos: p.valorPuntos === undefined || p.valorPuntos === null ? 0 : Number(p.valorPuntos)
    };
  }

  function traducirRespuesta(r) {
    if (!r) return null;
    return {
      id_respuesta: r.idRespuesta,
      id_resultado: r.resultado ? r.resultado.idResultado : (r.idResultado || null),
      id_pregunta: r.pregunta ? r.pregunta.idPregunta : (r.idPregunta || null),
      respuesta_dada: r.respuestaDada || "",
      puntaje_obtenido: r.puntajeObtenido === undefined || r.puntajeObtenido === null ? null : Number(r.puntajeObtenido),
      comentario_maestro: r.comentarioMaestro || "",
      pregunta: traducirPregunta(r.pregunta)
    };
  }

  function traducirNotificacion(n) {
    if (!n) return null;
    return {
      id_notificacion: n.idNotificacion,
      id_usuario: n.usuario ? n.usuario.idUsuario : (n.idUsuario || null),
      mensaje: n.mensaje,
      leido: !!n.leido,
      fecha_envio: n.fechaEnvio || null
    };
  }

  async function traducirResultado(r, totalPuntos) {
    if (!r) return null;
    let calificacion = null;
    if (r.calificacionFinal !== undefined && r.calificacionFinal !== null) {
      let total = totalPuntos;
      if (total === undefined) {
        const preguntas = await preguntasDeExamen(r.examen ? r.examen.idExamen : r.idExamen);
        total = preguntas.reduce((s, p) => s + (Number(p.valor_puntos) || 0), 0);
      }
      calificacion = total > 0 ? Math.round((Number(r.calificacionFinal) / total) * 10000) / 100 : 0;
    }
    return {
      id_resultado: r.idResultado,
      id_examen: r.examen ? r.examen.idExamen : (r.idExamen || null),
      id_alumno: r.alumno ? r.alumno.idUsuario : (r.idAlumno || null),
      calificacion_final: calificacion,
      estado_revision: r.estadoRevision,
      fecha_realizacion: r.fechaRealizacion || null,
      alumno: traducirUsuario(r.alumno)
    };
  }

  function mensajeError(e) {
    if (e && e.mensaje) return e.mensaje;
    if (e && e.message) return e.message;
    return "No se pudo conectar con el servidor.";
  }

  function textoExito(texto, marcador) {
    return typeof texto === "string" && texto.toLowerCase().includes(marcador.toLowerCase());
  }

  async function oNull(fn) { try { return await fn(); } catch (e) { return null; } }
  async function oLista(fn) { try { return await fn(); } catch (e) { return []; } }
  async function oOk(fn) { try { return await fn(); } catch (e) { return { ok: false, error: mensajeError(e) }; } }

  function fechaNoAnterior(valor) {
    if (!valor) return false;
    const fecha = new Date(valor);
    if (isNaN(fecha.getTime())) return false;
    const ahora = new Date();
    ahora.setSeconds(0, 0);
    return fecha.getTime() >= ahora.getTime();
  }

  // Sesión
  function usuarioActual() {
    const u = window.api.sesion.usuario();
    return u ? traducirUsuario(u) : null;
  }

  async function iniciarSesion(email, password) {
    return oOk(async () => {
      const usuario = await api.auth.login(String(email).trim(), password);
      return { ok: true, usuario: traducirUsuario(usuario) };
    });
  }

  function cerrarSesion() {
    api.auth.logout();
  }

  async function registrarUsuario(datos) {
    const actual = usuarioActual();
    if (!actual || actual.rol !== "ADMIN") {
      return { ok: false, error: "Solo el administrador puede registrar usuarios." };
    }
    return oOk(async () => {
      const usuario = await api.usuarios.crear({
        nombre: String(datos.nombre).trim(),
        email: String(datos.email).toLowerCase().trim(),
        password: String(datos.password),
        rol: ["ALUMNO", "MAESTRO", "ADMIN"].includes(datos.rol) ? datos.rol : "ALUMNO"
      });
      return { ok: true, usuario: traducirUsuario(usuario) };
    });
  }

  async function actualizarUsuario(id, datos) {
    const actual = usuarioActual();
    if (!actual || actual.rol !== "ADMIN") {
      return { ok: false, error: "Solo el administrador puede editar usuarios." };
    }
    if (actual.id_usuario === Number(id) && datos.rol && datos.rol !== actual.rol) {
      return { ok: false, error: "No puedes cambiar tu propio rol." };
    }
    return oOk(async () => {
      const usuario = await api.usuarios.editar(id, {
        nombre: String(datos.nombre).trim(),
        email: String(datos.email).toLowerCase().trim(),
        rol: datos.rol
      });
      if (datos.password) {
        await api.usuarios.cambiarPassword(id, String(datos.password));
      }
      return { ok: true, usuario: traducirUsuario(usuario) };
    });
  }

  // Eliminar un usuario
  async function eliminarUsuario(id) {
    const actual = usuarioActual();
    if (!actual || actual.rol !== "ADMIN") {
      return { ok: false, error: "Solo el administrador puede eliminar usuarios." };
    }
    if (actual.id_usuario === Number(id)) {
      return { ok: false, error: "No puedes eliminar tu propia cuenta." };
    }
    return oOk(async () => {
      await api.usuarios.eliminar(id);
      return { ok: true };
    });
  }

  // Usuarios
  async function usuarios() {
    return oLista(async () => {
      const lista = await api.usuarios.listar();
      return (lista || []).map(traducirUsuario);
    });
  }

  async function usuarioPorId(id) {
    return oNull(async () => traducirUsuario(await api.usuarios.obtener(id)));
  }

  // Cursos
  async function cursos() {
    const actual = usuarioActual();
    if (!actual) return [];
    if (actual.rol === "ADMIN") return [];
    return oLista(async () => {
      const lista = await api.cursos.listar();
      return (lista || []).map(traducirCurso);
    });
  }

  async function cursoPorId(id) {
    return oNull(async () => traducirCurso(await api.cursos.obtener(id)));
  }

  async function alumnosDeCurso(idCurso) {
    return oLista(async () => {
      const lista = await api.cursos.alumnos(idCurso);
      return (lista || []).map((ca) => {
        const alumno = traducirUsuario(ca.alumno) || {};
        alumno.fecha_inscripcion = ca.fechaInscripcion || null;
        return alumno;
      });
    });
  }

  async function crearCurso(datos) {
    const actual = usuarioActual();
    return oNull(async () => {
      const curso = await api.cursos.crear({
        nombre: String(datos.nombre).trim(),
        descripcion: String(datos.descripcion || "").trim(),
        idMaestro: actual ? actual.id_usuario : null
      });
      return traducirCurso(curso);
    });
  }

  async function actualizarCurso(id, datos) {
    return oNull(async () => {
      const curso = await api.cursos.editar(id, {
        nombre: datos.nombre !== undefined ? String(datos.nombre).trim() : undefined,
        descripcion: datos.descripcion !== undefined ? String(datos.descripcion).trim() : undefined
      });
      return traducirCurso(curso);
    });
  }

  // Eliminar un curso
  async function eliminarCurso(id) {
    const curso = await cursoPorId(id);
    if (!curso) return null;
    try {
      await api.cursos.eliminar(id);
      return curso;
    } catch (e) {
      return null;
    }
  }

  // Regenera el codigo de invitacion de un curso
  async function regenerarCodigo(idCurso) {
    return oNull(async () => {
      const curso = await api.cursos.regenerarCodigo(idCurso);
      const t = traducirCurso(curso);
      return t ? t.codigo_invitacion : null;
    });
  }

  // Dar de baja a un alumno de un curso
  async function darDeBajaAlumno(idCurso, idAlumno) {
    return oOk(async () => {
      await api.cursos.darDeBaja(idCurso, idAlumno);
      return { ok: true };
    });
  }

  async function unirseConCodigo(codigo) {
    return oOk(async () => {
      const curso = await api.cursos.unirse(String(codigo).toUpperCase().trim());
      return { ok: true, curso: traducirCurso(curso) };
    });
  }

  async function inscribirAlumno(idCurso, email) {
    return oOk(async () => {
      const ca = await api.peticion("POST", `/cursos/${idCurso}/alumnos`, { body: { email: String(email).trim() } });
      const alumno = traducirUsuario(ca.alumno || ca) || {};
      alumno.fecha_inscripcion = ca.fechaInscripcion || null;
      return { ok: true, alumno };
    });
  }

  // Tareas
  async function tareasDeCurso(idCurso) {
    return oLista(async () => {
      const lista = await api.tareas.listar(idCurso);
      return (lista || []).map(traducirTarea);
    });
  }

  async function tareaPorId(id) {
    return oNull(async () => traducirTarea(await api.tareas.obtener(id)));
  }

  async function crearTarea(idCurso, datos) {
    if (!fechaNoAnterior(datos.fecha_programada) || !fechaNoAnterior(datos.fecha_limite)) return null;
    return oNull(async () => {
      const tarea = await api.tareas.crear(idCurso, {
        titulo: String(datos.titulo).trim(),
        descripcion: String(datos.descripcion || "").trim(),
        fechaProgramada: datos.fecha_programada,
        fechaLimite: datos.fecha_limite,
        esEquipo: !!datos.es_equipo
      });
      return traducirTarea(tarea);
    });
  }

  async function actualizarTarea(id, datos) {
    const actual = await tareaPorId(id);
    const programada = datos.fecha_programada !== undefined ? datos.fecha_programada : (actual ? actual.fecha_programada : undefined);
    const limite = datos.fecha_limite !== undefined ? datos.fecha_limite : (actual ? actual.fecha_limite : undefined);
    if (programada && limite) {
      const tProgramada = new Date(programada).getTime();
      const tLimite = new Date(limite).getTime();
      if (!isNaN(tProgramada) && !isNaN(tLimite) && tLimite < tProgramada) return null;
      if (actual && actual.fecha_limite && tLimite !== new Date(actual.fecha_limite).getTime() && !fechaNoAnterior(limite)) return null;
    }
    return oNull(async () => {
      const tarea = await api.tareas.editar(id, {
        titulo: datos.titulo !== undefined ? String(datos.titulo).trim() : undefined,
        descripcion: datos.descripcion !== undefined ? String(datos.descripcion).trim() : undefined,
        fechaProgramada: datos.fecha_programada,
        fechaLimite: datos.fecha_limite,
        esEquipo: datos.es_equipo !== undefined ? !!datos.es_equipo : undefined
      });
      return traducirTarea(tarea);
    });
  }

  async function eliminarTarea(id) {
    const tarea = await tareaPorId(id);
    if (!tarea) return null;
    try {
      const texto = await api.tareas.eliminar(id);
      return textoExito(texto, "eliminad") ? tarea : null;
    } catch (e) {
      return null;
    }
  }

  // Equipos
  async function equiposDeTarea(idTarea) {
    return oLista(async () => {
      const lista = await api.equipos.listar(idTarea);
      return (lista || []).map(traducirEquipo);
    });
  }

  async function crearEquipo(idTarea, nombreEquipo, integrantesIds) {
    try {
      const equipo = await api.equipos.crear(idTarea, String(nombreEquipo).trim());
      if (integrantesIds && integrantesIds.length > 0) {
        try {
          await api.equipos.agregarIntegrantes(equipo.idEquipo, integrantesIds.map(Number));
        } catch (e) {
        }
      }
      const completo = await api.equipos.obtener(equipo.idEquipo);
      return traducirEquipo(completo);
    } catch (e) {
      return null;
    }
  }

  async function actualizarEquipo(id, datos) {
    return oNull(async () => {
      const equipo = await api.equipos.editar(id, {
        nombreEquipo: datos.nombre_equipo !== undefined ? String(datos.nombre_equipo).trim() : undefined
      });
      return traducirEquipo(equipo);
    });
  }

  async function eliminarEquipo(id) {
    const equipo = await oNull(async () => traducirEquipo(await api.equipos.obtener(id)));
    if (!equipo) return null;
    try {
      const texto = await api.equipos.eliminar(id);
      return textoExito(texto, "eliminad") ? equipo : null;
    } catch (e) {
      return null;
    }
  }

  // Agregar un integrante a un equipo que ya existe
  async function agregarIntegrante(idEquipo, idAlumno) {
    return oOk(async () => {
      await api.equipos.agregarIntegrantes(idEquipo, [Number(idAlumno)]);
      const alumno = await usuarioPorId(idAlumno);
      return { ok: true, alumno };
    });
  }

  async function quitarIntegrante(idEquipo, idAlumno) {
    try {
      const texto = await api.equipos.quitarIntegrante(idEquipo, idAlumno);
      return textoExito(texto, "eliminad");
    } catch (e) {
      return false;
    }
  }

  // Entregas
  async function entregasDeTarea(idTarea) {
    const actual = usuarioActual();
    if (actual && actual.rol === "MAESTRO") {
      return oLista(async () => {
        const lista = await api.entregas.listar(idTarea);
        return (lista || []).map(traducirEntrega);
      });
    }
    return oLista(async () => {
      const mia = await api.entregas.mia(idTarea);
      return mia ? [traducirEntrega(mia)] : [];
    });
  }

  // Fecha limite para un alumno
  async function fechaLimiteEfectiva(idTarea, idAlumno) {
    const tarea = await tareaPorId(idTarea);
    if (!tarea || !tarea.fecha_limite) return null;
    let limite = tarea.fecha_limite;
    let limiteMs = new Date(limite).getTime();
    const equipos = await equiposDeTarea(idTarea);
    const misEquipos = equipos
      .filter((e) => e.integrantes.some((u) => u.id_usuario === Number(idAlumno)))
      .map((e) => e.id_equipo);
    const prorrogas = await prorrogasDeTarea(idTarea);
    prorrogas
      .filter((p) => p.aprobada === true &&
        (p.id_alumno === Number(idAlumno) || (p.id_equipo && misEquipos.includes(p.id_equipo))))
      .forEach((p) => {
        const nueva = new Date(p.nueva_fecha_limite).getTime();
        if (!Number.isNaN(nueva) && nueva > limiteMs) { limiteMs = nueva; limite = p.nueva_fecha_limite; }
      });
    return limite;
  }

  async function entregaVencida(idTarea, idAlumno) {
    const limite = await fechaLimiteEfectiva(idTarea, idAlumno);
    if (!limite) return false;
    return new Date(limite).getTime() <= Date.now();
  }

  async function entregaAnticipada(idTarea) {
    const tarea = await tareaPorId(idTarea);
    if (!tarea || !tarea.fecha_programada) return false;
    return Date.now() < new Date(tarea.fecha_programada).getTime();
  }

  function esEnlaceValido(url) {
    if (!url) return false;
    const u = String(url).trim();
    return /^https?:\/\/(localhost(:\d+)?|[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)+(:\d+)?)(\/\S*)?$/.test(u);
  }

  async function entregarTarea(datos) {
    const actual = usuarioActual();
    const tarea = await tareaPorId(datos.id_tarea);
    if (tarea && await entregaAnticipada(datos.id_tarea)) {
      return { ok: false, error: "Esta tarea aún no abre. Podrás entregar a partir del " + formatearFecha(tarea.fecha_programada) + "." };
    }
    if (tarea && actual && await entregaVencida(datos.id_tarea, actual.id_usuario)) {
      return { ok: false, error: "La fecha de entrega ya pasó. Solicita una prórroga para poder entregar." };
    }
    try {
      const descripcion = String(datos.descripcion || "").trim();
      if (datos.archivo) {
        const form = new FormData();
        form.append("archivo", datos.archivo);
        if (descripcion) form.append("descripcion", descripcion);
        const entrega = await api.peticion("POST", "/tareas/" + Number(datos.id_tarea) + "/entregas", { body: form });
        return traducirEntrega(entrega);
      }
      const url = String(datos.archivo_url || "").trim();
      if (!esEnlaceValido(url)) {
        return { ok: false, error: "Pega un enlace válido (que empiece con http:// o https://) o sube un archivo." };
      }
      const entrega = await api.peticion("POST", "/tareas/" + Number(datos.id_tarea) + "/entregas", {
        body: { archivoUrl: url, descripcion }
      });
      return traducirEntrega(entrega);
    } catch (e) {
      return { ok: false, error: mensajeError(e) };
    }
  }

  async function calificarEntrega(idEntrega, calificacion, comentario) {
    return oNull(async () => {
      const entrega = await api.entregas.calificar(idEntrega, Number(calificacion), String(comentario || "").trim());
      return traducirEntrega(entrega);
    });
  }

  function esDuenoEntrega(entrega, idUsuario) {
    if (!entrega || !idUsuario) return false;
    if (entrega.id_alumno === Number(idUsuario)) return true;
    if (!entrega.id_equipo) return false;
    return entrega.equipo && Array.isArray(entrega.equipo.integrantes)
      ? entrega.equipo.integrantes.some((u) => u.id_usuario === Number(idUsuario))
      : false;
  }

  // Editar la entrega
  async function actualizarEntrega(idEntrega, datos) {
    const actual = usuarioActual();
    const entrega = await oNull(async () => traducirEntrega(await api.entregas.obtener(idEntrega)));
    if (!entrega) return { ok: false, error: "No existe esa entrega." };
    if (!actual || !esDuenoEntrega(entrega, actual.id_usuario)) {
      return { ok: false, error: "Solo puedes editar tus propias entregas." };
    }
    if (await entregaAnticipada(entrega.id_tarea)) {
      return { ok: false, error: "Esta tarea aún no abre." };
    }
    if (await entregaVencida(entrega.id_tarea, actual.id_usuario)) {
      return { ok: false, error: "La fecha de entrega ya pasó. Solicita una prórroga para poder entregar." };
    }
    try {
      const descripcion = String(datos.descripcion ?? entrega.descripcion ?? "").trim();
      if (datos.archivo) {
        const form = new FormData();
        form.append("archivo", datos.archivo);
        form.append("descripcion", descripcion);
        const actualizada = await api.peticion("PUT", "/entregas/" + Number(idEntrega), { body: form });
        return { ok: true, entrega: traducirEntrega(actualizada) };
      }
      let url = String(datos.archivo_url || "").trim();
      if (!url) url = entrega.archivo_url || "";
      if (url && url !== (entrega.archivo_url || "") && !esEnlaceValido(url)) {
        return { ok: false, error: "El enlace no es válido. Debe empezar con http:// o https://." };
      }
      const actualizada = await api.peticion("PUT", "/entregas/" + Number(idEntrega), {
        body: { archivoUrl: url, descripcion }
      });
      return { ok: true, entrega: traducirEntrega(actualizada) };
    } catch (e) {
      return { ok: false, error: mensajeError(e) };
    }
  }

  // Eliminar la entrega
  async function eliminarEntrega(idEntrega) {
    return oOk(async () => {
      await api.peticion("DELETE", `/entregas/${idEntrega}`);
      return { ok: true };
    });
  }

  // Prorrogas
  async function prorrogasDeTarea(idTarea) {
    return oLista(async () => {
      const lista = await api.prorrogas.listar(idTarea);
      return (lista || []).map(traducirProrroga);
    });
  }

  async function solicitarProrroga(datos) {
    return oNull(async () => {
      const prorroga = await api.prorrogas.solicitar(datos.id_tarea, {
        nuevaFechaLimite: datos.nueva_fecha_limite,
        justificacion: datos.justificacion,
        idEquipo: datos.id_equipo ? Number(datos.id_equipo) : undefined
      });
      return traducirProrroga(prorroga);
    });
  }

  // Resolver una prorroga
  async function resolverProrroga(idProrroga, aprobada, nuevaFechaLimite) {
    try {
      const texto = await api.prorrogas.resolver(idProrroga, !!aprobada, nuevaFechaLimite || undefined);
      if (typeof texto === "string" &&
        (texto.toLowerCase().includes("aprobada") || texto.toLowerCase().includes("rechazada"))) {
        return { id_prorroga: Number(idProrroga), aprobada: !!aprobada, nueva_fecha_limite: nuevaFechaLimite || null };
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Eliminar una prorroga
  async function eliminarProrroga(id) {
    return oOk(async () => {
      const texto = await api.prorrogas.eliminar(id);
      if (textoExito(texto, "eliminad")) return { ok: true };
      return { ok: false, error: typeof texto === "string" && texto ? texto : "No se pudo eliminar la prórroga." };
    });
  }

  // Foros
  async function forosDeCurso(idCurso) {
    return oLista(async () => {
      const lista = await api.foros.listar(idCurso);
      return (lista || []).map(traducirForo);
    });
  }

  async function crearTema(idCurso, datos) {
    return oNull(async () => {
      const foro = await api.foros.crear(idCurso, {
        titulo: String(datos.titulo).trim(),
        descripcion: String(datos.descripcion || "").trim()
      });
      return traducirForo(foro);
    });
  }

  async function comentariosDeForo(idForo) {
    return oLista(async () => {
      const lista = await api.comentarios.listar(idForo);
      return (lista || []).map(traducirComentario);
    });
  }

  async function comentarForo(idForo, contenido) {
    return oNull(async () => {
      const comentario = await api.comentarios.crear(idForo, String(contenido).trim());
      return traducirComentario(comentario);
    });
  }

  async function actualizarForo(id, datos) {
    return oNull(async () => {
      const foro = await api.foros.editar(id, {
        titulo: datos.titulo !== undefined ? String(datos.titulo).trim() : undefined,
        descripcion: datos.descripcion !== undefined ? String(datos.descripcion).trim() : undefined
      });
      return traducirForo(foro);
    });
  }

  // Eliminar un tema de foro con sus comentarios
  async function eliminarForo(id) {
    const foro = await oNull(async () => traducirForo(await api.foros.obtener(id)));
    if (!foro) return null;
    try {
      const texto = await api.foros.eliminar(id);
      return textoExito(texto, "eliminad") ? foro : null;
    } catch (e) {
      return null;
    }
  }

  // Editar un comentario
  async function actualizarComentario(id, contenido) {
    return oOk(async () => {
      const comentario = await api.comentarios.editar(id, String(contenido).trim());
      if (!comentario) return { ok: false, error: "No puedes editar ese comentario." };
      return { ok: true, comentario: traducirComentario(comentario) };
    });
  }

  // Eliminar un comentario
  async function eliminarComentario(id) {
    return oOk(async () => {
      const texto = await api.comentarios.eliminar(id);
      if (textoExito(texto, "eliminad")) return { ok: true };
      return { ok: false, error: typeof texto === "string" && texto ? texto : "No se pudo eliminar el comentario." };
    });
  }

  // Examenes
  async function examenesDeCurso(idCurso) {
    return oLista(async () => {
      const lista = await api.examenes.listar(idCurso);
      return (lista || []).map(traducirExamen);
    });
  }

  async function examenPorId(id) {
    return oNull(async () => traducirExamen(await api.examenes.obtener(id)));
  }

  async function preguntasDeExamen(idExamen) {
    return oLista(async () => {
      const lista = await api.preguntas.listar(idExamen);
      return (lista || []).map(traducirPregunta);
    });
  }

  function mapearPreguntaCrear(p) {
    return {
      pregunta: String(p.pregunta).trim(),
      tipo: p.tipo,
      opciones: p.tipo === "OPCION_MULTIPLE" ? (p.opciones || []) : [],
      respuestaCorrecta: p.tipo === "OPCION_MULTIPLE" ? String(p.respuesta_correcta || "") : "",
      valorPuntos: Number(p.valor_puntos) || 0
    };
  }

  async function crearExamen(idCurso, datos) {
    if (!fechaNoAnterior(datos.fecha_inicio) || !fechaNoAnterior(datos.fecha_fin)) return null;
    try {
      const examen = await api.examenes.crear(idCurso, {
        titulo: String(datos.titulo).trim(),
        fechaInicio: datos.fecha_inicio,
        fechaFin: datos.fecha_fin
      });
      for (const p of (datos.preguntas || [])) {
        try {
          await api.preguntas.crear(examen.idExamen, mapearPreguntaCrear(p));
        } catch (e) {
        }
      }
      return traducirExamen(examen);
    } catch (e) {
      return null;
    }
  }

  async function actualizarExamen(id, datos) {
    return oNull(async () => {
      const examen = await api.examenes.editar(id, {
        titulo: datos.titulo !== undefined ? String(datos.titulo).trim() : undefined,
        fechaInicio: datos.fecha_inicio,
        fechaFin: datos.fecha_fin
      });
      return traducirExamen(examen);
    });
  }

  async function eliminarExamen(id) {
    const examen = await examenPorId(id);
    if (!examen) return null;
    try {
      const texto = await api.examenes.eliminar(id);
      return textoExito(texto, "eliminad") ? examen : null;
    } catch (e) {
      return null;
    }
  }

  async function agregarPregunta(idExamen, datos) {
    return oNull(async () => {
      const pregunta = await api.preguntas.crear(idExamen, mapearPreguntaCrear(datos));
      return traducirPregunta(pregunta);
    });
  }

  async function actualizarPregunta(id, datos) {
    return oNull(async () => {
      const pregunta = await api.preguntas.editar(id, {
        pregunta: datos.pregunta !== undefined ? String(datos.pregunta).trim() : undefined,
        tipo: datos.tipo,
        opcionesJson: datos.opciones !== undefined
          ? JSON.stringify(datos.tipo === "OPCION_MULTIPLE" ? (datos.opciones || []) : [])
          : undefined,
        respuestaCorrecta: datos.respuesta_correcta !== undefined ? String(datos.respuesta_correcta || "") : undefined,
        valorPuntos: datos.valor_puntos !== undefined ? Number(datos.valor_puntos) || 0 : undefined
      });
      return traducirPregunta(pregunta);
    });
  }

  // Eliminar una pregunta
  async function eliminarPregunta(id) {
    try {
      const texto = await api.preguntas.eliminar(id);
      return textoExito(texto, "eliminad") ? { id_pregunta: Number(id) } : null;
    } catch (e) {
      return null;
    }
  }

  async function resultadosDeExamen(idExamen) {
    return oLista(async () => {
      const resultados = await api.resultados.listar(idExamen);
      const preguntas = await preguntasDeExamen(idExamen);
      const totalPuntos = preguntas.reduce((s, p) => s + (Number(p.valor_puntos) || 0), 0);
      const lista = [];
      for (const r of (resultados || [])) {
        const t = await traducirResultado(r, totalPuntos);
        if (t) lista.push(t);
      }
      return lista;
    });
  }

  async function resultadoDeExamen(idExamen, idAlumno) {
    // Solo un alumno presenta exámenes: para maestro o admin no se consulta (evita 404 innecesarios)
    const actual = usuarioActual();
    if (!actual || actual.rol !== "ALUMNO") return null;
    return oNull(async () => {
      const detalle = await api.resultados.mio(idExamen);
      if (!detalle || !detalle.resultado) return null;
      // El total se toma de las preguntas de las respuestas: el alumno ya no puede consultar
      // GET /examenes/{id}/preguntas cuando el examen cerró (403) y el total quedaba en 0 → "0 / 100"
      const total = (detalle.respuestas || []).reduce((s, r) => s + (Number(r.pregunta && r.pregunta.valorPuntos) || 0), 0);
      return traducirResultado(detalle.resultado, total > 0 ? total : undefined);
    });
  }

  async function respuestasDeResultado(idResultado) {
    return oLista(async () => {
      const detalle = await api.resultados.obtener(idResultado);
      return (detalle.respuestas || []).map(traducirRespuesta);
    });
  }

  async function responderExamen(idExamen, respuestas) {
    const actual = usuarioActual();
    if (!actual) return { ok: false, error: "Inicia sesión primero." };
    if (await resultadoDeExamen(idExamen, actual.id_usuario)) {
      return { ok: false, error: "Ya respondiste este examen." };
    }
    if (!await examenAbierto(idExamen)) {
      return { ok: false, error: "Este examen aún no abre." };
    }
    if (await examenCerrado(idExamen)) {
      return { ok: false, error: "Este examen ya cerró." };
    }
    try {
      const detalle = await api.resultados.enviar(idExamen, (respuestas || []).map((r) => ({
        idPregunta: Number(r.id_pregunta),
        respuestaDada: String(r.respuesta_dada || "")
      })));
      const lista = (detalle.respuestas || []).map(traducirRespuesta);
      const puntosTotales = lista.reduce((s, r) => s + (Number(r.pregunta && r.pregunta.valor_puntos) || 0), 0);
      const puntosObtenidos = lista.reduce((s, r) => s + (Number(r.puntaje_obtenido) || 0), 0);
      const resultado = await traducirResultado(detalle.resultado, puntosTotales);
      return { ok: true, resultado, puntosObtenidos, puntosTotales };
    } catch (e) {
      return { ok: false, error: mensajeError(e) };
    }
  }

  async function respuestasDeExamen(idExamen) {
    try {
      const resultados = await api.resultados.listar(idExamen);
      const preguntas = await preguntasDeExamen(idExamen);
      const totalPuntos = preguntas.reduce((s, p) => s + (Number(p.valor_puntos) || 0), 0);
      const lista = [];
      for (const r of (resultados || [])) {
        const detalle = await oNull(() => api.resultados.obtener(r.idResultado));
        if (!detalle || !detalle.resultado) continue;
        const resultado = await traducirResultado(detalle.resultado, totalPuntos);
        for (const resp of (detalle.respuestas || [])) {
          const pregunta = traducirPregunta(resp.pregunta);
          if (!pregunta) continue;
          const t = traducirRespuesta(resp);
          lista.push({
            ...t,
            alumno: traducirUsuario(detalle.resultado.alumno),
            pregunta,
            estado_revision: resultado.estado_revision,
            id_resultado: resultado.id_resultado,
            calificacion_final: resultado.calificacion_final,
            fecha_realizacion: resultado.fecha_realizacion
          });
        }
      }
      return lista;
    } catch (e) {
      return [];
    }
  }

  // Solo las respuestas abiertas
  async function respuestasPendientes(idExamen) {
    const todas = await respuestasDeExamen(idExamen);
    return todas.filter((r) => r.pregunta && r.pregunta.tipo === "ABIERTA");
  }

  async function calificarRespuesta(idRespuesta, puntaje, comentario) {
    return oNull(async () => {
      const respuesta = await api.resultados.calificarRespuesta(idRespuesta, Number(puntaje), String(comentario || "").trim());
      return traducirRespuesta(respuesta);
    });
  }

  async function examenCerrado(examenOId) {
    const examen = (examenOId && typeof examenOId === "object") ? examenOId : await examenPorId(examenOId);
    if (!examen) return false;
    const fin = examen.fecha_fin || examen.fechaFin;
    if (!fin) return false;
    return new Date(fin).getTime() <= Date.now();
  }

  async function examenAbierto(examenOId) {
    const examen = (examenOId && typeof examenOId === "object") ? examenOId : await examenPorId(examenOId);
    if (!examen) return true;
    const inicio = examen.fecha_inicio || examen.fechaInicio;
    if (!inicio) return true;
    return new Date(inicio).getTime() <= Date.now();
  }

  function revisarExamenesCerrados() {
    return 0;
  }

  // Notificaciones
  async function notificaciones() {
    return oLista(async () => {
      const lista = await api.notificaciones.listar();
      return (lista || []).map(traducirNotificacion)
        .sort((a, b) => String(b.fecha_envio).localeCompare(String(a.fecha_envio)));
    });
  }

  async function notificacionesNoLeidas() {
    try {
      const r = await api.notificaciones.totalNoLeidas();
      if (typeof r === "number") return r;
      if (r && r.total !== undefined && r.total !== null) return Number(r.total);
      return 0;
    } catch (e) {
      return 0;
    }
  }

  async function marcarNotificacionesLeidas() {
    try { await api.notificaciones.marcarTodasLeidas(); } catch (e) { /* sin efecto */ }
  }

  // Marcar una sola notificación como leída
  async function marcarNotificacionLeida(id) {
    try {
      const n = await api.notificaciones.marcarLeida(id);
      return !!n;
    } catch (e) {
      return false;
    }
  }

  async function eliminarNotificacion(id) {
    try {
      const texto = await api.notificaciones.eliminar(id);
      return textoExito(texto, "eliminad");
    } catch (e) {
      return false;
    }
  }

  function formatearFecha(valor) {
    if (!valor) return "Sin fecha";
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return String(valor);
    return fecha.toLocaleString("es-MX", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
  }

  function escaparHTML(texto) {
    return String(texto == null ? "" : texto)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function esMaestro() {
    const u = usuarioActual();
    return !!u && u.rol === "MAESTRO";
  }

  function esAlumno() {
    const u = usuarioActual();
    return !!u && u.rol === "ALUMNO";
  }

  function esAdmin() {
    const u = usuarioActual();
    return !!u && u.rol === "ADMIN";
  }

  return {
    usuarioActual, iniciarSesion, cerrarSesion, registrarUsuario,
    usuarios, usuarioPorId, actualizarUsuario, eliminarUsuario,
    cursos, cursoPorId, alumnosDeCurso, crearCurso, actualizarCurso, eliminarCurso, regenerarCodigo, unirseConCodigo, inscribirAlumno, darDeBajaAlumno,
    tareasDeCurso, tareaPorId, crearTarea, actualizarTarea, eliminarTarea,
    equiposDeTarea, crearEquipo, actualizarEquipo, eliminarEquipo, agregarIntegrante, quitarIntegrante,
    entregasDeTarea, entregarTarea, calificarEntrega, actualizarEntrega, eliminarEntrega, fechaLimiteEfectiva, entregaVencida, entregaAnticipada,
    prorrogasDeTarea, solicitarProrroga, resolverProrroga, eliminarProrroga,
    forosDeCurso, crearTema, actualizarForo, eliminarForo, comentariosDeForo, comentarForo, actualizarComentario, eliminarComentario,
    examenesDeCurso, examenPorId, preguntasDeExamen, crearExamen, actualizarExamen, eliminarExamen, agregarPregunta, actualizarPregunta, eliminarPregunta,
    resultadosDeExamen, resultadoDeExamen, respuestasDeResultado, responderExamen, respuestasDeExamen, respuestasPendientes, calificarRespuesta, revisarExamenesCerrados, examenAbierto, examenCerrado,
    notificaciones, notificacionesNoLeidas, marcarNotificacionesLeidas, marcarNotificacionLeida, eliminarNotificacion,
    formatearFecha, escaparHTML,
    esMaestro, esAlumno, esAdmin
  };
})();
