package com.uv.backend.repository;

import com.uv.backend.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CursoRepository extends JpaRepository<Curso, Integer> {
    boolean existsByCodigoInvitacion(String codigoInvitacion);
}
