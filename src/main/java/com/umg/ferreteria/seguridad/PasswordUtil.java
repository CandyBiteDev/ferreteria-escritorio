package com.umg.ferreteria.seguridad;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash seguro de contraseñas con PBKDF2-HMAC-SHA256 + sal aleatoria.
 * Formato guardado en BD: pbkdf2$iteraciones$salBase64$hashBase64
 */
public final class PasswordUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int LARGO_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(String password) {
        byte[] sal = new byte[16];
        RANDOM.nextBytes(sal);
        byte[] h = derivar(password.toCharArray(), sal, ITERACIONES);
        return "pbkdf2$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(sal) + "$" + Base64.getEncoder().encodeToString(h);
    }

    public static boolean verificar(String password, String guardado) {
        if (password == null || guardado == null) return false;
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2")) return false;
        int iteraciones = Integer.parseInt(partes[1]);
        byte[] sal = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        byte[] calculado = derivar(password.toCharArray(), sal, iteraciones);
        // Comparación en tiempo constante
        return MessageDigest.isEqual(esperado, calculado);
    }

    private static byte[] derivar(char[] password, byte[] sal, int iteraciones) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, sal, iteraciones, LARGO_BITS);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash", e);
        }
    }
}
