package com.springweb.study.security.service;

import com.springweb.study.security.domain.User;
import com.springweb.study.security.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepo userRepo;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepo.findByEmail(email);

		if(user == null) {
			throw new UsernameNotFoundException("not found user");
		}

		return new UserDetailsImpl(user, user.getEmail(), user.getPassword());
	}
}
