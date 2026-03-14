package com.employeesystem.controller;

import com.employeesystem.dto.JwtAuthResponse;
import com.employeesystem.dto.LoginDto;
import com.employeesystem.dto.RegisterDto;
import com.employeesystem.entity.User;
import com.employeesystem.repository.UserRepository;
import com.employeesystem.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginDto loginDto) {
        String token = authService.login(loginDto);

        User user = userRepository.findByUsername(loginDto.getUsernameOrEmail())
                .orElseGet(() -> userRepository.findByEmail(loginDto.getUsernameOrEmail())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found")));

        JwtAuthResponse jwtAuthResponse = new JwtAuthResponse();
        jwtAuthResponse.setAccessToken(token);
        jwtAuthResponse.setUserId(user.getId());
        jwtAuthResponse.setRole(user.getRole().name());

        return ResponseEntity.ok(jwtAuthResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterDto registerDto) {
        String response = authService.register(registerDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
