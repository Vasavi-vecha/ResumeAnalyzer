package com.app.ResumeAnalyzer.Controller;

import com.app.ResumeAnalyzer.Entity.Evaluation;
import com.app.ResumeAnalyzer.Service.EvaluationService;
import com.app.ResumeAnalyzer.Service.GeminiService;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/ai/gemini")
@RequiredArgsConstructor
public class GeminiController {

    private final GeminiService geminiService;
    private final EvaluationService evaluationService;
    @PostMapping("/ask")
    public Map<String, String> askGeminiAPI(@RequestBody List<Map<String, String>> body){

        List<Content> conversation = body.stream().map(chat -> {
            String role = chat.get("sender");
            if(role.equalsIgnoreCase("bot")) role = "model";
            return Content.builder().role(role).parts(List.of(Part.fromText(chat.get("text")))).build();
        }).toList();

        return Map.of("result", geminiService.askGemini(conversation));
    }

    @GetMapping("/evaluations")
    public List<Evaluation> getAllEvaluations() {
        return evaluationService.getAllEvaluations();
    }
}