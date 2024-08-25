package com.springweb.study.controller;


import com.springweb.study.security.config.UserAuthorize;
import com.springweb.study.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@UserAuthorize
public class UserController {

    private final UserService userService;
}
