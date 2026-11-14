package com.uv.backend.repository;

import com.uv.backend.entity.Notificacion;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Notificacion}.
 */
public interface INotificacion extends CrudRepository<Notificacion, Integer> {

    /**
     * Lista las notificaciones no leídas de un usuario.
     *
     * @param idUsuario identificador del usuario
     * @return notificaciones pendientes del usuario
     */
    List<Notificacion> findByUsuarioIdUsuarioAndLeidoFalse(Integer idUsuario);
}
