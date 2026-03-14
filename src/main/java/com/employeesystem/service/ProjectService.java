package com.employeesystem.service;

import com.employeesystem.dto.ProjectDto;
import com.employeesystem.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectService {
    ProjectDto createProject(ProjectDto projectDto);
    Page<ProjectDto> getAllProjects(Pageable pageable);
    ProjectDto getProjectById(Long id);
    ProjectDto updateProject(Long id, ProjectDto projectDto);
    void deleteProject(Long id);
    
    // Employee-specific actions
    List<ProjectDto> getProjectsByEmployeeId(Long employeeId);
    ProjectDto updateProjectStatus(Long id, ProjectStatus status, Long requestingEmployeeId);
}
