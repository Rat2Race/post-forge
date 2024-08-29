package com.springweb.study.security.impl;

import com.springweb.study.common.RoleType;
import com.springweb.study.domain.User;
import com.springweb.study.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepo userRepo;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepo.findByAccount(username)
				.orElseThrow(() -> new UsernameNotFoundException("user not found"));

		log.info("userInfo: " + user.toString());

		return UserDetailsImpl.build(user);
	}
}
