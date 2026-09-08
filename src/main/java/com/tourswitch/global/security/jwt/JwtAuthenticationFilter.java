package com.tourswitch.global.security.jwt;

import com.tourswitch.domain.member.entity.Member;
import com.tourswitch.domain.member.entity.MemberStatus;
import com.tourswitch.domain.member.exception.WithdrawnMemberException;
import com.tourswitch.domain.member.repository.MemberRepository;
import com.tourswitch.global.security.exception.TokenException;
import com.tourswitch.global.security.handler.SecurityErrorResponseWriter;
import com.tourswitch.global.security.principal.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String accessToken =
                extractAccessToken(request);

            if (accessToken == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!jwtProvider.validateAccessToken(accessToken)) {
                throw new TokenException();
            }

            Long memberId =
                jwtProvider.getMemberId(accessToken);

            Member member =
                memberRepository.findById(memberId)
                    .orElseThrow(TokenException::new);

            if (member.getStatus() != MemberStatus.ACTIVE) {
                throw new WithdrawnMemberException();
            }

            UserPrincipal principal =
                new UserPrincipal(memberId);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of()
                );

            SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(authentication);

            SecurityContextHolder.setContext(
                securityContext
            );

            filterChain.doFilter(
                request,
                response
            );

        } catch (
            TokenException | WithdrawnMemberException exception
        ) {
            SecurityContextHolder.clearContext();

            securityErrorResponseWriter.write(
                response,
                exception.getCode(),
                exception.getMessage()
            );
        }
    }

    /**
     * Authorization 헤더에서 Access Token 추출
     */
    private String extractAccessToken(
        HttpServletRequest request
    ) {
        String authorizationHeader =
            request.getHeader(
                HttpHeaders.AUTHORIZATION
            );

        if (authorizationHeader == null) {
            return null;
        }

        if (!authorizationHeader.startsWith(
            BEARER_PREFIX
        )) {
            throw new TokenException();
        }

        String accessToken =
            authorizationHeader
                .substring(BEARER_PREFIX.length())
                .trim();

        if (accessToken.isBlank()) {
            throw new TokenException();
        }

        return accessToken;
    }

    @Override
    protected boolean shouldNotFilter(
        HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        return path.equals("/api/auth/refresh");
    }
}