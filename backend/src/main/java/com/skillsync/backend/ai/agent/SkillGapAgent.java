package com.skillsync.backend.ai.agent;

import com.skillsync.backend.ai.dto.AssistantDtos.SkillGap;
import com.skillsync.backend.ai.tools.SkillSyncAgentTools;
import com.skillsync.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SkillGapAgent {

    private final SkillSyncAgentTools tools;

    public SkillGapAgent(SkillSyncAgentTools tools) {
        this.tools = tools;
    }

    public List<SkillGap> analyze(User user, Long jobRoleId) {
        if (jobRoleId == null) return List.of();
        Map<Long, Integer> currentLevels = tools.employeeSkills(user).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> number(row.get("skillId")),
                        row -> (int) number(row.get("proficiency")),
                        Math::max
                ));
        return tools.requiredSkills(user, jobRoleId).stream()
                .map(required -> {
                    long skillId = number(required.get("skillId"));
                    int target = (int) number(required.get("requiredLevel"));
                    int current = currentLevels.getOrDefault(skillId, 0);
                    return new SkillGap(
                            skillId,
                            String.valueOf(required.get("skillName")),
                            current,
                            target,
                            Math.max(0, target - current),
                            String.valueOf(required.getOrDefault("importance", "MEDIUM"))
                    );
                })
                .sorted(java.util.Comparator.comparingInt(SkillGap::gap).reversed())
                .toList();
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
