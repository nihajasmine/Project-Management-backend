package com.employeesystem.controller;

import com.employeesystem.dto.ProjectDto;
import com.employeesystem.entity.ProjectStatus;
import com.employeesystem.service.ProjectService;
import com.employeesystem.entity.User;
import com.employeesystem.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, UserRepository userRepository) {
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    // ADMIN ENDPOINTS

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProjectDto> createProject(@RequestBody ProjectDto projectDto) {
        return new ResponseEntity<>(projectService.createProject(projectDto), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ProjectDto>> getAllProjects(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(projectService.getAllProjects(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDto> updateProject(@PathVariable Long id, @RequestBody ProjectDto projectDto) {
        return ResponseEntity.ok(projectService.updateProject(id, projectDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok("Project deleted successfully");
    }

    // EMPLOYEE / SHARED ENDPOINTS

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @GetMapping("/employee/{userId}")
    public ResponseEntity<List<ProjectDto>> getProjectsByUserId(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getEmployee() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(projectService.getProjectsByEmployeeId(user.getEmployee().getId()));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PutMapping("/{id}/status")
    public ResponseEntity<ProjectDto> updateProjectStatus(
            @PathVariable Long id, 
            @RequestBody UpdateStatusDto statusDto, 
            Authentication authentication) {
        
        System.out.println("Received Status Update Request for Project ID: " + id);
        System.out.println("New Status: " + statusDto.getStatus());
        
        User authUser = (User) authentication.getPrincipal();
        User user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (user.getEmployee() == null) {
            throw new RuntimeException("No employee record associated with this user");
        }
        Long employeeId = user.getEmployee().getId();
        
        // Ensure status is uppercase for Enum.valueOf
        String statusStr = statusDto.getStatus().toUpperCase();
        
        return ResponseEntity.ok(projectService.updateProjectStatus(id, ProjectStatus.valueOf(statusStr), employeeId));
    }

    public static class UpdateStatusDto {
        private String status;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

}
