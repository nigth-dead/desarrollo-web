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
 * Entidad que mapea la tabla FOROS.
 *
 * <p>El campo {@code fechaCreacion} lo llena la base de datos
 * (DEFAULT CURRENT_TIMESTAMP), por eso se marca como no insertable
 * ni actualizable desde JPA.</p>
 */
@Entity
@Table(name = "FOROS")
public class Foro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_foro")
    private Integer idForo;

    @ManyToOne
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @Column(name = "titulo", nullable = false, length = 255)
    private String titulo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @JsonIgnore
    @OneToMany(mappedBy = "foro")
    private List<ForoComentario> comentarios = new ArrayList<>();

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public Foro() {
    }

    /**
     * Constructor con parámetros (sin idForo, que lo genera
     * la base de datos).
     */
    public Foro(Curso curso, String titulo, String descripcion) {
        this.curso = curso;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public Integer getIdForo() {
        return idForo;
    }

    public void setIdForo(Integer idForo) {
        this.idForo = idForo;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<ForoComentario> getComentarios() {
        return comentarios;
    }

    public void setComentarios(List<ForoComentario> comentarios) {
        this.comentarios = comentarios;
    }
}
