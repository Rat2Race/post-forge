//package com.postforge.api.auth.service;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.argThat;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//import com.postforge.domain.member.dto.request.CommonRegisterRequest;
//import com.postforge.domain.member.entity.Member;
//import com.postforge.domain.member.entity.Role;
//import com.postforge.domain.member.repository.MemberRepository;
//import java.util.Optional;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//@ExtendWith(MockitoExtension.class)
//class CommonAuthServiceTest {
//
//    @Mock
//    MemberRepository mockUserRepo;
//
//    @Mock
//    PasswordEncoder mockPasswordEncoder;
//
//    @InjectMocks
//    CommonAuthService authService;
//
//    @Test
//    void 올바른_회원정보_저장() {
//        CommonRegisterRequest request = new CommonRegisterRequest("test", "testtestId",
//            "testtestPw");
//
//        when(mockUserRepo.findByUsername("test"))
//            .thenReturn(Optional.empty());
//
//        when(mockPasswordEncoder.encode("testtestPw"))
//            .thenReturn("암호화된Password");
//
//        Member newMember = Member.builder()
//            .id(1L)
//            .username("test")
//            .userId("testtestId")
//            .userPw("testtestPw")
//            .build();
//
//        newMember.addRole(Role.USER);
//
//        when(mockUserRepo.save(any()))
//            .thenReturn(newMember);
//
//        Long userId = authService.signup(request);
//
//        assertThat(userId).isEqualTo(1L);
//
//        verify(mockUserRepo).findByName("test");
//        verify(mockPasswordEncoder).encode("testtestPw");
//        verify(mockUserRepo).save(argThat(member ->
//            member.getName().equals("test") &&
//                member.getUserPw().equals("암호화된Password")
//        ));
//    }
//
//    @Test
//    void 잘못된_회원정보_예외처리() {
//        CommonRegisterRequest request = new CommonRegisterRequest("test", "testtestId",
//            "testtestPw");
//
//        Member newMember = Member.builder()
//            .username("test")
//            .userId("testtestId")
//            .userPw("testtestPw")
//            .build();
//
//        newMember.addRole(Role.USER);
//
//        when(mockUserRepo.findByUsername("test"))
//            .thenReturn(Optional.of(newMember));
//
//        assertThrows(IllegalArgumentException.class, () -> authService.signup(request));
//
//        verify(mockUserRepo, never()).save(any());
//        verify(mockPasswordEncoder, never()).encode(any());
//    }
//
//}