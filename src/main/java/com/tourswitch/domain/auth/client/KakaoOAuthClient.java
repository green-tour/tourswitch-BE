package com.tourswitch.domain.auth.client;

import com.tourswitch.domain.auth.exception.KakaoOAuthException;
import com.tourswitch.domain.auth.response.KakaoTokenResponseDTO;
import com.tourswitch.domain.auth.response.KakaoUserResponseDTO;
import com.tourswitch.global.config.auth.KakaoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {
    private final RestClient kakaoRestClient;
    private final KakaoProperties kakaoProperties;

    /**
     * 카카오 인가 코드를 카카오 Access Token으로 교환
     */
    public KakaoTokenResponseDTO getToken(
        String oauthCode,
        String redirectUri
    ) {
        try {
            MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

            formData.add(
                "grant_type",
                "authorization_code"
            );
            formData.add(
                "client_id",
                kakaoProperties.clientId()
            );
            formData.add(
                "redirect_uri",
                redirectUri
            );
            formData.add(
                "code",
                oauthCode
            );

            if (kakaoProperties.clientSecret() != null
                && !kakaoProperties.clientSecret().isBlank()) {

                formData.add(
                    "client_secret",
                    kakaoProperties.clientSecret()
                );
            }

            KakaoTokenResponseDTO response =
                kakaoRestClient
                    .post()
                    .uri(kakaoProperties.tokenUri())
                    .contentType(
                        MediaType.APPLICATION_FORM_URLENCODED
                    )
                    .body(formData)
                    .retrieve()
                    .body(KakaoTokenResponseDTO.class);

            if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {

                throw new KakaoOAuthException();
            }

            return response;

        } catch (RestClientException exception) {
            throw new KakaoOAuthException();
        }
    }

    /**
     * 카카오 Access Token으로 사용자 정보 조회
     */
    public KakaoUserResponseDTO getUserInfo(
        String accessToken
    ) {
        try {
            KakaoUserResponseDTO response =
                kakaoRestClient
                    .get()
                    .uri(kakaoProperties.userInfoUri())
                    .headers(headers ->
                        headers.setBearerAuth(accessToken)
                    )
                    .retrieve()
                    .body(KakaoUserResponseDTO.class);

            if (response == null
                || response.id() == null) {

                throw new KakaoOAuthException();
            }

            return response;

        } catch (RestClientException exception) {
            throw new KakaoOAuthException();
        }
    }
}