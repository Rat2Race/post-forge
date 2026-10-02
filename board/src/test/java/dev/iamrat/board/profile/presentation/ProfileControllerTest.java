package dev.iamrat.board.profile.presentation;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.iamrat.board.profile.application.ProfileService;
import dev.iamrat.core.account.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    private static final UserPrincipal USER = () -> 1L;

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileController profileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
            .setCustomArgumentResolvers(new FixedPrincipalResolver())
            .build();
    }

    @Test
    @DisplayName("닉네임 변경은 본문 없이 204를 반환한다")
    void updateNickname_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/user/profile/nickname")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nickname\":\"새닉네임\"}"))
            .andExpect(status().isNoContent());

        verify(profileService).updateNickname(1L, "새닉네임");
    }

    @Test
    @DisplayName("비밀번호 변경은 본문 없이 204를 반환한다")
    void updatePassword_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/user/profile/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Old1234!\",\"newPassword\":\"New1234!\"}"))
            .andExpect(status().isNoContent());

        verify(profileService).updatePassword(1L, "Old1234!", "New1234!");
    }

    private static class FixedPrincipalResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return USER;
        }
    }
}
