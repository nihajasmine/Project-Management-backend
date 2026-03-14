package com.employeesystem.service.impl;

import com.employeesystem.dto.ProjectDto;
import com.employeesystem.entity.Employee;
import com.employeesystem.entity.Project;
import com.employeesystem.entity.ProjectStatus;
import com.employeesystem.exception.ResourceNotFoundException;
import com.employeesystem.repository.EmployeeRepository;
import com.employeesystem.repository.ProjectRepository;
import com.employeesystem.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    public ProjectServiceImpl(ProjectRepository projectRepository, EmployeeRepository employeeRepository) {
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    public ProjectDto createProject(ProjectDto projectDto) {
        System.out.println("Service: createProject called");
        System.out.println("DTO Name/ProjectName: " + projectDto.getName());
        System.out.println("DTO AssignedEmployeeId/EmployeeId: " + projectDto.getAssignedEmployeeId());
        System.out.println("DTO StartDate: " + projectDto.getStartDate());
        System.out.println("DTO Deadline: " + projectDto.getDeadline());
        System.out.println("DTO Status: " + projectDto.getStatus());

        Project project = mapToEntity(projectDto);
        
        if (projectDto.getAssignedEmployeeId() != null && !projectDto.getAssignedEmployeeId().trim().isEmpty()) {
            System.out.println("Service: Finding employee by ID: " + projectDto.getAssignedEmployeeId());
            Employee employee = employeeRepository.findByEmployeeId(projectDto.getAssignedEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", projectDto.getAssignedEmployeeId()));
            project.setAssignedEmployee(employee);
        } else {
            // If no employee assigned directly, ensure status is handled
            if (project.getStatus() == null) {
                project.setStatus(ProjectStatus.NOT_STARTED);
            }
        }

        Project savedProject = projectRepository.save(project);
        System.out.println("Service: Project saved with database ID: " + savedProject.getId());
        return mapToDto(savedProject);
    }

    @Override
    public Page<ProjectDto> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable).map(this::mapToDto);
    }

    @Override
    public ProjectDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        return mapToDto(project);
    }

    @Override
    public ProjectDto updateProject(Long id, ProjectDto projectDto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        project.setName(projectDto.getName());
        project.setDescription(projectDto.getDescription());
        project.setStartDate(projectDto.getStartDate());
        project.setDeadline(projectDto.getDeadline());
        project.setStatus(projectDto.getStatus());

        if (projectDto.getAssignedEmployeeId() != null && !projectDto.getAssignedEmployeeId().trim().isEmpty()) {
            Employee employee = employeeRepository.findByEmployeeId(projectDto.getAssignedEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee", "employeeId", projectDto.getAssignedEmployeeId()));
            project.setAssignedEmployee(employee);
        } else {
            project.setAssignedEmployee(null);
        }

        Project updatedProject = projectRepository.save(project);
        return mapToDto(updatedProject);
    }

    @Override
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));
        projectRepository.delete(project);
    }

    @Override
    public List<ProjectDto> getProjectsByEmployeeId(Long employeeId) {
        return projectRepository.findByAssignedEmployeeId(employeeId).stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public ProjectDto updateProjectStatus(Long id, ProjectStatus status, Long requestingEmployeeId) {
        System.out.println("Service: Updating project status to " + status + " for project " + id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        System.out.println("Service: Project found. Assigned Employee ID: " + (project.getAssignedEmployee() != null ? project.getAssignedEmployee().getId() : "null"));
        System.out.println("Service: Requesting Employee ID: " + requestingEmployeeId);

        if (project.getAssignedEmployee() == null || !project.getAssignedEmployee().getId().equals(requestingEmployeeId)) {
            throw new RuntimeException("Unauthorized: You can only update your assigned projects.");
        }

        project.setStatus(status);
        Project updatedProject = projectRepository.save(project);
        System.out.println("Service: Project saved successfully");
        return mapToDto(updatedProject);
    }

    private ProjectDto mapToDto(Project project) {
        ProjectDto dto = new ProjectDto();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setDescription(project.getDescription());
        dto.setStartDate(project.getStartDate());
        dto.setDeadline(project.getDeadline());
        dto.setStatus(project.getStatus());
        
        if (project.getAssignedEmployee() != null) {
            dto.setAssignedEmployeeId(project.getAssignedEmployee().getEmployeeId());
            dto.setAssignedEmployeeName(project.getAssignedEmployee().getFirstName() + " " + project.getAssignedEmployee().getLastName());
        }
        return dto;
    }

    private Project mapToEntity(ProjectDto dto) {
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setStartDate(dto.getStartDate());
        project.setDeadline(dto.getDeadline());
        project.setStatus(dto.getStatus() != null ? dto.getStatus() : ProjectStatus.NOT_STARTED);
        return project;
    }
}
