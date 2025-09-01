//package com.postforge.auth.security.service;
//
//import com.postforge.auth.domain.dto.AccountContext;
//import com.postforge.auth.domain.dto.AccountDto;
//import com.postforge.auth.domain.entity.Account;
//import com.postforge.auth.users.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.modelmapper.ModelMapper;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.core.userdetails.UsernameNotFoundException;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service("userDetailsService")
//@RequiredArgsConstructor
//public class FormUserDetailsService implements UserDetailsService {
//
//		private final UserRepository userRepository;
//
//		@Override
//		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//
//			Account account = userRepository.findByUsername(username);
//
//				if(account == null) {
//					throw new UsernameNotFoundException("No user found with username: " + username);
//				}
//
//				List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(account.getRoles()));
//				ModelMapper mapper = new ModelMapper();
//				AccountDto accountDto = mapper.map(account, AccountDto.class);
//
//				return new AccountContext(accountDto, authorities);
//		}
//}
