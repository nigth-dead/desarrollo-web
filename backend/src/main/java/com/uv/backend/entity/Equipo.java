package com.uv.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que mapea la tabla EQUIPOS.
 *
 * <p>Los integrantes se mapean con {@code @ManyToMany} sobre la
 * tabla intermedia EQUIPO_INTEGRANTES, sin entidad propia.</p>
 */
@Entity
@Table(name = "EQUIPOS")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipo")
    private Integer idEquipo;

    @ManyToOne
    @JoinColumn(name = "id_tarea", nullable = false)
    private Tarea tarea;

    @Column(name = "nombre_equipo", nullable = false, length = 255)
    private String nombreEquipo;

    @ManyToMany
    @JoinTable(name = "EQUIPO_INTEGRANTES",
            joinColumns = @JoinColumn(name = "id_equipo"),
            inverseJoinColumns = @JoinColumn(name = "id_alumno"))
    private List<Usuario> integrantes = new ArrayList<>();

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public Equipo() {
    }

    /**
     * Constructor con parámetros (sin idEquipo, que lo genera
     * la base de datos).
     */
    public Equipo(Tarea tarea, String nombreEquipo) {
        this.tarea = tarea;
        this.nombreEquipo = nombreEquipo;
    }

    public Integer getIdEquipo() {
        return idEquipo;
    }

    public void setIdEquipo(Integer idEquipo) {
        this.idEquipo = idEquipo;
    }

    public Tarea getTarea() {
        return tarea;
    }

    public void setTarea(Tarea tarea) {
        this.tarea = tarea;
    }

    public String getNombreEquipo() {
        return nombreEquipo;
    }

    public void setNombreEquipo(String nombreEquipo) {
        this.nombreEquipo = nombreEquipo;
    }

    public List<Usuario> getIntegrantes() {
        return integrantes;
    }

    public void setIntegrantes(List<Usuario> integrantes) {
        this.integrantes = integrantes;
    }
}
