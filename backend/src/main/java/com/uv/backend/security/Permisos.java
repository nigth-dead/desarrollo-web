package com.uv.backend.security;

import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IUsuario;
import com.uv.backend.security.TokenService.DatosToken;
import org.springframework.stereotype.Component;

/**
 * Permisos basados en el usuario del token.
 * El rol se consulta en la base de datos (no en el token) para que un cambio
 * de rol aplique de inmediato aunque el token se haya generado antes.
 *
 * Si la autenticacion esta desactivada (app.auth.requerir-token=false) no hay
 * sesion y estas validaciones se omiten, como antes de agregar el token.
 */
@Component
public class Permisos {

    private final IUsuario iUsuario;

    public Permisos(IUsuario iUsuario) {
        this.iUsuario = iUsuario;
    }

    /** No hay sesion porque la autenticacion esta desactivada. */
    public boolean sinAutenticacion(DatosToken sesion) {
        return sesion == null;
    }

    /** El usuario de la sesion es ADMIN. */
    public boolean esAdmin(DatosToken sesion) {
        if (sesion == null) {
            return true;
        }
        return iUsuario.findById(sesion.idUsuario())
                .map(Usuario::getRol)
                .map("ADMIN"::equals)
                .orElse(false);
    }

    /** El usuario de la sesion es el indicado. */
    public boolean esElMismo(DatosToken sesion, Integer idUsuario) {
        return sesion == null || sesion.idUsuario().equals(idUsuario);
    }

    /** Rol actual del usuario de la sesion (null si no hay sesion o no existe). */
    public String rol(DatosToken sesion) {
        if (sesion == null) {
            return null;
        }
        return iUsuario.findById(sesion.idUsuario()).map(Usuario::getRol).orElse(null);
    }
}
