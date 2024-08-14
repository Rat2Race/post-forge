package com.springweb.study.security.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String loginPage(@RequestParam(value = "error", required = false) String error,
	                        @RequestParam(value = "logout", required = false) String logout,
	                        RedirectAttributes redirectAttributes) {
		if (error != null) {
			redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
		}
		if (logout != null) {
			redirectAttributes.addFlashAttribute("message", "You have been logged out successfully.");
		}
		return "login";  // login.html로 이동
	}

	@GetMapping("/rootPage")
	public String rootPage() {
		return "rootPage";  // rootPage.html로 이동
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null) {
			new SecurityContextLogoutHandler().logout(request, response, auth);
		}
		return "redirect:/login?logout";  // 로그아웃 후 로그인 페이지로 이동
	}
}
