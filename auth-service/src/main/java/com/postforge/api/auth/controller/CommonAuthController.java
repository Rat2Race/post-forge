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

    /** testing **/
    @GetMapping("/security")
    public String operate() {
        return "security";
    }

    /** 회원가입 **/
    @PostMapping("/register")
    public CommonRegisterRequest register(@RequestBody CommonRegisterRequest request) {
        return request;
    }

    /** 로그인 **/
    @PostMapping("/login")
    public CommonLoginRequest login(@RequestBody CommonLoginRequest request) {
        return request;
    }

    /** 로그아웃 **/
    @PostMapping("/logout")
    public String logout() {
        return "logout";
    }
}
