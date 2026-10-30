package com.umg.ferreteria.seguridad;

import com.umg.ferreteria.modelo.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Exige "Authorization: Bearer <token>" en toda la API excepto el login. */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO = "usuario";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
        Optional<Usuario> u = SesionManager.getInstancia().validarToken(token);
        if (u.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Sesión inválida o expirada. Inicie sesión nuevamente.\"}");
            return false;
        }
        request.setAttribute(ATRIBUTO_USUARIO, u.get());
        return true;
    }
}
