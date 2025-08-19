package rat.boardservice.posts.service;

import rat.boardservice.posts.dto.user.request.UserUpdateRequest;
import rat.boardservice.posts.dto.user.response.UserDeleteResponse;
import rat.boardservice.posts.dto.user.response.UserInfoResponse;
import rat.boardservice.posts.dto.user.response.UserUpdateResponse;
import rat.boardservice.posts.repository.UserRepo;
import java.util.NoSuchElementException;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(UUID id) {
        return userRepo.findById(id)
                .map(UserInfoResponse::from)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원"));
    }

    @Transactional
    public UserDeleteResponse deleteUser(UUID id) {
        if (!userRepo.existsById(id))
            return new UserDeleteResponse(false);

        userRepo.deleteById(id);
        return new UserDeleteResponse(true);
    }

    @Transactional
    public UserUpdateResponse updateMember(UUID id, UserUpdateRequest request) {
        return userRepo.findById(id)
                .map(member -> {
                    member.update(request);
                    return UserUpdateResponse.of(true, member);
                })
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

}