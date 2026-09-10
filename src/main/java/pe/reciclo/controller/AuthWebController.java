package pe.reciclo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.reciclo.model.Rol;
import pe.reciclo.model.UsuarioSesion;
import pe.reciclo.service.AuthService;

@Controller
public class AuthWebController {

    private final AuthService authService;

    public AuthWebController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login(HttpSession session, Model model) {
        if (session.getAttribute("usuario") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String autenticar(@RequestParam String username,
                             @RequestParam String password,
                             HttpSession session,
                             Model model) {

        var usuario = authService.autenticar(username, password);

        if (usuario.isEmpty()) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            model.addAttribute("username", username);
            return "login";
        }

        UsuarioSesion sesion = usuario.get();
        session.setAttribute("usuario", sesion);

        return sesion.rol() == Rol.ADMIN
                ? "redirect:/admin"
                : "redirect:/empresa";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
}
