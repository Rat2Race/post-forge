package rat.boardservice.posts.service;

import com.springweb.board.common.RoleType;
import rat.boardservice.posts.dto.user.response.UserInfoResponse;
import rat.boardservice.posts.repository.UserRepo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final UserRepo userRepo;

    @Transactional(readOnly = true)
    public List<UserInfoResponse> getMembers() {
        return userRepo.findAllByType(RoleType.USER.name()).stream()
                .map(UserInfoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserInfoResponse> getAdmins() {
        return userRepo.findAllByType(RoleType.ADMIN.name()).stream()
                .map(UserInfoResponse::from)
                .toList();
    }
}