package com.uv.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entidad que mapea la tabla PRORROGAS.
 *
 * <p>Una prórroga pertenece a una tarea y puede solicitarla un
 * alumno (id_alumno) o un equipo (id_equipo).</p>
 */
@Entity
@Table(name = "PRORROGAS")
public class Prorroga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prorroga")
    private Integer idProrroga;

    @ManyToOne
    @JoinColumn(name = "id_tarea", nullable = false)
    private Tarea tarea;

    @ManyToOne
    @JoinColumn(name = "id_alumno", nullable = true)
    private Usuario alumno;

    @ManyToOne
    @JoinColumn(name = "id_equipo", nullable = true)
    private Equipo equipo;

    @Column(name = "nueva_fecha_limite", nullable = false)
    private LocalDateTime nuevaFechaLimite;

    @Column(name = "aprobada")
    private Boolean aprobada;

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public Prorroga() {
    }

    /**
     * Constructor con parámetros (sin idProrroga, que lo genera
     * la base de datos).
     */
    public Prorroga(Tarea tarea, Usuario alumno, Equipo equipo, LocalDateTime nuevaFechaLimite) {
        this.tarea = tarea;
        this.alumno = alumno;
        this.equipo = equipo;
        this.nuevaFechaLimite = nuevaFechaLimite;
    }

    public Integer getIdProrroga() {
        return idProrroga;
    }

    public void setIdProrroga(Integer idProrroga) {
        this.idProrroga = idProrroga;
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

    public LocalDateTime getNuevaFechaLimite() {
        return nuevaFechaLimite;
    }

    public void setNuevaFechaLimite(LocalDateTime nuevaFechaLimite) {
        this.nuevaFechaLimite = nuevaFechaLimite;
    }

    public Boolean getAprobada() {
        return aprobada;
    }

    public void setAprobada(Boolean aprobada) {
        this.aprobada = aprobada;
    }
}
