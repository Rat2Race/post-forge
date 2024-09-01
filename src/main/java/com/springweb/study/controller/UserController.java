package com.springweb.study.controller;

import com.springweb.study.domain.User;
import com.springweb.study.dto.user.request.UserUpdateRequest;
import com.springweb.study.dto.user.response.UserDeleteResponse;
import com.springweb.study.dto.user.response.UserInfoResponse;
import com.springweb.study.dto.user.response.UserUpdateResponse;
import com.springweb.study.security.config.UserAuthorize;
import com.springweb.study.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@UserAuthorize
public class UserController {

    private final UserService userService;

	@GetMapping
	public ResponseEntity<UserInfoResponse> getUserInfo(@RequestBody User user) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getUserInfo(UUID.fromString(user.getUsername())));
	}

	@DeleteMapping
	public ResponseEntity<UserDeleteResponse> deleteUser(@RequestBody User user) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.deleteUser(UUID.fromString(user.getUsername())));
	}

	@PutMapping
	public ResponseEntity<UserUpdateResponse> updateUser(@RequestBody User user, @RequestBody UserUpdateRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.updateMember(UUID.fromString(user.getUsername()), request));
	}

}
