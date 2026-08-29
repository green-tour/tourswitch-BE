package com.tourswitch.domain.member.entity;

public enum MemberStatus {
    // 정상 회원
    ACTIVE,

    // 탈퇴한 회원
    WITHDRAWN,

    // 개인정보 익명화까지 완료된 회원
    PURGED
}
