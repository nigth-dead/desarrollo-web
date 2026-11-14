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
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que mapea la tabla CURSOS.
 */
@Entity
@Table(name = "CURSOS")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Integer idCurso;

    @Column(name = "nombre", nullable = false, length = 255)
    private String nombre;

    @Column(name = "codigo_invitacion", nullable = false, unique = true, length = 100)
    private String codigoInvitacion;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_maestro", nullable = false)
    private Usuario maestro;

    @JsonIgnore
    @OneToMany(mappedBy = "curso")
    private List<CursoAlumno> alumnos = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "curso")
    private List<Tarea> tareas = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "curso")
    private List<Foro> foros = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "curso")
    private List<Examen> examenes = new ArrayList<>();

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public Curso() {
    }

    /**
     * Constructor con parámetros (sin idCurso, que lo genera
     * la base de datos).
     */
    public Curso(String nombre, String codigoInvitacion, String descripcion, Usuario maestro) {
        this.nombre = nombre;
        this.codigoInvitacion = codigoInvitacion;
        this.descripcion = descripcion;
        this.maestro = maestro;
    }

    public Integer getIdCurso() {
        return idCurso;
    }

    public void setIdCurso(Integer idCurso) {
        this.idCurso = idCurso;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCodigoInvitacion() {
        return codigoInvitacion;
    }

    public void setCodigoInvitacion(String codigoInvitacion) {
        this.codigoInvitacion = codigoInvitacion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Usuario getMaestro() {
        return maestro;
    }

    public void setMaestro(Usuario maestro) {
        this.maestro = maestro;
    }

    public List<CursoAlumno> getAlumnos() {
        return alumnos;
    }

    public void setAlumnos(List<CursoAlumno> alumnos) {
        this.alumnos = alumnos;
    }

    public List<Tarea> getTareas() {
        return tareas;
    }

    public void setTareas(List<Tarea> tareas) {
        this.tareas = tareas;
    }

    public List<Foro> getForos() {
        return foros;
    }

    public void setForos(List<Foro> foros) {
        this.foros = foros;
    }

    public List<Examen> getExamenes() {
        return examenes;
    }

    public void setExamenes(List<Examen> examenes) {
        this.examenes = examenes;
    }
}
