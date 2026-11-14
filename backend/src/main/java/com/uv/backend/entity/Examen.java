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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que mapea la tabla EXAMENES.
 */
@Entity
@Table(name = "EXAMENES")
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_examen")
    private Integer idExamen;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @JsonIgnore
    @OneToMany(mappedBy = "examen")
    private List<PreguntaExamen> preguntas = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "examen")
    private List<ExamenResultado> resultados = new ArrayList<>();

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public Examen() {
    }

    /**
     * Constructor con parámetros (sin idExamen, que lo genera
     * la base de datos).
     */
    public Examen(Curso curso, String titulo, LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        this.curso = curso;
        this.titulo = titulo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public Integer getIdExamen() {
        return idExamen;
    }

    public void setIdExamen(Integer idExamen) {
        this.idExamen = idExamen;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public List<PreguntaExamen> getPreguntas() {
        return preguntas;
    }

    public void setPreguntas(List<PreguntaExamen> preguntas) {
        this.preguntas = preguntas;
    }

    public List<ExamenResultado> getResultados() {
        return resultados;
    }

    public void setResultados(List<ExamenResultado> resultados) {
        this.resultados = resultados;
    }
}
