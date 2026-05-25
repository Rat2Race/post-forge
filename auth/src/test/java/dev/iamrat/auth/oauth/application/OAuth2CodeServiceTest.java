package dev.iamrat.auth.oauth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

import dev.iamrat.auth.support.error.AuthErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class OAuth2CodeServiceTest {

    @Mock
    OAuth2CodeStore oAuth2CodeStore;

    @InjectMocks
    OAuth2CodeService oAuth2CodeService;

    @Test
    @DisplayName("accountId로 OAuth2 교환 코드를 생성하고 저장한다")
    void createCode_storesExchangeCode() {
        String code = oAuth2CodeService.createCode(1L);

        assertThat(code).isNotBlank();
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(oAuth2CodeStore).save(codeCaptor.capture(), org.mockito.ArgumentMatchers.eq(1L));
        assertThat(codeCaptor.getValue()).isEqualTo(code);
    }

    @Test
    @DisplayName("code 교환은 값을 삭제하면서 accountId를 반환한다")
    void exchangeCode_existingCode_returnsAccountId() {
        given(oAuth2CodeStore.getAndDelete("exchange-code")).willReturn("1");

        assertThat(oAuth2CodeService.exchangeCode("exchange-code")).isEqualTo(1L);
    }

    @Test
    @DisplayName("저장된 code가 없으면 INVALID_TOKEN 예외를 던진다")
    void exchangeCode_missingCode_throwsInvalidToken() {
        given(oAuth2CodeStore.getAndDelete("missing-code")).willReturn(null);

        assertThatThrownBy(() -> oAuth2CodeService.exchangeCode("missing-code"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("저장된 accountId가 숫자가 아니면 INVALID_TOKEN 예외를 던진다")
    void exchangeCode_invalidStoredAccountId_throwsInvalidToken() {
        given(oAuth2CodeStore.getAndDelete("exchange-code")).willReturn("not-number");

        assertThatThrownBy(() -> oAuth2CodeService.exchangeCode("exchange-code"))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    @DisplayName("accountId가 없으면 code를 만들지 않는다")
    void createCode_nullAccountId_throwsInvalidToken() {
        assertThatThrownBy(() -> oAuth2CodeService.createCode(null))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_TOKEN));

        verify(oAuth2CodeStore, org.mockito.Mockito.never()).save(anyString(), org.mockito.ArgumentMatchers.any());
    }
}
