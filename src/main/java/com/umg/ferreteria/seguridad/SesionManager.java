package com.umg.ferreteria.seguridad;

import com.umg.ferreteria.modelo.Usuario;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PATRÓN SINGLETON: una única instancia en toda la aplicación guarda
 * - el usuario con sesión abierta en el escritorio (Swing), y
 * - los tokens emitidos a la app móvil (API), con expiración.
 */
public final class SesionManager {

    private static final SesionManager INSTANCIA = new SesionManager();
    private static final Duration DURACION_TOKEN = Duration.ofHours(8);

    private record SesionApi(Usuario usuario, Instant expira) {
    }

    private final SecureRandom random = new SecureRandom();
    private final Map<String, SesionApi> tokens = new ConcurrentHashMap<>();
    private volatile Usuario usuarioEscritorio;

    private SesionManager() {
    }

    public static SesionManager getInstancia() {
        return INSTANCIA;
    }

    // ---------- Escritorio ----------
    public void iniciarSesion(Usuario usuario) {
        this.usuarioEscritorio = usuario;
    }

    public void cerrarSesion() {
        this.usuarioEscritorio = null;
    }

    public Usuario getUsuarioActual() {
        return usuarioEscritorio;
    }

    // ---------- API móvil ----------
    public String emitirToken(Usuario usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, new SesionApi(usuario, Instant.now().plus(DURACION_TOKEN)));
        return token;
    }

    public Optional<Usuario> validarToken(String token) {
        if (token == null) return Optional.empty();
        SesionApi s = tokens.get(token);
        if (s == null) return Optional.empty();
        if (Instant.now().isAfter(s.expira())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(s.usuario());
    }

    public void revocarToken(String token) {
        if (token != null) tokens.remove(token);
    }
}
