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
 * Entidad que mapea la tabla PREGUNTAS_EXAMEN.
 *
 * <p>El campo {@code opcionesJson} guarda el JSON de opciones
 * tal como viene en la columna MySQL de tipo JSON.</p>
 */
@Entity
@Table(name = "PREGUNTAS_EXAMEN")
public class PreguntaExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta")
    private Integer idPregunta;

    @ManyToOne
    @JoinColumn(name = "id_examen", nullable = false)
    private Examen examen;

    @Column(name = "pregunta", nullable = false, columnDefinition = "TEXT")
    private String pregunta;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "opciones_json", columnDefinition = "json")
    private String opcionesJson;

    @Column(name = "respuesta_correcta", columnDefinition = "TEXT")
    private String respuestaCorrecta;

    @Column(name = "valor_puntos", nullable = false, precision = 5, scale = 2)
    private BigDecimal valorPuntos;

    /**
     * Constructor vacío obligatorio para JPA/Hibernate.
     */
    public PreguntaExamen() {
    }

    /**
     * Constructor con parámetros (sin idPregunta, que lo genera
     * la base de datos).
     */
    public PreguntaExamen(Examen examen, String pregunta, String tipo,
                          String opcionesJson, String respuestaCorrecta,
                          BigDecimal valorPuntos) {
        this.examen = examen;
        this.pregunta = pregunta;
        this.tipo = tipo;
        this.opcionesJson = opcionesJson;
        this.respuestaCorrecta = respuestaCorrecta;
        this.valorPuntos = valorPuntos;
    }

    public Integer getIdPregunta() {
        return idPregunta;
    }

    public void setIdPregunta(Integer idPregunta) {
        this.idPregunta = idPregunta;
    }

    public Examen getExamen() {
        return examen;
    }

    public void setExamen(Examen examen) {
        this.examen = examen;
    }

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getOpcionesJson() {
        return opcionesJson;
    }

    public void setOpcionesJson(String opcionesJson) {
        this.opcionesJson = opcionesJson;
    }

    public String getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public void setRespuestaCorrecta(String respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    public BigDecimal getValorPuntos() {
        return valorPuntos;
    }

    public void setValorPuntos(BigDecimal valorPuntos) {
        this.valorPuntos = valorPuntos;
    }
}
