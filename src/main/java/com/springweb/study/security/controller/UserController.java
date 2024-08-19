package com.springweb.study.security.controller;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.domain.dto.AuthRequest;
import com.springweb.study.security.service.JwtService;
import com.springweb.study.security.service.UserDetailsServiceImpl;
import com.springweb.study.security.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final JwtService jwtService;
	private final UserDetailsServiceImpl userDetailsService;

	@GetMapping("/login")
	public String showLoginForm(HttpSession session) {
		String token = (String) session.getAttribute("token");
		if (token != null) {
			return "redirect:/dashboard";
		}
		return "login";
	}


	@PostMapping("/login")
	public String login(@ModelAttribute AuthRequest authRequest, HttpSession session, Model model) {
		try {
			User user = userService.authenticateUser(authRequest);
			String token = jwtService.generateToken(user.getUsername());
			session.setAttribute("token", token);
			return "redirect:/dashboard";
		} catch (Exception e) {
			model.addAttribute("error", "Invalid username or password.");
			return "login";
		}
	}

	@GetMapping("/register")
	public String showRegisterForm() {
		return "register";
	}

	@PostMapping("/register")
	public String register(@ModelAttribute AuthRequest authRequest, Model model) {
		try {
			userService.registerUser(authRequest);
			model.addAttribute("message", "Registration successful. Please login.");
			return "redirect:/auth/login";
		} catch (Exception e) {
			model.addAttribute("message", "Registration failed. Please try again.");
			return "register";
		}
	}

	@GetMapping("/dashboard")
	public String dashboard(HttpSession session, Model model) {
		String token = (String) session.getAttribute("token");
		if (token != null) {
			String username = jwtService.getUsernameFromToken(token);
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);

			if (jwtService.validateToken(token, userDetails)) {
				model.addAttribute("username", username);
				return "dashboard";
			}
		}
		return "redirect:/auth/login";
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:/auth/login";
	}
}
