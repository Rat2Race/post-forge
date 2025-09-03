package com.postforge.api.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postforge.domain.member.dto.CommonLoginRequest;
import com.postforge.domain.member.dto.CommonRegisterRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CommonAuthController {

    ObjectMapper om = new ObjectMapper();

    @GetMapping("/security")
    public String operate() {
        return "security";
    }

    @PostMapping("/login")
    public CommonLoginRequest login(@RequestBody CommonLoginRequest commonLoginDto) {
        return commonLoginDto;
    }

    @PostMapping("/register")
    public CommonRegisterRequest register(@RequestBody CommonRegisterRequest registerDto) {
        return registerDto;
    }
}
