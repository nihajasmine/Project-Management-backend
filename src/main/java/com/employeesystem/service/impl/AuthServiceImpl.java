package com.employeesystem.service.impl;

import com.employeesystem.dto.LoginDto;
import com.employeesystem.dto.RegisterDto;
import com.employeesystem.entity.Employee;
import com.employeesystem.entity.Role;
import com.employeesystem.entity.User;
import com.employeesystem.exception.APIException;
import com.employeesystem.repository.EmployeeRepository;
import com.employeesystem.repository.UserRepository;
import com.employeesystem.security.JwtTokenProvider;
import com.employeesystem.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager, 
                           JwtTokenProvider jwtTokenProvider, 
                           UserRepository userRepository, 
                           EmployeeRepository employeeRepository, 
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String login(LoginDto loginDto) {

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsernameOrEmail(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenProvider.generateToken(authentication);

        return token;
    }

    @Override
    @Transactional
    public String register(RegisterDto registerDto) {

        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Username already exists.");
        }

        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new APIException(HttpStatus.BAD_REQUEST, "Email already exists.");
        }

        if (registerDto.getJoiningDate() != null) {
            LocalDate minAllowedDate = LocalDate.now().minusDays(5);
            if (registerDto.getJoiningDate().isBefore(minAllowedDate)) {
                throw new APIException(HttpStatus.BAD_REQUEST, "Joining date cannot be earlier than 5 days ago.");
            }
        }

        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        if ("ROLE_ADMIN".equalsIgnoreCase(registerDto.getRole()) || "ADMIN".equalsIgnoreCase(registerDto.getRole())) {
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
                // Fallback
            }
        }

        Employee employee = new Employee();
        employee.setEmployeeId(newEmployeeId);
        employee.setFirstName(registerDto.getFirstName());
        employee.setLastName(registerDto.getLastName());
        employee.setEmail(registerDto.getEmail());
        employee.setPhone(registerDto.getPhone());
        employee.setDepartment(registerDto.getDepartment());
        employee.setSalary(registerDto.getSalary());
        employee.setJoiningDate(registerDto.getJoiningDate());
        employee.setUser(savedUser);

        employeeRepository.save(employee);

        return "User registered successfully.";
    }
}
