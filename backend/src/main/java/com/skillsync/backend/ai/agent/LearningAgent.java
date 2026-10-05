package com.skillsync.backend.ai.agent;

import com.skillsync.backend.ai.dto.AssistantDtos.LearningStep;
import com.skillsync.backend.ai.dto.AssistantDtos.SkillGap;
import com.skillsync.backend.ai.service.OpenAiCompatibleClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LearningAgent {

    private static final Map<String, LearningResource> RESOURCES = Map.ofEntries(
            Map.entry("java", new LearningResource("Dev.java", "https://dev.java/learn/")),
            Map.entry("spring", new LearningResource("Spring Guides", "https://spring.io/guides")),
            Map.entry("docker", new LearningResource("Docker Get Started", "https://docs.docker.com/get-started/")),
            Map.entry("sql", new LearningResource("MySQL Tutorial", "https://dev.mysql.com/doc/mysql-tutorial-excerpt/8.0/en/")),
            Map.entry("aws", new LearningResource("AWS Skill Builder", "https://skillbuilder.aws/")),
            Map.entry("python", new LearningResource("Python Tutorial", "https://docs.python.org/3/tutorial/")),
            Map.entry("javascript", new LearningResource("MDN JavaScript Guide", "https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide")),
            Map.entry("system design", new LearningResource("System Design Primer", "https://github.com/donnemartin/system-design-primer"))
    );

    private final OpenAiCompatibleClient aiClient;
    private final ObjectMapper objectMapper;

    public LearningAgent(OpenAiCompatibleClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public RoadmapResult roadmap(List<SkillGap> gaps, String roleName) {
        AtomicInteger order = new AtomicInteger(1);
        List<LearningStep> steps = gaps.stream()
                .filter(gap -> gap.gap() > 0)
                .sorted(java.util.Comparator.comparingInt(SkillGap::gap).reversed())
                .limit(3)
                .map(gap -> roadmapStep(gap, order.getAndIncrement()))
                .toList();
        if (steps.isEmpty() || !aiClient.isConfigured()) return new RoadmapResult(steps, false);

        String evidence = gaps.stream()
                .filter(gap -> gap.gap() > 0)
                .limit(3)
                .map(gap -> Map.of(
                        "skillName", gap.skillName(),
                        "currentLevel", gap.currentProficiency(),
                        "targetLevel", gap.requiredProficiency()
                ))
                .toList()
                .toString();
        String generated = aiClient.complete(
                """
                You are a practical learning coach. Create study topics and hands-on projects only for
                the supplied skill names. Do not invent profile facts, scores, job requirements, course
                providers, URLs, or new skills. Return only valid JSON with this shape:
                {"steps":[{"skillName":"exact supplied name","topics":["topic 1","topic 2","topic 3"],"practicalProject":"specific project"}]}
                """,
                "Target role: " + roleName + "\nRecorded skill gaps: " + evidence
        ).orElse("");
        if (generated.isBlank()) return new RoadmapResult(steps, false);

        try {
            GeneratedRoadmap roadmap = objectMapper.readValue(stripCodeFence(generated), GeneratedRoadmap.class);
            Map<String, GeneratedStep> generatedSteps = roadmap.steps() == null
                    ? Map.of()
                    : roadmap.steps().stream()
                            .filter(step -> step.skillName() != null)
                            .collect(java.util.stream.Collectors.toMap(
                                    step -> step.skillName().toLowerCase(),
                                    step -> step,
                                    (first, ignored) -> first
                            ));
            List<LearningStep> personalized = steps.stream()
                    .map(step -> mergeGeneratedContent(step, generatedSteps.get(step.skillName().toLowerCase())))
                    .toList();
            return new RoadmapResult(personalized, true);
        } catch (JacksonException exception) {
            return new RoadmapResult(steps, false);
        }
    }

    private LearningStep roadmapStep(SkillGap gap, int order) {
        String skill = gap.skillName();
        String normalized = skill.toLowerCase();
        List<String> topics = topics(normalized);
        LearningResource resource = resource(normalized);
        return new LearningStep(
                order,
                skill,
                gap.currentProficiency(),
                gap.requiredProficiency(),
                gap.gap(),
                "Build fundamentals from level " + gap.currentProficiency() + " toward level " + gap.requiredProficiency() + ".",
                topics,
                "Create a small portfolio project that applies " + skill + " to a realistic user need.",
                resource.name(),
                resource.url(),
                Math.max(1, Math.min(4, (gap.gap() + 2) / 3))
        );
    }

    private List<String> topics(String skill) {
        if (skill.contains("spring") || skill.contains("security")) {
            return List.of("Dependency injection and request lifecycle", "REST validation and error handling", "Authentication, authorization and secure API patterns");
        }
        if (skill.contains("java")) {
            return List.of("Core language and collections", "Streams, exceptions and testing", "Build a tested REST service");
        }
        if (skill.contains("docker")) {
            return List.of("Images, layers and Dockerfiles", "Containers, networking and volumes", "Compose a local multi-service development setup");
        }
        if (skill.contains("system") || skill.contains("architecture")) {
            return List.of("Capacity and latency fundamentals", "Data modeling, caching and consistency", "Draw and explain a resilient service design");
        }
        if (skill.contains("sql") || skill.contains("database")) {
            return List.of("Filtering, joins and aggregation", "Indexes and query plans", "Design and query a normalized schema");
        }
        if (skill.contains("aws") || skill.contains("cloud")) {
            return List.of("Identity and least-privilege access", "Networking and managed compute", "Deploy and observe a small cloud service");
        }
        if (skill.contains("python")) {
            return List.of("Core syntax and data structures", "Functions, modules and testing", "Build a small tested application");
        }
        return List.of("Core concepts and vocabulary", "Practice common workflows", "Apply the skill in a small end-to-end project");
    }

    private LearningResource resource(String skill) {
        return RESOURCES.entrySet().stream()
                .filter(entry -> skill.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(new LearningResource("Search official documentation", "https://www.google.com/search?q=" + java.net.URLEncoder.encode(skill + " official documentation", java.nio.charset.StandardCharsets.UTF_8)));
    }

    private LearningStep mergeGeneratedContent(LearningStep original, GeneratedStep generated) {
        if (generated == null) return original;
        List<String> topics = generated.topics() == null
                ? List.of()
                : generated.topics().stream()
                        .filter(topic -> topic != null && !topic.isBlank())
                        .map(String::trim)
                        .limit(4)
                        .toList();
        String project = generated.practicalProject() == null || generated.practicalProject().isBlank()
                ? original.practicalProject()
                : generated.practicalProject().trim();
        return new LearningStep(
                original.order(),
                original.skillName(),
                original.currentProficiency(),
                original.targetProficiency(),
                original.gap(),
                original.focus(),
                topics.isEmpty() ? original.topics() : topics,
                project,
                original.resourceName(),
                original.resourceUrl(),
                original.estimatedWeeks()
        );
    }

    private String stripCodeFence(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        return firstLineEnd >= 0 && lastFence > firstLineEnd
                ? trimmed.substring(firstLineEnd + 1, lastFence).trim()
                : trimmed;
    }

    private record LearningResource(String name, String url) {}
    private record GeneratedRoadmap(List<GeneratedStep> steps) {}
    private record GeneratedStep(String skillName, List<String> topics, String practicalProject) {}
    public record RoadmapResult(List<LearningStep> steps, boolean modelGenerated) {}
}
