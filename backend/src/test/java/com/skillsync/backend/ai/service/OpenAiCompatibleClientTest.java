package com.skillsync.backend.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAiCompatibleClientTest {

    private static final String GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

    @Test
    void sendsCompletionToGeminiOpenAiCompatibleEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GEMINI_ENDPOINT))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"SKILL_GAP"}}]}
                        """, MediaType.APPLICATION_JSON));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "test-key",
                "https://generativelanguage.googleapis.com/v1beta/openai/",
                "gemini-3.1-flash-lite",
                builder
        );

        assertEquals(Optional.of("SKILL_GAP"), client.complete("system", "user"));
        server.verify();
    }

    @Test
    void givesActionableMessageWhenGeminiRejectsTheKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(GEMINI_ENDPOINT))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        OpenAiCompatibleClient client = new OpenAiCompatibleClient(
                "test-key",
                "https://generativelanguage.googleapis.com/v1beta/openai/",
                "gemini-3.1-flash-lite",
                builder
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> client.complete("system", "user")
        );

        assertTrue(exception.getReason().contains("Gemini rejected the configured API key"));
        assertTrue(exception.getReason().contains("GEMINI_API_KEY"));
        server.verify();
    }
}
