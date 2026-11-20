package com.uv.backend.config;

import com.uv.backend.entity.Usuario;
import com.uv.backend.repository.IUsuario;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final IUsuario iUsuario;

    public AdminSeeder(IUsuario iUsuario) {
        this.iUsuario = iUsuario;
    }

    @Override
    public void run(String... args) {
        if (iUsuario.existsByEmail("admin@uv.mx")) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre("Administración UV");
        admin.setEmail("admin@uv.mx");
        admin.setPasswordHash(sha256("admin123"));
        admin.setRol("ADMIN");
        iUsuario.save(admin);
    }

    private static String sha256(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
