package com.uv.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta de la tabla CURSO_ALUMNOS
 * (id_curso, id_alumno).
 */
@Embeddable
public class CursoAlumnoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_curso")
    private Integer idCurso;

    @Column(name = "id_alumno")
    private Integer idAlumno;

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public CursoAlumnoId() {
    }

    public CursoAlumnoId(Integer idCurso, Integer idAlumno) {
        this.idCurso = idCurso;
        this.idAlumno = idAlumno;
    }

    public Integer getIdCurso() {
        return idCurso;
    }

    public void setIdCurso(Integer idCurso) {
        this.idCurso = idCurso;
    }

    public Integer getIdAlumno() {
        return idAlumno;
    }

    public void setIdAlumno(Integer idAlumno) {
        this.idAlumno = idAlumno;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CursoAlumnoId that = (CursoAlumnoId) o;
        return Objects.equals(idCurso, that.idCurso)
                && Objects.equals(idAlumno, that.idAlumno);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCurso, idAlumno);
    }
}
