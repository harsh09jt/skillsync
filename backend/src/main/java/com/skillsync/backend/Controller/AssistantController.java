package com.skillsync.backend.Controller;

import com.skillsync.backend.ai.agent.SupervisorAgent;
import com.skillsync.backend.ai.dto.AssistantDtos.Request;
import com.skillsync.backend.ai.dto.AssistantDtos.Response;
import com.skillsync.backend.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final SupervisorAgent supervisorAgent;

    public AssistantController(SupervisorAgent supervisorAgent) {
        this.supervisorAgent = supervisorAgent;
    }

    @PostMapping("/analyze")
    @ResponseStatus(HttpStatus.OK)
    public Response analyze(
            Authentication authentication,
            @Valid @RequestBody Request request
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return supervisorAgent.handle(user, request);
    }
}
