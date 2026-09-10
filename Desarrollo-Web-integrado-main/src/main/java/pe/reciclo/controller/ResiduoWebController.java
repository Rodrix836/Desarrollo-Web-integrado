package pe.reciclo.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.reciclo.model.RegistroResiduo;
import pe.reciclo.model.TipoResiduo;
import pe.reciclo.service.RegistroNoEncontradoException;
import pe.reciclo.service.RegistroResiduoService;

import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class ResiduoWebController {

    private final RegistroResiduoService service;

    public ResiduoWebController(RegistroResiduoService service) {
        this.service = service;
    }

    @ModelAttribute("tiposResiduo")
    public TipoResiduo[] tiposResiduo() {
        return TipoResiduo.values();
    }

    @GetMapping("/")
    public String inicio() {
        return "inicio";
    }

    @GetMapping({"/residuos", "/admin"})
    public String listar(Model model) {
        model.addAttribute("residuos", service.listar());
        return "residuos/lista";
    }

    @GetMapping("/residuos/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("residuo", new RegistroResiduo(null, null, null, "kg", LocalDate.now(), ""));
        prepararFormulario(model, null, null);
        return "residuos/formulario";
    }

    @PostMapping("/residuos")
    public String registrar(@Valid @ModelAttribute("residuo") RegistroResiduo residuo,
                            BindingResult resultado, Model model, HttpServletResponse response,
                            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            prepararFormulario(model, null, resultado);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "residuos/formulario";
        }
        service.crear(residuo);
        redirect.addFlashAttribute("mensaje", "Residuo registrado correctamente.");
        return "redirect:/residuos";
    }

    @GetMapping("/residuos/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("residuo", service.consultar(id));
        return "residuos/detalle";
    }

    @GetMapping("/residuos/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        model.addAttribute("residuo", service.consultar(id));
        prepararFormulario(model, id, null);
        return "residuos/formulario";
    }

    @PostMapping("/residuos/{id}/editar")
    public String actualizar(@PathVariable long id,
                             @Valid @ModelAttribute("residuo") RegistroResiduo residuo,
                             BindingResult resultado, Model model, HttpServletResponse response,
                             RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            prepararFormulario(model, id, resultado);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "residuos/formulario";
        }
        service.actualizar(id, residuo);
        redirect.addFlashAttribute("mensaje", "Residuo actualizado correctamente.");
        return "redirect:/residuos";
    }

    @GetMapping("/residuos/{id}/eliminar")
    public String confirmarEliminacion(@PathVariable long id, Model model) {
        model.addAttribute("residuo", service.consultar(id));
        return "residuos/eliminar";
    }

    @PostMapping("/residuos/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes redirect) {
        service.eliminar(id);
        redirect.addFlashAttribute("mensaje", "Residuo eliminado correctamente.");
        return "redirect:/residuos";
    }

    @ExceptionHandler(RegistroNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String noEncontrado(RegistroNoEncontradoException exception, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("mensajeError", exception.getMessage());
        return "error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String identificadorInvalido(Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("mensajeError", "El identificador del residuo debe ser un número entero.");
        return "error";
    }

    @ExceptionHandler(UncheckedIOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String errorDeArchivo(Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("mensajeError", "No se pudieron guardar los cambios. Inténtalo nuevamente.");
        return "error";
    }

    private void prepararFormulario(Model model, Long id, BindingResult resultado) {
        model.addAttribute("edicion", id != null);
        model.addAttribute("accionFormulario", id == null ? "/residuos" : "/residuos/" + id + "/editar");
        Map<String, String> errores = new LinkedHashMap<>();
        if (resultado != null) {
            for (FieldError error : resultado.getFieldErrors()) {
                errores.putIfAbsent(error.getField(), mensajeDeCampo(error));
            }
        }
        model.addAttribute("errores", errores);
    }

    private String mensajeDeCampo(FieldError error) {
        if (!error.isBindingFailure()) {
            return error.getDefaultMessage();
        }
        return switch (error.getField()) {
            case "tipoResiduo" -> "Selecciona un tipo de residuo válido.";
            case "cantidad" -> "Ingresa una cantidad numérica.";
            case "fecha" -> "Ingresa una fecha válida.";
            default -> "Revisa este valor.";
        };
    }
}
