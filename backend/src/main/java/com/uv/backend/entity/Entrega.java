package com.uv.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla ENTREGAS.
 *
 * <p>Una entrega pertenece a una tarea y puede ser individual
 * (id_alumno) o de equipo (id_equipo). El campo
 * {@code fechaEntrega} lo llena la base de datos
 * (DEFAULT CURRENT_TIMESTAMP), por eso se marca como no insertable
 * ni actualizable desde JPA.</p>
 */
@Entity
@Table(name = "ENTREGAS")
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Integer idEntrega;

    @ManyToOne
    @JoinColumn(name = "id_tarea", nullable = false)
    private Tarea tarea;

    @ManyToOne
    @JoinColumn(name = "id_alumno", nullable = true)
    private Usuario alumno;

    @ManyToOne
    @JoinColumn(name = "id_equipo", nullable = true)
    private Equipo equipo;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Column(name = "fecha_entrega", insertable = false, updatable = false)
    private LocalDateTime fechaEntrega;

    @Column(name = "calificacion", precision = 5, scale = 2)
    private BigDecimal calificacion;

    @Column(name = "comentario_maestro", columnDefinition = "TEXT")
    private String comentarioMaestro;

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public Entrega() {
    }

    /**
     * Constructor con parámetros (sin idEntrega, que lo genera
     * la base de datos).
     */
    public Entrega(Tarea tarea, Usuario alumno, Equipo equipo, String archivoUrl) {
        this.tarea = tarea;
        this.alumno = alumno;
        this.equipo = equipo;
        this.archivoUrl = archivoUrl;
    }

    public Integer getIdEntrega() {
        return idEntrega;
    }

    public void setIdEntrega(Integer idEntrega) {
        this.idEntrega = idEntrega;
    }

    public Tarea getTarea() {
        return tarea;
    }

    public void setTarea(Tarea tarea) {
        this.tarea = tarea;
    }

    public Usuario getAlumno() {
        return alumno;
    }

    public void setAlumno(Usuario alumno) {
        this.alumno = alumno;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public String getArchivoUrl() {
        return archivoUrl;
    }

    public void setArchivoUrl(String archivoUrl) {
        this.archivoUrl = archivoUrl;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public BigDecimal getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(BigDecimal calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentarioMaestro() {
        return comentarioMaestro;
    }

    public void setComentarioMaestro(String comentarioMaestro) {
        this.comentarioMaestro = comentarioMaestro;
    }
}
