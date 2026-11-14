package com.uv.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que mapea la tabla EXAMEN_RESULTADOS.
 *
 * <p>El campo {@code fechaRealizacion} lo llena la base de datos
 * (DEFAULT CURRENT_TIMESTAMP), por eso se marca como no insertable
 * ni actualizable desde JPA.</p>
 */
@Entity
@Table(name = "EXAMEN_RESULTADOS")
public class ExamenResultado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Integer idResultado;

    @ManyToOne
    @JoinColumn(name = "id_examen", nullable = false)
    private Examen examen;

    @ManyToOne
    @JoinColumn(name = "id_alumno", nullable = false)
    private Usuario alumno;

    @Column(name = "calificacion_final", precision = 5, scale = 2)
    private BigDecimal calificacionFinal;

    @Column(name = "estado_revision", nullable = false, length = 50)
    private String estadoRevision;

    @Column(name = "fecha_realizacion", insertable = false, updatable = false)
    private LocalDateTime fechaRealizacion;

    @JsonIgnore
    @OneToMany(mappedBy = "resultado")
    private List<RespuestaAlumno> respuestas = new ArrayList<>();

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public ExamenResultado() {
    }

    /**
     * Constructor con parámetros (sin idResultado, que lo genera
     * la base de datos).
     */
    public ExamenResultado(Examen examen, Usuario alumno, String estadoRevision) {
        this.examen = examen;
        this.alumno = alumno;
        this.estadoRevision = estadoRevision;
    }

    public Integer getIdResultado() {
        return idResultado;
    }

    public void setIdResultado(Integer idResultado) {
        this.idResultado = idResultado;
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = examen;
    }

    public Usuario getAlumno() {
        return alumno;
    }

    public void setAlumno(Usuario alumno) {
        this.alumno = alumno;
    }

    public BigDecimal getCalificacionFinal() {
        return calificacionFinal;
    }

    public void setCalificacionFinal(BigDecimal calificacionFinal) {
        this.calificacionFinal = calificacionFinal;
    }

    public String getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(String estadoRevision) {
        this.estadoRevision = estadoRevision;
    }

    public LocalDateTime getFechaRealizacion() {
        return fechaRealizacion;
    }

    public void setFechaRealizacion(LocalDateTime fechaRealizacion) {
        this.fechaRealizacion = fechaRealizacion;
    }

    public List<RespuestaAlumno> getRespuestas() {
        return respuestas;
    }

    public void setRespuestas(List<RespuestaAlumno> respuestas) {
        this.respuestas = respuestas;
    }
}
