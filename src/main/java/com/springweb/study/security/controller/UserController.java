package com.springweb.study.security.controller;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.domain.dto.AuthRequest;
import com.springweb.study.security.domain.dto.AuthResponse;
import com.springweb.study.security.service.JwtService;
import com.springweb.study.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final UserDetailsServiceImpl userDetailsService;

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
		authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
		);

		final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getUsername());
		final String jwt = jwtService.generateToken(userDetails);

		return ResponseEntity.ok(new AuthResponse(jwt));
	}
}
