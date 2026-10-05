package com.skillsync.backend.Controller;

import com.skillsync.backend.entity.User;
import com.skillsync.backend.service.SkillSyncDomainService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SkillSyncDomainController {

    private final SkillSyncDomainService service;

    public SkillSyncDomainController(SkillSyncDomainService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        return service.dashboard(currentUser(authentication));
    }

    @GetMapping("/skills")
    public List<Map<String, Object>> skills() {
        return service.skills();
    }

    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> createSkill(@Valid @RequestBody SkillSyncDomainService.SkillInput input) {
        return service.createSkill(input);
    }

    @PutMapping("/skills/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateSkill(
            @PathVariable long id,
            @Valid @RequestBody SkillSyncDomainService.SkillInput input
    ) {
        return service.updateSkill(id, input);
    }

    @DeleteMapping("/skills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSkill(@PathVariable long id) {
        service.deleteSkill(id);
    }

    @GetMapping("/employee-skills")
    public List<Map<String, Object>> employeeSkills(
            Authentication authentication,
            @RequestParam(required = false) Long employeeId
    ) {
        return service.employeeSkills(currentUser(authentication), employeeId);
    }

    @PostMapping("/employee-skills")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> saveEmployeeSkill(
            Authentication authentication,
            @RequestParam(required = false) Long employeeId,
            @Valid @RequestBody SkillSyncDomainService.EmployeeSkillInput input
    ) {
        return service.saveEmployeeSkill(currentUser(authentication), employeeId, input);
    }

    @DeleteMapping("/employee-skills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEmployeeSkill(@PathVariable long id, Authentication authentication) {
        service.deleteEmployeeSkill(currentUser(authentication), id);
    }

    @GetMapping("/job-roles")
    public List<Map<String, Object>> jobRoles() {
        return service.jobRoles();
    }

    @PostMapping("/job-roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> createJobRole(@Valid @RequestBody SkillSyncDomainService.JobRoleInput input) {
        return service.createJobRole(input);
    }

    @PutMapping("/job-roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateJobRole(
            @PathVariable long id,
            @Valid @RequestBody SkillSyncDomainService.JobRoleInput input
    ) {
        return service.updateJobRole(id, input);
    }

    @DeleteMapping("/job-roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteJobRole(@PathVariable long id) {
        service.deleteJobRole(id);
    }

    @GetMapping("/job-roles/{jobRoleId}/required-skills")
    public List<Map<String, Object>> requiredSkills(@PathVariable long jobRoleId) {
        return service.requiredSkills(jobRoleId);
    }

    @PutMapping("/job-roles/{jobRoleId}/required-skills")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> saveRequiredSkill(
            @PathVariable long jobRoleId,
            @Valid @RequestBody SkillSyncDomainService.RequiredSkillInput input
    ) {
        return service.saveRequiredSkill(jobRoleId, input);
    }

    @DeleteMapping("/job-roles/{jobRoleId}/required-skills/{mappingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRequiredSkill(@PathVariable long jobRoleId, @PathVariable long mappingId) {
        service.deleteRequiredSkill(jobRoleId, mappingId);
    }

    @GetMapping("/assessments")
    public List<Map<String, Object>> assessments(
            Authentication authentication,
            @RequestParam(required = false) Long employeeId
    ) {
        return service.assessments(currentUser(authentication), employeeId);
    }

    @PostMapping("/assessments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'SME', 'MANAGER', 'ADMIN')")
    public Map<String, Object> submitAssessment(
            Authentication authentication,
            @Valid @RequestBody SkillSyncDomainService.AssessmentInput input
    ) {
        return service.submitAssessment(currentUser(authentication), input);
    }

    @GetMapping("/analytics/skill-gaps")
    public List<Map<String, Object>> skillGaps(
            Authentication authentication,
            @RequestParam(required = false) Long jobRoleId,
            @RequestParam(required = false) Long employeeId
    ) {
        return service.skillGaps(currentUser(authentication), jobRoleId, employeeId);
    }

    @GetMapping("/learning/recommendations")
    public List<Map<String, Object>> recommendations(
            Authentication authentication,
            @RequestParam(required = false) Long jobRoleId
    ) {
        return service.recommendations(currentUser(authentication), jobRoleId);
    }

    @GetMapping("/training-requests")
    public List<Map<String, Object>> trainingRequests(
            Authentication authentication,
            @RequestParam(required = false) Long employeeId
    ) {
        return service.trainingRequests(currentUser(authentication), employeeId);
    }

    @PostMapping("/training-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTrainingRequest(
            Authentication authentication,
            @Valid @RequestBody SkillSyncDomainService.TrainingRequestInput input
    ) {
        return service.createTrainingRequest(currentUser(authentication), input);
    }

    @PatchMapping("/training-requests/{id}")
    public Map<String, Object> updateTrainingRequest(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody SkillSyncDomainService.TrainingStatusInput input
    ) {
        return service.updateTrainingRequest(currentUser(authentication), id, input);
    }

    @GetMapping("/training-sessions")
    public List<Map<String, Object>> trainingSessions(Authentication authentication) {
        return service.trainingSessions(currentUser(authentication));
    }

    @PostMapping("/training-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SME', 'ADMIN')")
    public Map<String, Object> createTrainingSession(
            Authentication authentication,
            @Valid @RequestBody SkillSyncDomainService.TrainingSessionInput input
    ) {
        return service.createTrainingSession(currentUser(authentication), input);
    }

    @PatchMapping("/training-sessions/{id}/feedback")
    @PreAuthorize("hasAnyRole('SME', 'ADMIN')")
    public Map<String, Object> submitSessionFeedback(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody SkillSyncDomainService.SessionFeedbackInput input
    ) {
        return service.submitSessionFeedback(currentUser(authentication), id, input);
    }

    @PutMapping("/manager-employees/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void assignEmployee(
            @PathVariable long employeeId,
            @RequestBody ManagerAssignmentInput input
    ) {
        service.assignEmployeeToManager(employeeId, input.managerId());
    }

    @GetMapping("/reports/workforce-readiness")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public List<Map<String, Object>> workforceReport(Authentication authentication) {
        return service.workforceReport(currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return user;
    }

    public record ManagerAssignmentInput(long managerId) {}
}
