package com.employeesystem.service;

import com.employeesystem.dto.LoginDto;

import com.employeesystem.dto.RegisterDto;

public interface AuthService {
    String login(LoginDto loginDto);
    String register(RegisterDto registerDto);
}
