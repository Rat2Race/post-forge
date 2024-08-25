package com.springweb.study.security.controller;


import com.springweb.study.security.config.UserAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@UserAuthorize
public class UserController {

}
