package com.uv.backend.repository;

import com.uv.backend.entity.Usuario;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Usuario}.
 */
public interface IUsuario extends CrudRepository<Usuario, Integer> {

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param email correo del usuario
     * @return el usuario si existe
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Indica si ya existe un usuario registrado con ese correo.
     *
     * @param email correo a verificar
     * @return {@code true} si el correo ya está registrado
     */
    boolean existsByEmail(String email);
}
