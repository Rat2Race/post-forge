package rat.boardservice.posts.controller;

import rat.boardservice.posts.dto.user.request.UserUpdateRequest;
import rat.boardservice.posts.dto.user.response.UserDeleteResponse;
import rat.boardservice.posts.dto.user.response.UserInfoResponse;
import rat.boardservice.posts.dto.user.response.UserUpdateResponse;
import rat.boardservice.posts.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

	@GetMapping
	public ResponseEntity<UserInfoResponse> getUserInfo(@RequestBody UUID id) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.getUserInfo(id));
	}

	@DeleteMapping
	public ResponseEntity<UserDeleteResponse> deleteUser(@RequestBody UUID id) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.deleteUser(id));
	}

	@PutMapping
	public ResponseEntity<UserUpdateResponse> updateUser(@RequestBody UUID id, @RequestBody UserUpdateRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(userService.updateMember(id, request));
	}

}