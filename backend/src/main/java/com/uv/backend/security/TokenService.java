package com.uv.backend.security;

import com.uv.backend.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genera y valida tokens JWT firmados con HMAC-SHA256 (HS256).
 * El token guarda el id del usuario (sub), su rol y la fecha de expiracion (exp).
 */
@Service
public class TokenService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER = B64.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private static final Pattern SUB = Pattern.compile("\"sub\":\"(\\d+)\"");
    private static final Pattern ROL = Pattern.compile("\"rol\":\"(\\w+)\"");
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");

    private final byte[] secreto;
    private final long duracionSegundos;

    public TokenService(@Value("${app.jwt.secreto}") String secreto,
            @Value("${app.jwt.duracion-horas:8}") long duracionHoras) {
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.duracionSegundos = duracionHoras * 3600;
    }

    /** Datos que se obtienen de un token valido. */
    public record DatosToken(Integer idUsuario, String rol, long expira) {}

    /** Crea el token de un usuario que inicio sesion. */
    public String generar(Usuario usuario) {
        long exp = Instant.now().getEpochSecond() + duracionSegundos;
        String payload = "{\"sub\":\"" + usuario.getIdUsuario() + "\",\"rol\":\"" + usuario.getRol()
                + "\",\"exp\":" + exp + "}";
        String cuerpo = HEADER + "." + B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return cuerpo + "." + firmar(cuerpo);
    }

    /** Devuelve los datos del token, o null si es invalido, esta alterado o ya expiro. */
    public DatosToken validar(String token) {
        if (token == null) {
            return null;
        }
        String[] partes = token.split("\\.");
        if (partes.length != 3 || !partes[0].equals(HEADER)) {
            return null;
        }
        String esperada = firmar(partes[0] + "." + partes[1]);
        // comparacion en tiempo constante para no filtrar informacion de la firma
        if (!MessageDigest.isEqual(esperada.getBytes(StandardCharsets.UTF_8),
                partes[2].getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        String payload;
        try {
            payload = new String(B64D.decode(partes[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Matcher sub = SUB.matcher(payload);
        Matcher rol = ROL.matcher(payload);
        Matcher exp = EXP.matcher(payload);
        if (!sub.find() || !rol.find() || !exp.find()) {
            return null;
        }
        long expira = Long.parseLong(exp.group(1));
        if (Instant.now().getEpochSecond() >= expira) {
            return null;
        }
        return new DatosToken(Integer.valueOf(sub.group(1)), rol.group(1), expira);
    }

    private String firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return B64.encodeToString(mac.doFinal(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }
}
