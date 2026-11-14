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

/**
 * Entidad que mapea la tabla RESPUESTAS_ALUMNO.
 */
@Entity
@Table(name = "RESPUESTAS_ALUMNO")
public class RespuestaAlumno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Integer idRespuesta;

    @ManyToOne
    @JoinColumn(name = "id_resultado", nullable = false)
    private ExamenResultado resultado;

    @ManyToOne
    @JoinColumn(name = "id_pregunta", nullable = false)
    private PreguntaExamen pregunta;

    @Column(name = "respuesta_dada", columnDefinition = "TEXT")
    private String respuestaDada;

    @Column(name = "puntaje_obtenido", precision = 5, scale = 2)
    private BigDecimal puntajeObtenido;

    @Column(name = "comentario_maestro", columnDefinition = "TEXT")
    private String comentarioMaestro;

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public RespuestaAlumno() {
    }

    /**
     * Constructor con parámetros (sin idRespuesta, que lo genera
     * la base de datos).
     */
    public RespuestaAlumno(ExamenResultado resultado, PreguntaExamen pregunta, String respuestaDada) {
        this.resultado = resultado;
        this.pregunta = pregunta;
        this.respuestaDada = respuestaDada;
    }

    public Integer getIdRespuesta() {
        return idRespuesta;
    }

    public void setIdRespuesta(Integer idRespuesta) {
        this.idRespuesta = idRespuesta;
    }

    public ExamenResultado getResultado() {
        return resultado;
    }

    public void setResultado(ExamenResultado resultado) {
        this.resultado = resultado;
    }

    public PreguntaExamen getPregunta() {
        return pregunta;
    }

    public void setPregunta(PreguntaExamen pregunta) {
        this.pregunta = pregunta;
    }

    public String getRespuestaDada() {
        return respuestaDada;
    }

    public void setRespuestaDada(String respuestaDada) {
        this.respuestaDada = respuestaDada;
    }

    public BigDecimal getPuntajeObtenido() {
        return puntajeObtenido;
    }

    public void setPuntajeObtenido(BigDecimal puntajeObtenido) {
        this.puntajeObtenido = puntajeObtenido;
    }

    public String getComentarioMaestro() {
        return comentarioMaestro;
    }

    public void setComentarioMaestro(String comentarioMaestro) {
        this.comentarioMaestro = comentarioMaestro;
    }
}
