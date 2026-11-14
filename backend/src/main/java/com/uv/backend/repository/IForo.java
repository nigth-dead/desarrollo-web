package com.uv.backend.repository;

import com.uv.backend.entity.Foro;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Foro}.
 */
public interface IForo extends CrudRepository<Foro, Integer> {

    /**
     * Lista los foros de un curso.
     *
     * @param idCurso identificador del curso
     * @return foros del curso
     */
    List<Foro> findByCursoIdCurso(Integer idCurso);
}
