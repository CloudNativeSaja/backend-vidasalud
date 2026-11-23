package cl.duoc.vidasalud.bff.controller;

import jakarta.servlet.http.HttpServletRequest;

/** Convierte la ruta publica del BFF en la ruta interna del microservicio. */
final class ProxyPaths {

    private ProxyPaths() {
    }

    /**
     * Ej.: /api/bff/report/kpis?range=last7d -> /api/report/kpis?range=last7d
     */
    static String target(HttpServletRequest request, String bffPrefix, String targetPrefix) {
        String path = request.getRequestURI()
                .substring(request.getContextPath().length() + bffPrefix.length());
        String query = request.getQueryString();
        return targetPrefix + path + (query != null ? "?" + query : "");
    }
}
