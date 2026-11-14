package com.uv.backend.repository;

import com.uv.backend.entity.Entrega;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Entrega}.
 */
public interface IEntrega extends CrudRepository<Entrega, Integer> {

    /**
     * Lista las entregas de una tarea.
     *
     * @param idTarea identificador de la tarea
     * @return entregas de la tarea
     */
    List<Entrega> findByTareaIdTarea(Integer idTarea);
}
