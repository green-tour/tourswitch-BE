package com.tourswitch.domain.auth.service;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.global.security.jwt.TokenPair;

public record AuthLoginResult(
    Member member,
    boolean newUser,
    TokenPair tokenPair
) {
}