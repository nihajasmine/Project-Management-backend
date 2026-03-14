package com.employeesystem.service.impl;

import com.employeesystem.dto.EmployeeDto;
import com.employeesystem.entity.Employee;
import com.employeesystem.entity.Role;
import com.employeesystem.entity.User;
import com.employeesystem.exception.APIException;
import com.employeesystem.exception.ResourceNotFoundException;
import com.employeesystem.repository.EmployeeRepository;
import com.employeesystem.repository.UserRepository;
import com.employeesystem.service.EmployeeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, 
                               UserRepository userRepository, 
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public EmployeeDto createEmployee(EmployeeDto employeeDto) {
        // Check if username or email exists
        if (userRepository.existsByUsername(employeeDto.getUsername())) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.existsByEmail(employeeDto.getEmail())) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        if (employeeDto.getJoiningDate() != null) {
            LocalDate minAllowedDate = LocalDate.now().minusDays(5);
            if (employeeDto.getJoiningDate().isBefore(minAllowedDate)) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Joining date cannot be earlier than 5 days ago.");
            }
        }

        User user = new User();
        user.setUsername(employeeDto.getUsername());
        user.setEmail(employeeDto.getEmail());
        user.setPassword(passwordEncoder.encode(employeeDto.getPassword()));

        // Default role is EMPLOYEE, unless specified as ADMIN
        if (employeeDto.getRole() != null && (employeeDto.getRole().equals("ROLE_ADMIN") || employeeDto.getRole().equals("ADMIN"))) {
            user.setRole(Role.ADMIN);
        } else {
            user.setRole(Role.EMPLOYEE);
        }

        User savedUser = userRepository.save(user);

        // Generate Employee ID
        String newEmployeeId = "EMP001";
        Employee lastEmployee = employeeRepository.findTopByOrderByIdDesc();
        if (lastEmployee != null && lastEmployee.getEmployeeId() != null && lastEmployee.getEmployeeId().startsWith("EMP")) {
            try {
                int lastNumber = Integer.parseInt(lastEmployee.getEmployeeId().substring(3));
                newEmployeeId = String.format("EMP%03d", lastNumber + 1);
            } catch (NumberFormatException e) {
                // If parsing fails for some reason, fallback to a safe generated value or keep it at EMP001 (though rare)
            }
        }

        Employee employee = new Employee();
        employee.setEmployeeId(newEmployeeId);
        employee.setFirstName(employeeDto.getFirstName());
        employee.setLastName(employeeDto.getLastName());
        employee.setEmail(employeeDto.getEmail());
        employee.setPhone(employeeDto.getPhone());
        employee.setDepartment(employeeDto.getDepartment());
        employee.setSalary(employeeDto.getSalary());
        employee.setJoiningDate(employeeDto.getJoiningDate());
        employee.setUser(savedUser);

        Employee savedEmployee = employeeRepository.save(employee);
        
        return mapToDto(savedEmployee);
    }

    @Override
    public EmployeeDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        return mapToDto(employee);
    }

    @Override
    public Page<EmployeeDto> getAllEmployees(Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAll(pageable);
        return employees.map(this::mapToDto);
    }

    @Override
    @Transactional
    public EmployeeDto updateEmployee(Long id, EmployeeDto employeeDto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));

        employee.setFirstName(employeeDto.getFirstName());
        employee.setLastName(employeeDto.getLastName());
        // Do not update email freely if it affects User table without careful handling. 
        // For simplicity, updating Employee specific fields:
        employee.setPhone(employeeDto.getPhone());
        employee.setDepartment(employeeDto.getDepartment());
        
        // Only admin should update salary, this will be handled at controller level authorization
        if(employeeDto.getSalary() != null) {
            employee.setSalary(employeeDto.getSalary());
        }

        Employee updatedEmployee = employeeRepository.save(employee);
        return mapToDto(updatedEmployee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
                
        // Deleting the employee will also delete the user due to Cascade logic (if implemented)
        // Since we have OneToOne mappedBy in User but not cascade on Employee side, 
        // we should delete the user which will cascade to employee
        if (employee.getUser() != null) {
            userRepository.delete(employee.getUser());
        } else {
            employeeRepository.delete(employee);
        }
    }

    @Override
    public Page<EmployeeDto> searchEmployees(String query, Pageable pageable) {
        Page<Employee> employees = employeeRepository.searchByName(query, pageable);
        return employees.map(this::mapToDto);
    }
    
    @Override
    public EmployeeDto getEmployeeByUserId(Long userId) {
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user id: " + userId));
        return mapToDto(employee);
    }

    private EmployeeDto mapToDto(Employee employee) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(employee.getId());
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setEmail(employee.getEmail());
        dto.setPhone(employee.getPhone());
        dto.setDepartment(employee.getDepartment());
        dto.setSalary(employee.getSalary());
        dto.setJoiningDate(employee.getJoiningDate());
        if (employee.getUser() != null) {
            dto.setUsername(employee.getUser().getUsername());
            dto.setRole("ROLE_" + employee.getUser().getRole().name());
        }
        return dto;
    }
}
