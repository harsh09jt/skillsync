package com.skillsync.backend.ai.agent;

import com.skillsync.backend.ai.dto.AssistantDtos.CareerRecommendation;
import com.skillsync.backend.ai.dto.AssistantDtos.SkillGap;
import com.skillsync.backend.ai.tools.SkillSyncAgentTools;
import com.skillsync.backend.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CareerAgent {

    private final SkillSyncAgentTools tools;
    private final SkillGapAgent skillGapAgent;

    public CareerAgent(SkillSyncAgentTools tools, SkillGapAgent skillGapAgent) {
        this.tools = tools;
        this.skillGapAgent = skillGapAgent;
    }

    public List<CareerRecommendation> recommend(User user, Long preferredRoleId) {
        return tools.jobRoles().stream()
                .filter(role -> number(role.get("requiredSkillCount")) > 0)
                .map(role -> {
                    long roleId = number(role.get("id"));
                    List<SkillGap> gaps = skillGapAgent.analyze(user, roleId);
                    int readiness = readiness(gaps);
                    return new CareerRecommendation(
                            roleId,
                            String.valueOf(role.get("name")),
                            readiness,
                            readiness >= 80,
                            explanation(readiness, gaps),
                            gaps.stream().filter(gap -> gap.gap() > 0).map(SkillGap::skillName).toList()
                    );
                })
                .sorted((left, right) -> {
                    if (preferredRoleId != null && left.jobRoleId() == preferredRoleId) return -1;
                    if (preferredRoleId != null && right.jobRoleId() == preferredRoleId) return 1;
                    return Integer.compare(right.readinessPercentage(), left.readinessPercentage());
                })
                .limit(5)
                .toList();
    }

    public static int readiness(List<SkillGap> gaps) {
        int requiredTotal = gaps.stream().mapToInt(SkillGap::requiredProficiency).sum();
        if (requiredTotal == 0) return 0;
        int achieved = gaps.stream()
                .mapToInt(gap -> Math.min(gap.currentProficiency(), gap.requiredProficiency()))
                .sum();
        return (int) Math.round(achieved * 100.0 / requiredTotal);
    }

    private String explanation(int readiness, List<SkillGap> gaps) {
        List<String> missing = gaps.stream()
                .filter(gap -> gap.gap() > 0)
                .limit(3)
                .map(SkillGap::skillName)
                .toList();
        if (missing.isEmpty()) return "Current recorded proficiency meets all configured requirements.";
        return readiness + "% of configured proficiency targets met. Focus next on "
                + String.join(", ", missing) + ".";
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }
}
