package com.uv.backend.repository;

import com.uv.backend.entity.Curso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para la entidad {@link Curso}.
 */
public interface ICurso extends JpaRepository<Curso, Integer> {

    /**
     * Busca un curso por su código de invitación.
     *
     * @param codigoInvitacion código de invitación del curso
     * @return el curso si existe
     */
    Optional<Curso> findByCodigoInvitacion(String codigoInvitacion);

    /**
     * Lista los cursos impartidos por un maestro.
     *
     * @param idMaestro identificador del maestro
     * @return cursos del maestro
     */
    List<Curso> findByMaestroIdUsuario(Integer idMaestro);

    /**
     * Indica si ya existe un curso con ese código de invitación.
     *
     * @param codigoInvitacion código a verificar
     * @return {@code true} si el código ya está en uso
     */
    boolean existsByCodigoInvitacion(String codigoInvitacion);
}
