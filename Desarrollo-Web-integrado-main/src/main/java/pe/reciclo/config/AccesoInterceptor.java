package pe.reciclo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.reciclo.model.Rol;
import pe.reciclo.model.UsuarioSesion;

@Component
public class AccesoInterceptor implements HandlerInterceptor {

    private final boolean seguridadActiva;

    public AccesoInterceptor(@Value("${reciclo.seguridad.activa:true}") boolean seguridadActiva) {
        this.seguridadActiva = seguridadActiva;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!seguridadActiva) {
            return true;
        }

        String ruta = request.getRequestURI().substring(request.getContextPath().length());

        if (ruta.equals("/") || ruta.equals("/login") || ruta.equals("/error")
                || ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/api/")) {
            return true;
        }

        UsuarioSesion usuario = (UsuarioSesion) request.getSession().getAttribute("usuario");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        if (ruta.startsWith("/admin") || ruta.startsWith("/residuos")) {
            if (usuario.rol() != Rol.ADMIN) {
                response.sendRedirect(request.getContextPath() + "/acceso-denegado");
                return false;
            }
        }

        if (ruta.startsWith("/empresa") && usuario.rol() != Rol.EMPRESA) {
            response.sendRedirect(request.getContextPath() + "/acceso-denegado");
            return false;
        }

        return true;
    }
}
