package com.uv.backend.repository;

import com.uv.backend.entity.Equipo;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Equipo}.
 */
public interface IEquipo extends CrudRepository<Equipo, Integer> {

    /**
     * Lista los equipos de una tarea.
     *
     * @param idTarea identificador de la tarea
     * @return equipos de la tarea
     */
    List<Equipo> findByTareaIdTarea(Integer idTarea);
}
