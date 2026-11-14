package com.uv.backend.repository;

import com.uv.backend.entity.Prorroga;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Prorroga}.
 */
public interface IProrroga extends CrudRepository<Prorroga, Integer> {

    /**
     * Lista las prórrogas de una tarea.
     *
     * @param idTarea identificador de la tarea
     * @return prórrogas de la tarea
     */
    List<Prorroga> findByTareaIdTarea(Integer idTarea);
}
