package com.skillsync.backend.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAiCompatibleClient {

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    @Autowired
    public OpenAiCompatibleClient(
            @Value("${skillsync.ai.api-key:}") String apiKey,
            @Value("${skillsync.ai.base-url:https://generativelanguage.googleapis.com/v1beta/openai/}") String baseUrl,
            @Value("${skillsync.ai.model:gemini-3.1-flash-lite}") String model
    ) {
        this(apiKey, baseUrl, model, RestClient.builder());
    }

    OpenAiCompatibleClient(
            String apiKey,
            String baseUrl,
            String model,
            RestClient.Builder restClientBuilder
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    public Optional<String> complete(String systemPrompt, String userPrompt) {
        if (!isConfigured()) return Optional.empty();
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        try {
            ChatCompletion completion = restClient.post()
                    .uri("chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(ChatCompletion.class);
            if (completion == null || completion.choices() == null || completion.choices().isEmpty()) {
                return Optional.empty();
            }
            Message message = completion.choices().getFirst().message();
            return message == null || message.content() == null
                    ? Optional.empty()
                    : Optional.of(message.content().trim());
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            String reason = switch (status) {
                case 401, 403 -> "Gemini rejected the configured API key or the key is not authorized.";
                case 404 -> "Gemini could not find the configured model or API endpoint.";
                case 429 -> "Gemini rate limit or quota was reached.";
                default -> "Gemini returned HTTP " + status + ".";
            };
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    reason + " Check GEMINI_API_KEY, GEMINI_BASE_URL, and GEMINI_MODEL.",
                    exception
            );
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not reach Gemini. Check the network and GEMINI_BASE_URL.",
                    exception
            );
        }
    }

    private record ChatCompletion(List<Choice> choices) {}
    private record Choice(Message message) {}
    private record Message(String content) {}
}
