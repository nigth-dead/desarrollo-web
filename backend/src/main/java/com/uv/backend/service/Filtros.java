package com.uv.backend.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades para los filtros de busqueda de los GET (?q=, ?estado=, etc.).
 */
public final class Filtros {

    private Filtros() {
    }

    /**
     * Indica si alguno de los textos contiene la busqueda, sin importar
     * mayusculas ni acentos. Si la busqueda viene vacia, todo coincide.
     */
    public static boolean contiene(String busqueda, String... textos) {
        if (busqueda == null || busqueda.isBlank()) {
            return true;
        }
        String q = normalizar(busqueda);
        for (String t : textos) {
            if (t != null && normalizar(t).contains(q)) {
                return true;
            }
        }
        return false;
    }

    /** Pasa a minusculas y quita acentos: "Práctica" -> "practica". */
    public static String normalizar(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Valida que el valor de un filtro sea uno de los permitidos (sin importar mayusculas). */
    public static boolean valorValido(String valor, String... permitidos) {
        if (valor == null) {
            return true;
        }
        for (String p : permitidos) {
            if (p.equalsIgnoreCase(valor)) {
                return true;
            }
        }
        return false;
    }
}
