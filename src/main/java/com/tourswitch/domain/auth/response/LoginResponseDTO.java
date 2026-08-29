package com.tourswitch.domain.auth.response;

import com.tourswitch.domain.auth.service.AuthLoginResult;
import com.tourswitch.domain.member.response.MemberResponseDTO;

public record LoginResponseDTO(
    MemberResponseDTO user,
    boolean newUser,
    String accessToken,
    long expiresIn
) {

    public static LoginResponseDTO from(
        AuthLoginResult result
    ) {
        return new LoginResponseDTO(
            MemberResponseDTO.from(result.member()),
            result.newUser(),
            result.tokenPair().accessToken(),
            result.tokenPair().accessTokenExpiresIn()
        );
    }
}