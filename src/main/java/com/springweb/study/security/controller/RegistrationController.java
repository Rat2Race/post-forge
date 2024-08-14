package com.springweb.study.security.controller;

import com.springweb.study.security.service.UserDetailsServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistrationController {

	private final UserDetailsServiceImpl userService;

	public RegistrationController(UserDetailsServiceImpl userService) {
		this.userService = userService;
	}

	@GetMapping("/register")
	public String showRegistrationForm() {
		return "register";
	}

	@PostMapping("/register")
	public String registerUser(@RequestParam("username") String username,
	                           @RequestParam("password") String password,
	                           Model model) {
		try {
			userService.registerUser(username, password);
			return "redirect:/login?registerSuccess";
		} catch (IllegalArgumentException e) {
			model.addAttribute("error", e.getMessage());
			return "register";
		}
	}
}

