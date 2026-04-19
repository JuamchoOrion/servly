package co.edu.uniquindio.servly.controller;

import co.edu.uniquindio.servly.DTO.RecipeChatResponse;
import co.edu.uniquindio.servly.service.RecipeService;
import co.edu.uniquindio.servly.model.entity.Recipe;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final RecipeService recipeService;

    @PostMapping("/consulta")
    public ResponseEntity<?> consulta(@RequestBody Map<String, String> body) {
        String pregunta = body != null ? body.get("pregunta") : null;

        List<Recipe> recetas = recipeService.searchRecipes(pregunta);

        List<RecipeChatResponse> respuesta = recetas.stream()
                .map(RecipeChatResponse::new)
                .collect(Collectors.toList());

        long disponibles = respuesta.stream().filter(RecipeChatResponse::isDisponible).count();
        long noDisponibles = respuesta.size() - disponibles;

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("recetas", respuesta);
        resultado.put("total", respuesta.size());
        resultado.put("disponibles", disponibles);
        resultado.put("noDisponibles", noDisponibles);

        return ResponseEntity.ok(resultado);
    }
}
