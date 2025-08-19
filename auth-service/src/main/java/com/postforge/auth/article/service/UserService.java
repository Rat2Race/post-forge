//package com.postforge.auth.article.service;
//
//import java.util.Map;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
///**
// * https://do5do.tistory.com/20
// * https://sjh9708.tistory.com/170
// */
//
//@Service
//@Transactional
//@RequiredArgsConstructor
//public class UserService extends DefaultOAuth2UserService {
//
//	private final UserRepository userRepository;
//
//	@Override
//	public OAuth2User loadUser(OAuth2UserRequest userRequest) {
//
//		// 유저 정보
//		Map<String, Object> userAttributes = super.loadUser(userRequest).getAttributes();
//
//		// registrationId
//		String registrationId = userRequest.getClientRegistration().getRegistrationId();
//
//		// userNameAttributeName
//		String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
//				.getUserInfoEndpoint().getUserNameAttributeName();
//
//		// user dto
//		UserInfo userInfo = UserInfo.of(registrationId, userAttributes);
//
//		// register, login
//		User user = getOrSave(userInfo);
//
//		return new PrincipalDetails(user, userAttributes, userNameAttributeName);
//
//	}
//
//	private User getOrSave(UserInfo userInfo) {
//		User user = userRepository.findByEmail(userInfo.email())
//				.orElseGet(userInfo.toEntity());
//
//		return userRepository.save(user);
//	}
//
//	public boolean checkLoginIdDuplicate(String loginId) {
//		return userRepository.existsByLoginId(loginId);
//	}
//
//	public boolean checkNicknameDuplicate(String nickname) {
//		return userRepository.existsByNickname(nickname);
//	}
//
//	public void join(JoinRequest req) {
//		userRepository.save(req.toEntity());
//	}
//
//	public User login(LoginRequest req) {
//		Optional<User> optionalUser = userRepository.findByLoginId(req.getLoginId());
//
//		if (optionalUser.isEmpty()) {
//			return null;
//		}
//
//		User user = optionalUser.get();
//
//		if (!user.getPassword().equals(req.getPassword())) {
//			return null;
//		}
//
//		return user;
//	}
//
//	public User getLoginUser(Long userId) {
//		if (userId == null) return null;
//
//		Optional<User> optionalUser = userRepository.findById(userId);
//		if (optionalUser.isEmpty()) return null;
//
//		return optionalUser.get();
//	}
//
//	public User getLoginUserByLoginId(String loginId) {
//		if (loginId == null) return null;
//
//		Optional<User> optionalUser = userRepository.findByLoginId(loginId);
//		if (optionalUser.isEmpty()) return null;
//
//		return optionalUser.get();
//	}
//}
