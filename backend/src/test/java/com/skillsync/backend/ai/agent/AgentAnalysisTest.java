package com.skillsync.backend.ai.agent;

import com.skillsync.backend.ai.dto.AssistantDtos.SkillGap;
import com.skillsync.backend.ai.service.OpenAiCompatibleClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AgentAnalysisTest {

    private final LearningAgent learningAgent =
            new LearningAgent(mock(OpenAiCompatibleClient.class), new ObjectMapper());

    @Test
    void readinessUsesRecordedProficiencyAgainstRoleRequirements() {
        List<SkillGap> gaps = List.of(
                new SkillGap(1, "Java", 8, 10, 2, "HIGH"),
                new SkillGap(2, "Docker", 5, 10, 5, "MEDIUM")
        );

        assertEquals(65, CareerAgent.readiness(gaps));
    }

    @Test
    void roadmapPrioritizesLargestGapsAndOnlyIncludesUnmetSkills() {
        List<SkillGap> gaps = List.of(
                new SkillGap(1, "Java", 10, 10, 0, "LOW"),
                new SkillGap(2, "Docker", 2, 8, 6, "HIGH"),
                new SkillGap(3, "Spring Security", 3, 8, 5, "HIGH"),
                new SkillGap(4, "SQL", 4, 8, 4, "MEDIUM"),
                new SkillGap(5, "AWS", 6, 8, 2, "LOW")
        );

        var roadmap = learningAgent.roadmap(gaps, "Backend Developer").steps();

        assertEquals(List.of("Docker", "Spring Security", "SQL"),
                roadmap.stream().map(step -> step.skillName()).toList());
        assertEquals(List.of(1, 2, 3), roadmap.stream().map(step -> step.order()).toList());
        assertEquals("Docker Get Started", roadmap.getFirst().resourceName());
    }

    @Test
    void readinessIsZeroWhenThereAreNoConfiguredRequirements() {
        assertEquals(0, CareerAgent.readiness(List.of()));
    }
}
