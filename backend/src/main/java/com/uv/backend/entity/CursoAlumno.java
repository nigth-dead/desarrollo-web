package com.uv.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla CURSO_ALUMNOS.
 *
 * <p>El campo {@code fechaInscripcion} lo llena la base de datos
 * (DEFAULT CURRENT_TIMESTAMP), por eso se marca como no insertable
 * ni actualizable desde JPA.</p>
 */
@Entity
@Table(name = "CURSO_ALUMNOS")
public class CursoAlumno {

    @EmbeddedId
    private CursoAlumnoId id;

    @ManyToOne
    @MapsId("idCurso")
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @ManyToOne
    @MapsId("idAlumno")
    @JoinColumn(name = "id_alumno")
    private Usuario alumno;

    @Column(name = "fecha_inscripcion", insertable = false, updatable = false)
    private LocalDateTime fechaInscripcion;

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public CursoAlumno() {
    }

    /**
     * Constructor con parámetros. La clave compuesta se construye
     * a partir de los identificadores del curso y del alumno.
     */
    public CursoAlumno(Curso curso, Usuario alumno) {
        this.curso = curso;
        this.alumno = alumno;
        this.id = new CursoAlumnoId(curso.getIdCurso(), alumno.getIdUsuario());
    }

    public CursoAlumnoId getId() {
        return id;
    }

    public void setId(CursoAlumnoId id) {
        this.id = id;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public Usuario getAlumno() {
        return alumno;
    }

    public void setAlumno(Usuario alumno) {
        this.alumno = alumno;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public void setFechaInscripcion(LocalDateTime fechaInscripcion) {
        this.fechaInscripcion = fechaInscripcion;
    }
}
