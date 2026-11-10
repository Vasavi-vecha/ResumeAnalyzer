package com.app.ResumeAnalyzer.Service;

import com.google.genai.Client;
import com.google.genai.errors.ServerException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {
    private final Client client;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 2000; // 2 seconds

    public String askGemini(List<Content> conversation) {
        Content systemInstruction = Content.builder()
                .role("system")
                .parts(List.of(
                        Part.builder()
                                .text("How AI Works?")
                                .build()
                ))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstruction)
                .build();

        int attempt = 0;
        long delay = INITIAL_DELAY_MS;

        while (attempt < MAX_RETRIES) {
            try {
                GenerateContentResponse response = client.models.generateContent(
                        "gemini-2.5-flash",
                        conversation,
                        config);
                return response.text();

            } catch (ServerException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("Max retries reached. Gemini API still overloaded.");
                    throw new RuntimeException("Gemini API is overloaded after " + MAX_RETRIES + " attempts. Please try again later.", e);
                }

                log.warn("Gemini API overloaded. Retrying attempt {}/{} after {}ms delay...", attempt, MAX_RETRIES, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                delay *= 2; // Exponential backoff
            }
        }

        throw new RuntimeException("Failed to get response from Gemini");
    }
}
