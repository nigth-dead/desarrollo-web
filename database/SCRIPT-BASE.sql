CREATE DATABASE IF NOT EXISTS lms_db;
USE lms_db;

CREATE TABLE USUARIOS (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(50) NOT NULL COMMENT 'MAESTRO / ALUMNO / ADMIN',
    fecha_registro DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE CURSOS (
    id_curso INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    codigo_invitacion VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    id_maestro INT NOT NULL,
    FOREIGN KEY (id_maestro) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE
);

CREATE TABLE CURSO_ALUMNOS (
    id_curso INT NOT NULL,
    id_alumno INT NOT NULL,
    fecha_inscripcion DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_curso, id_alumno),
    FOREIGN KEY (id_curso) REFERENCES CURSOS(id_curso) ON DELETE CASCADE,
    FOREIGN KEY (id_alumno) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE
);

CREATE TABLE TAREAS (
    id_tarea INT AUTO_INCREMENT PRIMARY KEY,
    id_curso INT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    fecha_programada DATETIME,
    fecha_limite DATETIME,
    es_equipo BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_curso) REFERENCES CURSOS(id_curso) ON DELETE CASCADE
);

CREATE TABLE EQUIPOS (
    id_equipo INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea INT NOT NULL,
    nombre_equipo VARCHAR(255) NOT NULL,
    FOREIGN KEY (id_tarea) REFERENCES TAREAS(id_tarea) ON DELETE CASCADE
);

CREATE TABLE EQUIPO_INTEGRANTES (
    id_equipo INT NOT NULL,
    id_alumno INT NOT NULL,
    PRIMARY KEY (id_equipo, id_alumno),
    FOREIGN KEY (id_equipo) REFERENCES EQUIPOS(id_equipo) ON DELETE CASCADE,
    FOREIGN KEY (id_alumno) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE
);

CREATE TABLE ENTREGAS (
    id_entrega INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea INT NOT NULL,
    id_alumno INT DEFAULT NULL COMMENT 'Null si es por equipo',
    id_equipo INT DEFAULT NULL COMMENT 'Null si es individual',
    archivo_url VARCHAR(500),
    fecha_entrega DATETIME DEFAULT CURRENT_TIMESTAMP,
    calificacion DECIMAL(5,2),
    comentario_maestro TEXT,
    FOREIGN KEY (id_tarea) REFERENCES TAREAS(id_tarea) ON DELETE CASCADE,
    FOREIGN KEY (id_alumno) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_equipo) REFERENCES EQUIPOS(id_equipo) ON DELETE CASCADE
);

CREATE TABLE PRORROGAS (
    id_prorroga INT AUTO_INCREMENT PRIMARY KEY,
    id_tarea INT NOT NULL,
    id_alumno INT DEFAULT NULL COMMENT 'Null si es para equipo',
    id_equipo INT DEFAULT NULL COMMENT 'Null si es para alumno',
    nueva_fecha_limite DATETIME NOT NULL,
    aprobada BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (id_tarea) REFERENCES TAREAS(id_tarea) ON DELETE CASCADE,
    FOREIGN KEY (id_alumno) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE,
    FOREIGN KEY (id_equipo) REFERENCES EQUIPOS(id_equipo) ON DELETE CASCADE
);

CREATE TABLE FOROS (
    id_foro INT AUTO_INCREMENT PRIMARY KEY,
    id_curso INT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    descripcion TEXT,
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_curso) REFERENCES CURSOS(id_curso) ON DELETE CASCADE
);

CREATE TABLE FORO_COMENTARIOS (
    id_comentario INT AUTO_INCREMENT PRIMARY KEY,
    id_foro INT NOT NULL,
    id_usuario INT NOT NULL,
    contenido TEXT NOT NULL,
    fecha_comentario DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_foro) REFERENCES FOROS(id_foro) ON DELETE CASCADE,
    FOREIGN KEY (id_usuario) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE
);

CREATE TABLE EXAMENES (
    id_examen INT AUTO_INCREMENT PRIMARY KEY,
    id_curso INT NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    fecha_inicio DATETIME NOT NULL,
    fecha_fin DATETIME NOT NULL,
    FOREIGN KEY (id_curso) REFERENCES CURSOS(id_curso) ON DELETE CASCADE
);

CREATE TABLE PREGUNTAS_EXAMEN (
    id_pregunta INT AUTO_INCREMENT PRIMARY KEY,
    id_examen INT NOT NULL,
    pregunta TEXT NOT NULL,
    tipo VARCHAR(50) NOT NULL COMMENT 'OPCION_MULTIPLE / ABIERTA',
    opciones_json JSON DEFAULT NULL COMMENT 'Null si es abierta',
    respuesta_correcta TEXT DEFAULT NULL COMMENT 'Null si es abierta',
    valor_puntos DECIMAL(5,2) NOT NULL,
    FOREIGN KEY (id_examen) REFERENCES EXAMENES(id_examen) ON DELETE CASCADE
);

CREATE TABLE EXAMEN_RESULTADOS (
    id_resultado INT AUTO_INCREMENT PRIMARY KEY,
    id_examen INT NOT NULL,
    id_alumno INT NOT NULL,
    calificacion_final DECIMAL(5,2) DEFAULT NULL COMMENT 'Suma total de puntajes',
    estado_revision VARCHAR(50) NOT NULL COMMENT 'PENDIENTE_MANUAL / CALIFICADO',
    fecha_realizacion DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_examen) REFERENCES EXAMENES(id_examen) ON DELETE CASCADE,
    FOREIGN KEY (id_alumno) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE
);

CREATE TABLE RESPUESTAS_ALUMNO (
    id_respuesta INT AUTO_INCREMENT PRIMARY KEY,
    id_resultado INT NOT NULL,
    id_pregunta INT NOT NULL,
    respuesta_dada TEXT,
    puntaje_obtenido DECIMAL(5,2) DEFAULT NULL COMMENT 'Null si está pendiente',
    comentario_maestro TEXT DEFAULT NULL,
    FOREIGN KEY (id_resultado) REFERENCES EXAMEN_RESULTADOS(id_resultado) ON DELETE CASCADE,
    FOREIGN KEY (id_pregunta) REFERENCES PREGUNTAS_EXAMEN(id_pregunta) ON DELETE CASCADE
);

CREATE TABLE NOTIFICACIONES (
    id_notificacion INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    mensaje TEXT NOT NULL,
    leido BOOLEAN DEFAULT FALSE,
    fecha_envio DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_usuario) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE
);