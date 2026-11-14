package com.uv.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "CURSOS")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCurso;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String codigoInvitacion;

    private String descripcion;

    @Column(nullable = false)
    private Integer idMaestro;

    public Integer getIdCurso() { return idCurso; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigoInvitacion() { return codigoInvitacion; }
    public void setCodigoInvitacion(String codigoInvitacion) { this.codigoInvitacion = codigoInvitacion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Integer getIdMaestro() { return idMaestro; }
    public void setIdMaestro(Integer idMaestro) { this.idMaestro = idMaestro; }
}
