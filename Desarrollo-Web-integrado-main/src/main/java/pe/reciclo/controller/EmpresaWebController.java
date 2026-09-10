package pe.reciclo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.reciclo.service.RegistroNoEncontradoException;
import pe.reciclo.service.RegistroResiduoService;

@Controller
public class EmpresaWebController {

    private final RegistroResiduoService service;

    public EmpresaWebController(RegistroResiduoService service) {
        this.service = service;
    }

    @ModelAttribute("vistaEmpresa")
    public boolean vistaEmpresa() {
        return true;
    }

    @GetMapping("/empresa")
    public String catalogo(@RequestParam(defaultValue = "Empresa Demo") String empresa, Model model) {
        String actual = prepararEmpresa(empresa, model);
        model.addAttribute("ofertas", service.disponiblesPara(actual));
        return "empresa/catalogo";
    }

    @GetMapping("/empresa/materiales/{id}")
    public String detalle(@PathVariable long id,
                          @RequestParam(defaultValue = "Empresa Demo") String empresa, Model model) {
        String actual = prepararEmpresa(empresa, model);
        model.addAttribute("residuo", service.consultarDisponible(id, actual));
        return "empresa/detalle";
    }

    private String prepararEmpresa(String nombre, Model model) {
        var empresas = service.empresasDeDemostracion();
        String buscada = nombre == null ? "Empresa Demo" : nombre.trim();
        String actual = empresas.stream().filter(empresa -> empresa.equalsIgnoreCase(buscada))
                .findFirst().orElse("Empresa Demo");
        model.addAttribute("empresaActual", actual);
        model.addAttribute("empresas", empresas);
        return actual;
    }

    @ExceptionHandler(RegistroNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noDisponible(HttpServletRequest request, Model model) {
        model.addAttribute("vistaEmpresa", true);
        prepararEmpresa(request.getParameter("empresa"), model);
        model.addAttribute("status", 404);
        model.addAttribute("mensajeError", "Este material no está disponible para la empresa seleccionada.");
        return "error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String identificadorInvalido(HttpServletRequest request, Model model) {
        model.addAttribute("vistaEmpresa", true);
        prepararEmpresa(request.getParameter("empresa"), model);
        model.addAttribute("status", 400);
        model.addAttribute("mensajeError", "El identificador del material debe ser un número entero.");
        return "error";
    }
}
