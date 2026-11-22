package pe.reciclo.residuos.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Usuario que realiza la operación, tomado del JWT (sub y rol). Se usa para auditar y filtrar. */
public record Actor(String usuario, String rol) {

    public static final Actor ADMIN_SISTEMA = new Actor("sistema", "ADMIN");
    public static final Actor USUARIO_ANONIMO = new Actor("", "USER");

    public static Actor de(Authentication autenticacion) {
        if (autenticacion.getPrincipal() instanceof Jwt jwt) {
            return new Actor(jwt.getSubject(), jwt.getClaimAsString("rol"));
        }
        String rol = autenticacion.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("");
        return new Actor(autenticacion.getName(), rol);
    }

    public boolean admin() { return "ADMIN".equals(rol); }
}
