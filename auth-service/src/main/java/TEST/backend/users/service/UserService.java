package TEST.backend.users.service;

import TEST.backend.domain.entity.Account;
import TEST.backend.domain.entity.SocialMember;
import TEST.backend.domain.entity.SocialMemberKey;
import TEST.backend.users.repository.SocialMemberRepository;
import TEST.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

	private final SocialMemberRepository socialMemberRepository;
	private final UserRepository userRepository;

	@Transactional
	public void createUser(Account account) {
		userRepository.save(account);
	}

	@Transactional
	public Account findMemberBySocialIdentifier(String provider, String identifier) {
		return socialMemberRepository
				.findById(new SocialMemberKey(provider, identifier))
				.flatMap(socialMember -> userRepository.findById(socialMember.getUserId()))
				.orElse(null);
	}

	@Transactional
	public Account createMemberWithSocialIdentifier(String username, String provider, String identifier) {
		Account account = new Account(0L, username, "", "", 0);
		userRepository.save(account);
		socialMemberRepository.save(new SocialMember(provider, identifier, account.getId()));
		return account;
	}
}
