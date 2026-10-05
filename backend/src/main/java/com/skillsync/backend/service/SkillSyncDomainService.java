package com.skillsync.backend.service;

import com.skillsync.backend.entity.Role;
import com.skillsync.backend.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Service
public class SkillSyncDomainService {

    private final JdbcTemplate jdbc;

    public SkillSyncDomainService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> skills() {
        return jdbc.queryForList("SELECT * FROM skills ORDER BY name");
    }

    public Map<String, Object> createSkill(SkillInput input) {
        requireText(input.name(), "Skill name is required");
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO skills (name, category, description) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, input.name().trim());
            statement.setString(2, input.category());
            statement.setString(3, input.description());
            return statement;
        }, key);
        return skillById(key.getKey().longValue());
    }

    public Map<String, Object> updateSkill(long id, SkillInput input) {
        requireText(input.name(), "Skill name is required");
        int updated = jdbc.update(
                "UPDATE skills SET name = ?, category = ?, description = ? WHERE id = ?",
                input.name().trim(), input.category(), input.description(), id
        );
        requireUpdated(updated, "Skill not found");
        return skillById(id);
    }

    public void deleteSkill(long id) {
        requireUpdated(jdbc.update("DELETE FROM skills WHERE id = ?", id), "Skill not found");
    }

    public List<Map<String, Object>> employeeSkills(User user, Long requestedEmployeeId) {
        long employeeId = user.getRole() == Role.SME && requestedEmployeeId != null
                ? claimedEmployeeId(user, requestedEmployeeId)
                : visibleEmployeeId(user, requestedEmployeeId);
        return jdbc.queryForList(
                """
                SELECT es.id, es.employee_id AS employeeId, es.skill_id AS skillId,
                       es.proficiency, es.years_of_experience AS yearsOfExperience,
                       es.last_validated_at AS lastValidatedAt, s.name AS skillName,
                       s.category, es.updated_at AS updatedAt
                FROM employee_skills es JOIN skills s ON s.id = es.skill_id
                WHERE es.employee_id = ? ORDER BY s.name
                """,
                employeeId
        );
    }

    @Transactional
    public Map<String, Object> saveEmployeeSkill(
            User user,
            Long requestedEmployeeId,
            EmployeeSkillInput input
    ) {
        validateScore(input.proficiency());
        long employeeId = visibleEmployeeId(user, requestedEmployeeId);
        requireExists("SELECT COUNT(*) FROM skills WHERE id = ?", input.skillId(), "Skill not found");
        jdbc.update(
                """
                INSERT INTO employee_skills (employee_id, skill_id, proficiency, years_of_experience)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE proficiency = VALUES(proficiency),
                    years_of_experience = VALUES(years_of_experience),
                    updated_at = CURRENT_TIMESTAMP
                """,
                employeeId, input.skillId(), input.proficiency(),
                input.yearsOfExperience() == null ? 0 : input.yearsOfExperience()
        );
        return jdbc.queryForMap(
                """
                SELECT es.id, es.employee_id AS employeeId, es.skill_id AS skillId,
                       es.proficiency, es.years_of_experience AS yearsOfExperience,
                       s.name AS skillName, s.category
                FROM employee_skills es JOIN skills s ON s.id = es.skill_id
                WHERE es.employee_id = ? AND es.skill_id = ?
                """,
                employeeId, input.skillId()
        );
    }

    public void deleteEmployeeSkill(User user, long id) {
        Map<String, Object> mapping = queryOne(
                "SELECT employee_id FROM employee_skills WHERE id = ?", id, "Skill mapping not found"
        );
        long employeeId = ((Number) mapping.get("employee_id")).longValue();
        if (!canViewEmployee(user, employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change this employee's skills");
        }
        jdbc.update("DELETE FROM employee_skills WHERE id = ?", id);
    }

    public List<Map<String, Object>> jobRoles() {
        return jdbc.queryForList(
                """
                SELECT jr.id, jr.name, jr.description, COUNT(rs.id) AS requiredSkillCount
                FROM job_roles jr LEFT JOIN required_skills rs ON rs.job_role_id = jr.id
                GROUP BY jr.id, jr.name, jr.description ORDER BY jr.name
                """
        );
    }

    @Transactional
    public Map<String, Object> createJobRole(JobRoleInput input) {
        requireText(input.name(), "Job role name is required");
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO job_roles (name, description) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, input.name().trim());
            statement.setString(2, input.description());
            return statement;
        }, key);
        return jobRoleById(key.getKey().longValue());
    }

    public Map<String, Object> updateJobRole(long id, JobRoleInput input) {
        requireText(input.name(), "Job role name is required");
        requireUpdated(jdbc.update(
                "UPDATE job_roles SET name = ?, description = ? WHERE id = ?",
                input.name().trim(), input.description(), id
        ), "Job role not found");
        return jobRoleById(id);
    }

    public void deleteJobRole(long id) {
        requireUpdated(jdbc.update("DELETE FROM job_roles WHERE id = ?", id), "Job role not found");
    }

    public List<Map<String, Object>> requiredSkills(long jobRoleId) {
        return jdbc.queryForList(
                """
                SELECT rs.id, rs.job_role_id AS jobRoleId, rs.skill_id AS skillId,
                       rs.required_level AS requiredLevel, rs.importance,
                       s.name AS skillName, s.category
                FROM required_skills rs JOIN skills s ON s.id = rs.skill_id
                WHERE rs.job_role_id = ? ORDER BY rs.importance DESC, s.name
                """,
                jobRoleId
        );
    }

    public List<Map<String, Object>> requiredSkillsForAssistant(User user, long jobRoleId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        requireExists("SELECT COUNT(*) FROM job_roles WHERE id = ?", jobRoleId, "Job role not found");
        return requiredSkills(jobRoleId);
    }

    @Transactional
    public Map<String, Object> saveRequiredSkill(long jobRoleId, RequiredSkillInput input) {
        if (input.requiredLevel() < 1 || input.requiredLevel() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required proficiency must be between 1 and 10");
        }
        requireExists("SELECT COUNT(*) FROM job_roles WHERE id = ?", jobRoleId, "Job role not found");
        requireExists("SELECT COUNT(*) FROM skills WHERE id = ?", input.skillId(), "Skill not found");
        Integer mappingId = jdbc.query(
                "SELECT id FROM required_skills WHERE job_role_id = ? AND skill_id = ?",
                result -> result.next() ? result.getInt(1) : null,
                jobRoleId, input.skillId()
        );
        if (mappingId == null) {
            jdbc.update(
                    "INSERT INTO required_skills (job_role_id, skill_id, required_level, importance) VALUES (?, ?, ?, ?)",
                    jobRoleId, input.skillId(), input.requiredLevel(), importance(input.importance())
            );
        } else {
            jdbc.update(
                    "UPDATE required_skills SET required_level = ?, importance = ? WHERE id = ?",
                    input.requiredLevel(), importance(input.importance()), mappingId
            );
        }
        return jdbc.queryForMap(
                """
                SELECT rs.id, rs.job_role_id AS jobRoleId, rs.skill_id AS skillId,
                       rs.required_level AS requiredLevel, rs.importance,
                       s.name AS skillName, s.category
                FROM required_skills rs JOIN skills s ON s.id = rs.skill_id
                WHERE rs.job_role_id = ? AND rs.skill_id = ?
                """,
                jobRoleId, input.skillId()
        );
    }

    public void deleteRequiredSkill(long jobRoleId, long mappingId) {
        requireUpdated(jdbc.update(
                "DELETE FROM required_skills WHERE id = ? AND job_role_id = ?",
                mappingId, jobRoleId
        ), "Required skill mapping not found");
    }

    public List<Map<String, Object>> assessments(User user, Long requestedEmployeeId) {
        long employeeId = visibleEmployeeId(user, requestedEmployeeId);
        return jdbc.queryForList(
                """
                SELECT a.id, a.employee_id AS employeeId, a.skill_id AS skillId,
                       a.score, a.assessment_type AS assessmentType, a.assessed_by AS assessedBy,
                       a.feedback, a.assessed_at AS assessedAt, s.name AS skillName
                FROM assessments a JOIN skills s ON s.id = a.skill_id
                WHERE a.employee_id = ? ORDER BY a.assessed_at DESC, a.id DESC
                """,
                employeeId
        );
    }

    @Transactional
    public Map<String, Object> submitAssessment(User user, AssessmentInput input) {
        validateScore(input.score());
        requireExists("SELECT COUNT(*) FROM skills WHERE id = ?", input.skillId(), "Skill not found");
        long employeeId = input.employeeId() != null && user.getRole() == Role.SME
                ? claimedEmployeeId(user, input.employeeId())
                : visibleEmployeeId(user, input.employeeId());
        String type = switch (user.getRole()) {
            case SME -> "SME_VALIDATED";
            case MANAGER, ADMIN -> "MANAGER_VALIDATED";
            default -> "SELF";
        };
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO assessments
                    (employee_id, skill_id, score, assessment_type, assessed_by, feedback)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, employeeId);
            statement.setLong(2, input.skillId());
            statement.setInt(3, input.score());
            statement.setString(4, type);
            statement.setLong(5, user.getId());
            statement.setString(6, input.feedback());
            return statement;
        }, key);
        jdbc.update(
                """
                INSERT INTO employee_skills (employee_id, skill_id, proficiency, years_of_experience, last_validated_at)
                VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE proficiency = VALUES(proficiency),
                    last_validated_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                """,
                employeeId, input.skillId(), input.score()
        );
        return jdbc.queryForMap(
                """
                SELECT a.id, a.employee_id AS employeeId, a.skill_id AS skillId,
                       a.score, a.assessment_type AS assessmentType, a.assessed_by AS assessedBy,
                       a.feedback, a.assessed_at AS assessedAt, s.name AS skillName
                FROM assessments a JOIN skills s ON s.id = a.skill_id WHERE a.id = ?
                """,
                key.getKey().longValue()
        );
    }

    public List<Map<String, Object>> skillGaps(User user, Long jobRoleId, Long requestedEmployeeId) {
        long employeeId = visibleEmployeeId(user, requestedEmployeeId);
        if (jobRoleId == null) {
            return jdbc.queryForList(
                    """
                    SELECT es.skill_id AS skillId, s.name AS skillName, s.category,
                           es.proficiency AS currentProficiency, 10 AS requiredProficiency,
                           GREATEST(10 - es.proficiency, 0) AS gap
                    FROM employee_skills es JOIN skills s ON s.id = es.skill_id
                    WHERE es.employee_id = ? AND es.proficiency < 10
                    ORDER BY gap DESC, s.name
                    """,
                    employeeId
            );
        }
        requireExists("SELECT COUNT(*) FROM job_roles WHERE id = ?", jobRoleId, "Job role not found");
        return jdbc.queryForList(
                """
                SELECT rs.skill_id AS skillId, s.name AS skillName, s.category,
                       COALESCE(es.proficiency, 0) AS currentProficiency,
                       rs.required_level AS requiredProficiency,
                       GREATEST(rs.required_level - COALESCE(es.proficiency, 0), 0) AS gap
                FROM required_skills rs JOIN skills s ON s.id = rs.skill_id
                LEFT JOIN employee_skills es ON es.skill_id = rs.skill_id AND es.employee_id = ?
                WHERE rs.job_role_id = ? ORDER BY gap DESC, s.name
                """,
                employeeId, jobRoleId
        );
    }

    public List<Map<String, Object>> recommendations(User user, Long jobRoleId) {
        List<Map<String, Object>> gaps = skillGaps(user, jobRoleId, null);
        return gaps.stream()
                .filter(gap -> ((Number) gap.get("gap")).intValue() > 0)
                .limit(8)
                .map(gap -> Map.<String, Object>of(
                        "skillId", gap.get("skillId"),
                        "skillName", gap.get("skillName"),
                        "currentProficiency", gap.get("currentProficiency"),
                        "requiredProficiency", gap.get("requiredProficiency"),
                        "gap", gap.get("gap"),
                        "title", "Build your " + gap.get("skillName") + " skills",
                        "type", "guided-practice",
                        "estimatedHours", Math.max(1, Math.min(12,
                                ((Number) gap.get("gap")).intValue() / 2)),
                        "requestable", true
                ))
                .toList();
    }

    public List<Map<String, Object>> trainingRequests(User user, Long requestedEmployeeId) {
        String sql = """
                SELECT tr.id, tr.employee_id AS employeeId, tr.skill_id AS skillId,
                       tr.requested_level AS requestedLevel, tr.reason, tr.priority,
                       tr.status, tr.claimed_by AS claimedBy, tr.created_at AS createdAt,
                       s.name AS skillName, u.name AS employeeName
                FROM training_requests tr JOIN skills s ON s.id = tr.skill_id
                JOIN users u ON u.id = tr.employee_id
                """;
        if (user.getRole() == Role.ADMIN) {
            return jdbc.queryForList(sql + " ORDER BY tr.created_at DESC");
        }
        if (user.getRole() == Role.SME) {
            return jdbc.queryForList(
                    sql + " WHERE tr.status = 'OPEN' OR tr.claimed_by = ? ORDER BY tr.created_at DESC",
                    user.getId()
            );
        }
        long employeeId = visibleEmployeeId(user, requestedEmployeeId);
        return jdbc.queryForList(sql + " WHERE tr.employee_id = ? ORDER BY tr.created_at DESC", employeeId);
    }

    @Transactional
    public Map<String, Object> createTrainingRequest(User user, TrainingRequestInput input) {
        if (user.getRole() != Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only employees can create training requests");
        }
        requireExists("SELECT COUNT(*) FROM skills WHERE id = ?", input.skillId(), "Skill not found");
        if (input.requestedLevel() != null) {
            validateScore(input.requestedLevel());
        }
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO training_requests
                    (employee_id, skill_id, requested_level, reason, priority, status)
                    VALUES (?, ?, ?, ?, ?, 'OPEN')
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, user.getId());
            statement.setLong(2, input.skillId());
            if (input.requestedLevel() == null) statement.setNull(3, java.sql.Types.INTEGER);
            else statement.setInt(3, input.requestedLevel());
            statement.setString(4, input.reason());
            statement.setString(5, importance(input.priority()));
            return statement;
        }, key);
        return trainingRequestById(key.getKey().longValue());
    }

    @Transactional
    public Map<String, Object> updateTrainingRequest(User user, long id, TrainingStatusInput input) {
        String status = validTrainingStatus(input.status());
        if (status.equals("CLAIMED") && user.getRole() == Role.SME) {
            requireUpdated(jdbc.update(
                    "UPDATE training_requests SET status = ?, claimed_by = ? WHERE id = ? AND status IN ('OPEN', 'CLAIMED')",
                    status, user.getId(), id
            ), "Request is not available to claim");
        } else if (user.getRole() == Role.ADMIN) {
            requireUpdated(jdbc.update(
                    "UPDATE training_requests SET status = ? WHERE id = ?",
                    status, id
            ), "Training request not found");
        } else if (user.getRole() == Role.MANAGER) {
            Map<String, Object> request = queryOne(
                    "SELECT employee_id FROM training_requests WHERE id = ?", id,
                    "Training request not found"
            );
            long employeeId = ((Number) request.get("employee_id")).longValue();
            if (!canViewEmployee(user, employeeId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update this training request");
            }
            requireUpdated(jdbc.update(
                    "UPDATE training_requests SET status = ? WHERE id = ?",
                    status, id
            ), "Training request not found");
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot update training request status");
        }
        return trainingRequestById(id);
    }

    public List<Map<String, Object>> trainingSessions(User user) {
        String sql = """
                SELECT ts.id, ts.training_request_id AS trainingRequestId, ts.sme_id AS smeId,
                       ts.session_title AS sessionTitle, ts.scheduled_at AS scheduledAt,
                       ts.completed_at AS completedAt, ts.employee_score_before AS employeeScoreBefore,
                       ts.employee_score_after AS employeeScoreAfter, ts.feedback, ts.status,
                       tr.employee_id AS employeeId, u.name AS employeeName, s.name AS skillName
                FROM training_sessions ts JOIN training_requests tr ON tr.id = ts.training_request_id
                JOIN users u ON u.id = tr.employee_id JOIN skills s ON s.id = tr.skill_id
                """;
        if (user.getRole() == Role.ADMIN) {
            return jdbc.queryForList(sql + " ORDER BY ts.scheduled_at DESC");
        }
        if (user.getRole() == Role.SME) {
            return jdbc.queryForList(sql + " WHERE ts.sme_id = ? ORDER BY ts.scheduled_at DESC", user.getId());
        }
        return jdbc.queryForList(sql + " WHERE tr.employee_id = ? ORDER BY ts.scheduled_at DESC", user.getId());
    }

    @Transactional
    public Map<String, Object> createTrainingSession(User user, TrainingSessionInput input) {
        requireExists("SELECT COUNT(*) FROM training_requests WHERE id = ?", input.trainingRequestId(), "Training request not found");
        if (user.getRole() == Role.SME) {
            Integer assigned = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM training_requests WHERE id = ? AND claimed_by = ? AND status = 'CLAIMED'",
                    Integer.class,
                    input.trainingRequestId(), user.getId()
            );
            if (assigned == null || assigned == 0) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Claim this training request before scheduling a session");
            }
        }
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO training_sessions
                    (training_request_id, sme_id, session_title, scheduled_at, status)
                    VALUES (?, ?, ?, ?, 'SCHEDULED')
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, input.trainingRequestId());
            statement.setLong(2, user.getId());
            statement.setString(3, input.sessionTitle().trim());
            statement.setTimestamp(4, input.scheduledAt() == null ? null : java.sql.Timestamp.valueOf(input.scheduledAt()));
            return statement;
        }, key);
        jdbc.update(
                "UPDATE training_requests SET status = 'IN_PROGRESS', claimed_by = ? WHERE id = ?",
                user.getId(), input.trainingRequestId()
        );
        return trainingSessionById(key.getKey().longValue());
    }

    @Transactional
    public Map<String, Object> submitSessionFeedback(User user, long id, SessionFeedbackInput input) {
        validateScoreNullable(input.employeeScoreBefore());
        validateScoreNullable(input.employeeScoreAfter());
        List<Map<String, Object>> sessions = user.getRole() == Role.ADMIN
                ? jdbc.queryForList(
                        """
                        SELECT tr.employee_id AS employeeId, tr.skill_id AS skillId
                        FROM training_sessions ts JOIN training_requests tr ON tr.id=ts.training_request_id
                        WHERE ts.id = ?
                        """,
                        id
                )
                : jdbc.queryForList(
                        """
                        SELECT tr.employee_id AS employeeId, tr.skill_id AS skillId
                        FROM training_sessions ts JOIN training_requests tr ON tr.id=ts.training_request_id
                        WHERE ts.id = ? AND ts.sme_id = ?
                        """,
                        id, user.getId()
                );
        if (sessions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found or not assigned to this SME");
        }
        Map<String, Object> session = sessions.getFirst();
        requireUpdated(jdbc.update(
                """
                UPDATE training_sessions SET feedback = ?, employee_score_before = ?,
                    employee_score_after = ?, completed_at = CURRENT_TIMESTAMP, status = 'COMPLETED'
                WHERE id = ?
                """,
                input.feedback(), input.employeeScoreBefore(), input.employeeScoreAfter(),
                id
        ), "Session not found or not assigned to this SME");
        if (input.employeeScoreAfter() != null) {
            long employeeId = ((Number) session.get("employeeId")).longValue();
            long skillId = ((Number) session.get("skillId")).longValue();
            jdbc.update(
                    """
                    INSERT INTO assessments (employee_id, skill_id, score, assessment_type, assessed_by, feedback)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    employeeId,
                    skillId,
                    input.employeeScoreAfter(),
                    user.getRole() == Role.SME ? "SME_VALIDATED" : "MANAGER_VALIDATED",
                    user.getId(),
                    input.feedback()
            );
            jdbc.update(
                    """
                    INSERT INTO employee_skills (employee_id, skill_id, proficiency, years_of_experience, last_validated_at)
                    VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP)
                    ON DUPLICATE KEY UPDATE proficiency = VALUES(proficiency),
                        last_validated_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                    """,
                    employeeId, skillId, input.employeeScoreAfter()
            );
        }
        jdbc.update(
                """
                UPDATE training_requests tr JOIN training_sessions ts
                  ON ts.training_request_id = tr.id
                SET tr.status = 'COMPLETED'
                WHERE ts.id = ?
                """,
                id
        );
        return trainingSessionById(id);
    }

    public Map<String, Object> dashboard(User user) {
        return switch (user.getRole()) {
            case ADMIN -> adminDashboard(user);
            case MANAGER -> managerDashboard(user);
            case SME -> smeDashboard(user);
            case EMPLOYEE -> employeeDashboard(user);
        };
    }

    public List<Map<String, Object>> workforceReport(User user) {
        requireRole(user, Role.MANAGER, Role.ADMIN);
        if (user.getRole() == Role.MANAGER) {
            return jdbc.queryForList(
                    """
                    SELECT u.id AS employeeId, u.name, u.email,
                           COUNT(es.id) AS skillsTracked,
                           COALESCE(ROUND(AVG(es.proficiency)), 0) AS averageProficiency,
                           COUNT(CASE WHEN es.proficiency < 5 THEN 1 END) AS developmentAreas
                    FROM manager_employees me JOIN users u ON u.id = me.employee_id
                    LEFT JOIN employee_skills es ON es.employee_id = u.id
                    WHERE me.manager_id = ?
                    GROUP BY u.id, u.name, u.email ORDER BY averageProficiency DESC
                    """,
                    user.getId()
            );
        }

        return jdbc.queryForList(
                """
                SELECT u.id AS employeeId, u.name, u.email,
                       COUNT(es.id) AS skillsTracked,
                       COALESCE(ROUND(AVG(es.proficiency)), 0) AS averageProficiency,
                       COUNT(CASE WHEN es.proficiency < 5 THEN 1 END) AS developmentAreas
                FROM users u LEFT JOIN employee_skills es ON es.employee_id = u.id
                WHERE u.role = 'EMPLOYEE'
                GROUP BY u.id, u.name, u.email ORDER BY averageProficiency DESC
                """
        );
    }

    @Transactional
    public void assignEmployeeToManager(long employeeId, long managerId) {
        requireExists("SELECT COUNT(*) FROM users WHERE id = ?", employeeId, "Employee not found");
        requireExists("SELECT COUNT(*) FROM users WHERE id = ? AND role = 'MANAGER'", managerId, "Manager not found");
        jdbc.update("DELETE FROM manager_employees WHERE employee_id = ?", employeeId);
        jdbc.update(
                "INSERT INTO manager_employees (manager_id, employee_id) VALUES (?, ?)",
                managerId, employeeId
        );
    }

    private Map<String, Object> employeeDashboard(User user) {
        return Map.of(
                "role", user.getRole().name(),
                "skills", employeeSkills(user, null),
                "assessments", assessments(user, null),
                "skillGaps", skillGaps(user, null, null),
                "recommendations", recommendations(user, null),
                "trainingRequests", trainingRequests(user, null),
                "trainingSessions", trainingSessions(user)
        );
    }

    private Map<String, Object> smeDashboard(User user) {
        return Map.of(
                "role", user.getRole().name(),
                "openRequests", jdbc.queryForList(
                        """
                        SELECT tr.id, tr.employee_id AS employeeId, tr.skill_id AS skillId,
                               tr.requested_level AS requestedLevel, tr.reason, tr.priority,
                               tr.status, tr.claimed_by AS claimedBy, tr.created_at AS createdAt,
                               s.name AS skillName, u.name AS employeeName
                        FROM training_requests tr JOIN skills s ON s.id=tr.skill_id
                        JOIN users u ON u.id=tr.employee_id
                        WHERE tr.status IN ('OPEN','CLAIMED') ORDER BY tr.created_at DESC
                        """
                ),
                "sessions", trainingSessions(user),
                "recentAssessments", jdbc.queryForList(
                        """
                        SELECT a.*, s.name AS skillName, u.name AS employeeName
                        FROM assessments a JOIN skills s ON s.id=a.skill_id JOIN users u ON u.id=a.employee_id
                        WHERE a.assessed_by = ? ORDER BY a.assessed_at DESC LIMIT 10
                        """,
                        user.getId()
                )
        );
    }

    private Map<String, Object> managerDashboard(User user) {
        return Map.of(
                "role", user.getRole().name(),
                "team", jdbc.queryForList(
                        """
                        SELECT u.id AS employeeId, u.name, u.email,
                               COUNT(es.id) AS skillsTracked,
                               COALESCE(ROUND(AVG(es.proficiency)), 0) AS readiness
                        FROM manager_employees me JOIN users u ON u.id=me.employee_id
                        LEFT JOIN employee_skills es ON es.employee_id=u.id
                        WHERE me.manager_id = ? GROUP BY u.id, u.name, u.email ORDER BY u.name
                        """,
                        user.getId()
                ),
                "workforceReport", workforceReport(user),
                "openTrainingRequests", jdbc.queryForList(
                        """
                        SELECT tr.id, u.name AS employeeName, s.name AS skillName, tr.priority, tr.status
                        FROM training_requests tr
                        JOIN manager_employees me ON me.employee_id=tr.employee_id
                        JOIN users u ON u.id=tr.employee_id JOIN skills s ON s.id=tr.skill_id
                        WHERE me.manager_id = ? AND tr.status IN ('OPEN','CLAIMED','IN_PROGRESS')
                        ORDER BY tr.created_at DESC
                        """,
                        user.getId()
                )
        );
    }

    private Map<String, Object> adminDashboard(User user) {
        return Map.of(
                "role", Role.ADMIN.name(),
                "users", jdbc.queryForList("SELECT id, name, email, role, created_at AS createdAt FROM users ORDER BY created_at DESC"),
                "skills", skills(),
                "jobRoles", jobRoles(),
                "workforceReport", workforceReport(user),
                "managerAssignments", jdbc.queryForList(
                        "SELECT employee_id AS employeeId, manager_id AS managerId FROM manager_employees"
                ),
                "openTrainingRequests", jdbc.queryForList("SELECT COUNT(*) FROM training_requests WHERE status IN ('OPEN','CLAIMED','IN_PROGRESS')").getFirst().get("COUNT(*)"),
                "scheduledSessions", jdbc.queryForList("SELECT COUNT(*) FROM training_sessions WHERE status = 'SCHEDULED'").getFirst().get("COUNT(*)")
        );
    }

    private long visibleEmployeeId(User user, Long requestedId) {
        if (requestedId == null || requestedId == user.getId()) {
            return user.getId();
        }
        if (user.getRole() == Role.ADMIN) return requestedId;
        if (user.getRole() == Role.MANAGER && Boolean.TRUE.equals(jdbc.query(
                "SELECT EXISTS(SELECT 1 FROM manager_employees WHERE manager_id = ? AND employee_id = ?)",
                result -> result.next() && result.getBoolean(1),
                user.getId(), requestedId
        ))) return requestedId;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot access this employee's data");
    }

    private boolean canViewEmployee(User user, long employeeId) {
        if (user.getId() == employeeId || user.getRole() == Role.ADMIN) return true;
        return user.getRole() == Role.MANAGER && Boolean.TRUE.equals(jdbc.query(
                "SELECT EXISTS(SELECT 1 FROM manager_employees WHERE manager_id = ? AND employee_id = ?)",
                result -> result.next() && result.getBoolean(1),
                user.getId(), employeeId
        ));
    }

    private long claimedEmployeeId(User user, long requestedEmployeeId) {
        List<Map<String, Object>> assignments = jdbc.queryForList(
                """
                SELECT employee_id FROM training_requests
                WHERE claimed_by = ? AND employee_id = ? LIMIT 1
                """,
                user.getId(), requestedEmployeeId
        );
        if (assignments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only assess employees with a request assigned to you");
        }
        return requestedEmployeeId;
    }

    private void requireRole(User user, Role... roles) {
        for (Role role : roles) {
            if (user.getRole() == role) return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role permissions");
    }

    private Map<String, Object> skillById(long id) {
        return queryOne("SELECT * FROM skills WHERE id = ?", id, "Skill not found");
    }

    private Map<String, Object> jobRoleById(long id) {
        return queryOne("SELECT * FROM job_roles WHERE id = ?", id, "Job role not found");
    }

    private Map<String, Object> trainingRequestById(long id) {
        return queryOne(
                """
                SELECT tr.id, tr.employee_id AS employeeId, tr.skill_id AS skillId,
                       tr.requested_level AS requestedLevel, tr.reason, tr.priority,
                       tr.status, tr.claimed_by AS claimedBy, tr.created_at AS createdAt,
                       s.name AS skillName
                FROM training_requests tr JOIN skills s ON s.id=tr.skill_id WHERE tr.id=?
                """,
                id, "Training request not found"
        );
    }

    private Map<String, Object> trainingSessionById(long id) {
        return queryOne(
                """
                SELECT ts.id, ts.training_request_id AS trainingRequestId, ts.sme_id AS smeId,
                       ts.session_title AS sessionTitle, ts.scheduled_at AS scheduledAt,
                       ts.completed_at AS completedAt, ts.employee_score_before AS employeeScoreBefore,
                       ts.employee_score_after AS employeeScoreAfter, ts.feedback, ts.status
                FROM training_sessions ts WHERE ts.id=?
                """,
                id, "Training session not found"
        );
    }

    private Map<String, Object> queryOne(String sql, long id, String message) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        return rows.getFirst();
    }

    private void requireExists(String sql, long id, String message) {
        Integer count = jdbc.queryForObject(sql, Integer.class, id);
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        }
    }

    private void requireUpdated(int updated, String message) {
        if (updated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private void validateScore(int score) {
        if (score < 0 || score > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Proficiency must be between 0 and 10");
        }
    }

    private void validateScoreNullable(Integer score) {
        if (score != null) validateScore(score);
    }

    private String importance(String value) {
        String normalized = value == null ? "MEDIUM" : value.toUpperCase();
        if (!List.of("LOW", "MEDIUM", "HIGH").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value must be LOW, MEDIUM, or HIGH");
        }
        return normalized;
    }

    private String validTrainingStatus(String value) {
        String normalized = value == null ? "" : value.toUpperCase();
        if (!List.of("OPEN", "CLAIMED", "IN_PROGRESS", "COMPLETED", "CANCELLED").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid training request status");
        }
        return normalized;
    }

    public record SkillInput(String name, String category, String description) {}
    public record EmployeeSkillInput(long skillId, int proficiency, Double yearsOfExperience) {}
    public record JobRoleInput(String name, String description) {}
    public record RequiredSkillInput(long skillId, int requiredLevel, String importance) {}
    public record AssessmentInput(Long employeeId, long skillId, int score, String feedback) {}
    public record TrainingRequestInput(long skillId, Integer requestedLevel, String reason, String priority) {}
    public record TrainingStatusInput(String status) {}
    public record TrainingSessionInput(long trainingRequestId, String sessionTitle, java.time.LocalDateTime scheduledAt) {}
    public record SessionFeedbackInput(String feedback, Integer employeeScoreBefore, Integer employeeScoreAfter) {}
}
