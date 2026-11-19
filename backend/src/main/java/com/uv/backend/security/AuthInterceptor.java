package com.uv.backend.security;

import com.uv.backend.security.TokenService.DatosToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Revisa el token en cada peticion a /api/** (excepto login y registro).
 * - Sin token, token alterado o expirado: 401.
 * - Si la URL trae ?idUsuario=, debe ser el mismo usuario del token: 403.
 *   Asi nadie puede hacerse pasar por otro usuario cambiando el idUsuario.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Nombre del atributo de la peticion donde queda el usuario autenticado. */
    public static final String ATRIBUTO = "usuarioAutenticado";

    private final TokenService tokenService;

    public AuthInterceptor(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // las peticiones previas de CORS (OPTIONS) no llevan token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        DatosToken datos = tokenService.validar(token);
        if (datos == null) {
            return rechazar(response, HttpStatus.UNAUTHORIZED,
                    token == null ? "Falta el token de sesion" : "Token invalido o expirado");
        }
        String idUsuario = request.getParameter("idUsuario");
        if (idUsuario != null && !idUsuario.equals(String.valueOf(datos.idUsuario()))) {
            return rechazar(response, HttpStatus.FORBIDDEN, "El idUsuario no corresponde a la sesion");
        }
        request.setAttribute(ATRIBUTO, datos);
        return true;
    }

    private boolean rechazar(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(mensaje);
        return false;
    }
}
