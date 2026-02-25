package dev.iamrat.oauth.service;

import dev.iamrat.member.entity.Member;
import dev.iamrat.member.entity.Role;
import dev.iamrat.member.repository.MemberRepository;
import dev.iamrat.member.service.MemberService;
import dev.iamrat.oauth.dto.OAuth2UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private MemberService memberService;
    
    @Mock
    private OAuth2UserInfo oAuth2UserInfo;
    
    @InjectMocks
    private OAuth2MemberService oAuth2MemberService;
    
    @Test
    @DisplayName("기존 회원이면 생성하지 않고 기존 회원을 반환한다")
    void getOrCreateMember_existingMember_returnsExistingMember() {
        Member created = member("user-uuid-123", "tester");
        given(oAuth2UserInfo.getId()).willReturn("google-user-123");
        given(memberRepository.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.of(created));
        
        Member result = oAuth2MemberService.getOrCreateMember("GOOGLE", oAuth2UserInfo);
        
        assertThat(result).isEqualTo(created);
        verify(memberService, never()).createMember(
            anyString(), anyString(), isNull(), anyString(), anyString(), anyString(), anyString()
        );
    }
    
    @Test
    @DisplayName("신규 회원이면 닉네임을 생성해 회원을 만든다")
    void getOrCreateMember_newMember_createsMember() {
        given(oAuth2UserInfo.getId()).willReturn("google-user-123");
        given(oAuth2UserInfo.getName()).willReturn("홍길동");
        given(oAuth2UserInfo.getEmail()).willReturn("test@gmail.com");
        given(memberRepository.findByProviderAndProviderId("GOOGLE", "google-user-123"))
            .willReturn(Optional.empty());
        given(memberRepository.existsByNickname(anyString())).willReturn(false);
        
        Member created = member("new-user-123", "newTester");
        given(memberService.createMember(anyString(), anyString(), isNull(), anyString(), anyString(), anyString(), anyString()))
            .willReturn(created);
        
        Member result = oAuth2MemberService.getOrCreateMember("GOOGLE", oAuth2UserInfo);
        
        assertThat(result).isEqualTo(created);
        
        ArgumentCaptor<String> nicknameCaptor = ArgumentCaptor.forClass(String.class);
        verify(memberService).createMember(
            eq("홍길동"),
            eq("google-user-123"),
            isNull(),
            eq("test@gmail.com"),
            nicknameCaptor.capture(),
            eq("GOOGLE"),
            eq("google-user-123")
        );
        
        assertThat(nicknameCaptor.getValue())
            .startsWith("user_")
            .hasSizeLessThan(20);
    }
    
    private Member member(String userId, String nickname) {
        Member member = Member.builder()
            .userName("홍길동")
            .userId(userId)
            .nickname(nickname)
            .email(userId + "@test.com")
            .provider("GOOGLE")
            .providerId("google-user-123")
            .build();
        member.addRole(Role.USER);
        return member;
    }
}