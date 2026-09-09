package com.tourswitch.domain.auth.service;

import com.tourswitch.domain.member.entity.Member;

public record LoginMemberResult(
    Member member,
    boolean newUser
) {
}