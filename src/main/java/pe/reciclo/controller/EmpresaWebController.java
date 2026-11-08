package pe.reciclo.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.reciclo.model.UsuarioSesion;
import pe.reciclo.service.RegistroNoEncontradoException;
import pe.reciclo.service.RegistroResiduoService;

@Controller
public class EmpresaWebController {

    private final RegistroResiduoService service;

    public EmpresaWebController(RegistroResiduoService service) {
        this.service = service;
    }

    @GetMapping("/empresa")
    public String catalogo(HttpSession session, Model model) {
        String actual = prepararEmpresa(session, model);
        model.addAttribute("ofertas", service.disponiblesPara(actual));
        return "empresa/catalogo";
    }

    @GetMapping("/empresa/materiales/{id}")
    public String detalle(@PathVariable long id, HttpSession session, Model model) {
        String actual = prepararEmpresa(session, model);
        model.addAttribute("residuo", service.consultarDisponible(id, actual));
        return "empresa/detalle";
    }

    private String prepararEmpresa(HttpSession session, Model model) {
        UsuarioSesion usuario = (UsuarioSesion) session.getAttribute("usuario");
        String actual = usuario == null || usuario.empresa().isBlank()
                ? "Empresa Demo"
                : usuario.empresa();

        model.addAttribute("empresaActual", actual);
        model.addAttribute("empresas", service.empresasDeDemostracion());
        return actual;
    }

    @ExceptionHandler(RegistroNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noDisponible(HttpServletRequest request, HttpSession session, Model model) {
        prepararEmpresa(session, model); 
        model.addAttribute("vistaEmpresa", true);
        model.addAttribute("status", 404);
        model.addAttribute("mensajeError", "Este material no está disponible para la empresa seleccionada.");
        return "error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String identificadorInvalido(HttpSession session, Model model) {
        prepararEmpresa(session, model);
        model.addAttribute("vistaEmpresa", true);
        model.addAttribute("status", 400);
        model.addAttribute("mensajeError", "El identificador del material debe ser un número entero.");
        return "error";
    }
}
