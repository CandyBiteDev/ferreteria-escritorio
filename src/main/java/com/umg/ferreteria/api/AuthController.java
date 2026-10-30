package com.umg.ferreteria.api;

import com.umg.ferreteria.modelo.Usuario;
import com.umg.ferreteria.seguridad.AuthInterceptor;
import com.umg.ferreteria.seguridad.SesionManager;
import com.umg.ferreteria.servicio.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record LoginRequest(String username, String password) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("estado", "ok", "sistema", "Ferretería Multisucursal");
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        Usuario u = auth.autenticar(req.username(), req.password(), "app móvil");
        String token = SesionManager.getInstancia().emitirToken(u);
        return Map.of("token", token, "usuario", u);
    }

    @PostMapping("/auth/logout")
    public Map<String, String> logout(@RequestHeader("Authorization") String header) {
        SesionManager.getInstancia().revocarToken(header.substring(7).trim());
        return Map.of("estado", "sesión cerrada");
    }

    @GetMapping("/auth/yo")
    public Usuario yo(@RequestAttribute(AuthInterceptor.ATRIBUTO_USUARIO) Usuario u) {
        return u;
    }
}
