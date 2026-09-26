package com.tourswitch.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tourswitch.domain.auth.service.RefreshTokenService;
import com.tourswitch.global.config.security.FrontendProperties;
import com.tourswitch.global.security.cookie.RefreshTokenCookieManager;
import org.junit.jupiter.api.Test;

/**
 * 카카오 콜백이 FE 도메인으로 돌아오므로 로그인 시작도 FE 도메인으로 보내야 한다.
 * 상대 경로로 두면 프록시 뒤의 BE 호스트로 리다이렉트되어 인가 세션이 콜백에 전달되지 않는다.
 */
class AuthControllerLoginTest {

    private AuthController controller(String frontendOrigin) {
        return new AuthController(mock(RefreshTokenService.class), mock(RefreshTokenCookieManager.class),
                new FrontendProperties("https://fe.example/oauth2/callback", frontendOrigin));
    }

    @Test
    void 로그인_시작은_FE_도메인의_인가_엔드포인트로_보낸다() {
        assertThat(controller("https://fe.example").login().getUrl())
                .isEqualTo("https://fe.example/api/auth/oauth2/authorization/kakao");
    }

    @Test
    void origin_끝의_슬래시는_중복되지_않는다() {
        assertThat(controller("https://fe.example/").login().getUrl())
                .isEqualTo("https://fe.example/api/auth/oauth2/authorization/kakao");
    }
}
