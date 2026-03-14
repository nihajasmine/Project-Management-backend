package com.employeesystem.service;

import com.employeesystem.dto.EmployeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeService {
    EmployeeDto createEmployee(EmployeeDto employeeDto);
    EmployeeDto getEmployeeById(Long id);
    Page<EmployeeDto> getAllEmployees(Pageable pageable);
    EmployeeDto updateEmployee(Long id, EmployeeDto employeeDto);
    void deleteEmployee(Long id);
    Page<EmployeeDto> searchEmployees(String query, Pageable pageable);
    EmployeeDto getEmployeeByUserId(Long userId);
}
