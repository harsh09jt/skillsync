package com.skillsync.backend.ai.tools;

import com.skillsync.backend.entity.User;
import com.skillsync.backend.service.SkillSyncDomainService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SkillSyncAgentTools {

    private final SkillSyncDomainService domainService;

    public SkillSyncAgentTools(SkillSyncDomainService domainService) {
        this.domainService = domainService;
    }

    public List<Map<String, Object>> employeeSkills(User user) {
        return domainService.employeeSkills(user, null);
    }

    public List<Map<String, Object>> jobRoles() {
        return domainService.jobRoles();
    }

    public List<Map<String, Object>> requiredSkills(User user, long jobRoleId) {
        return domainService.requiredSkillsForAssistant(user, jobRoleId);
    }

    public List<Map<String, Object>> assessments(User user) {
        return domainService.assessments(user, null);
    }

    public Long findRoleId(User user, String query, Long requestedRoleId) {
        List<Map<String, Object>> roles = jobRoles();
        if (requestedRoleId != null) {
            return roles.stream()
                    .filter(role -> number(role.get("id")) == requestedRoleId)
                    .map(role -> number(role.get("id")))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected job role was not found."));
        }

        String normalizedQuery = query.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
        return roles.stream()
                .filter(role -> role.get("name") != null)
                .filter(role -> {
                    String name = role.get("name").toString().toLowerCase();
                    return normalizedQuery.contains(name)
                            || name.split("\\s+").length > 1
                            && java.util.Arrays.stream(name.split("\\s+"))
                                    .filter(token -> token.length() > 2)
                                    .allMatch(normalizedQuery::contains);
                })
                .map(role -> number(role.get("id")))
                .findFirst()
                .orElseGet(() -> roles.stream()
                        .filter(role -> number(role.get("requiredSkillCount")) > 0)
                        .map(role -> number(role.get("id")))
                        .findFirst()
                        .orElse(null));
    }

    public String roleName(long roleId) {
        return jobRoles().stream()
                .filter(role -> number(role.get("id")) == roleId)
                .map(role -> Objects.toString(role.get("name"), ""))
                .findFirst()
                .orElse("");
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
