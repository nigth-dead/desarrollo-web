package com.uv.backend.repository;

import com.uv.backend.entity.ForoComentario;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link ForoComentario}.
 */
public interface IForoComentario extends CrudRepository<ForoComentario, Integer> {

    /**
     * Lista los comentarios de un foro.
     *
     * @param idForo identificador del foro
     * @return comentarios del foro
     */
    List<ForoComentario> findByForoIdForo(Integer idForo);
}
