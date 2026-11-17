package com.uv.backend.controller;

import java.util.Comparator;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uv.backend.entity.Notificacion;
import com.uv.backend.repository.INotificacion;
import com.uv.backend.repository.IUsuario;

@RestController 
@RequestMapping ("/api")
public class NotificacionController {
    @Autowired
    INotificacion iNotificacion;
    @Autowired
    IUsuario iUsuario;

    //metodo para validar permisos
    private boolean tienePermiso(Notificacion notificacion, Integer idUsuario) {
        return notificacion.getUsuario() != null &&
                notificacion.getUsuario().getIdUsuario().equals(idUsuario);
    }

    //marcar una como leída
    @PatchMapping ("/notificaciones/{id}/leida")
    public Notificacion marcarLeida(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iNotificacion.existsById(id) || !iUsuario.existsById(idUsuario)) {
            return null;
        }
        Notificacion notificacion = iNotificacion.findById(id).get();
        if (!tienePermiso(notificacion, idUsuario)) {
            return null;
        }
        notificacion.setLeido(true);
        iNotificacion.save(notificacion);
        return notificacion;
    }

    //marcar todas como leídas
    @PatchMapping ("/notificaciones/leidas")
    public String marcarTodasLeidas(@RequestParam Integer idUsuario) {
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }
        List<Notificacion> pendientes = iNotificacion.findByUsuarioIdUsuarioAndLeidoFalse(idUsuario);
        for (Notificacion n : pendientes) {
            n.setLeido(true);
        }
        iNotificacion.saveAll(pendientes);
        return pendientes.size() + " notificaciones marcadas como leídas";
    }

    //eliminar notificación
    @DeleteMapping ("/notificaciones/{id}")
    public String eliminarNotificacion(@PathVariable Integer id,
        @RequestParam Integer idUsuario) {
        if (!iNotificacion.existsById(id)) {
            return "La notificación no existe";
        }
        if (!iUsuario.existsById(idUsuario)) {
            return "El usuario no existe";
        }
        Notificacion notificacion = iNotificacion.findById(id).get();
        if (!tienePermiso(notificacion, idUsuario)) {
            return "Usuario no autorizado";
        }
        iNotificacion.deleteById(id);
        return "Notificación eliminada";
    }

    // ===== GET =====

    //ver mis notificaciones (filtro opcional ?leido=false), las mas recientes primero
    @GetMapping ("/notificaciones")
    public ResponseEntity<?> misNotificaciones(@RequestParam Integer idUsuario,
            @RequestParam(required = false) Boolean leido) {
        if (!iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
        }
        return ResponseEntity.ok(iNotificacion.findByUsuarioIdUsuario(idUsuario).stream()
                .filter(n -> leido == null || leido.equals(Boolean.TRUE.equals(n.getLeido())))
                .sorted(Comparator.comparing(Notificacion::getIdNotificacion).reversed())
                .toList());
    }

    //contar no leidas (para el icono de la campana)
    @GetMapping ("/notificaciones/no-leidas/total")
    public ResponseEntity<?> totalNoLeidas(@RequestParam Integer idUsuario) {
        if (!iUsuario.existsById(idUsuario)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("El usuario no existe");
        }
        long total = iNotificacion.findByUsuarioIdUsuario(idUsuario).stream()
                .filter(n -> !Boolean.TRUE.equals(n.getLeido()))
                .count();
        return ResponseEntity.ok(Map.of("total", total));
    }
}
