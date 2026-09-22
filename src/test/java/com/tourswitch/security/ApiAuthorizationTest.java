package com.tourswitch.security;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tourswitch.domain.course.controller.CourseController;
import com.tourswitch.domain.course.controller.ExtraVoteController;
import com.tourswitch.domain.course.service.CourseConfirmationService;
import com.tourswitch.domain.course.service.CourseQueryService;
import com.tourswitch.domain.course.service.ExtraVoteService;
import com.tourswitch.domain.history.controller.TravelHistoryController;
import com.tourswitch.domain.history.service.TravelHistoryService;
import com.tourswitch.domain.member.repository.MemberRepository;
import com.tourswitch.domain.place.controller.PlaceController;
import com.tourswitch.domain.place.service.PlaceFavoriteService;
import com.tourswitch.domain.place.service.PlaceSearchService;
import com.tourswitch.domain.room.controller.InviteController;
import com.tourswitch.domain.room.controller.TravelRoomController;
import com.tourswitch.domain.room.service.ActiveRoomService;
import com.tourswitch.domain.room.service.InviteService;
import com.tourswitch.domain.room.service.TravelRoomService;
import com.tourswitch.domain.vote.controller.VoteController;
import com.tourswitch.domain.vote.service.VoteService;
import com.tourswitch.global.config.security.FrontendProperties;
import com.tourswitch.global.security.SecurityConfig;
import com.tourswitch.global.security.handler.CustomAccessDeniedHandler;
import com.tourswitch.global.security.handler.CustomAuthenticationEntryPoint;
import com.tourswitch.global.security.handler.SecurityErrorResponseWriter;
import com.tourswitch.global.security.jwt.JwtAuthenticationFilter;
import com.tourswitch.global.security.jwt.JwtProvider;
import com.tourswitch.global.security.oauth2.CustomOAuth2UserService;
import com.tourswitch.global.security.oauth2.OAuth2FailureHandler;
import com.tourswitch.global.security.oauth2.OAuth2SuccessHandler;
import com.tourswitch.global.security.principal.UserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * 인증이 필요한 경로와 공개 경로가 설정대로 갈리는지, 그리고 컨트롤러가 쿼리 파라미터가 아닌
 * 인증 주체에서 회원을 읽는지 확인한다.
 */
@WebMvcTest({ExtraVoteController.class, CourseController.class, VoteController.class,
        TravelHistoryController.class, TravelRoomController.class, InviteController.class,
        PlaceController.class})
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class})
@EnableWebSecurity
class ApiAuthorizationTest {

    private static final long MEMBER_ID = 76L;

    @Autowired MockMvc mockMvc;

    @MockitoBean ExtraVoteService extraVoteService;
    @MockitoBean CourseQueryService courseQueryService;
    @MockitoBean CourseConfirmationService courseConfirmationService;
    @MockitoBean VoteService voteService;
    @MockitoBean TravelHistoryService travelHistoryService;
    @MockitoBean TravelRoomService travelRoomService;
    @MockitoBean ActiveRoomService activeRoomService;
    @MockitoBean InviteService inviteService;
    @MockitoBean PlaceSearchService placeSearchService;
    @MockitoBean PlaceFavoriteService placeFavoriteService;

    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean MemberRepository memberRepository;
    @MockitoBean CustomOAuth2UserService customOAuth2UserService;
    @MockitoBean OAuth2SuccessHandler oauth2SuccessHandler;
    @MockitoBean OAuth2FailureHandler oauth2FailureHandler;

    @TestConfiguration
    static class TestBeans {
        @Bean FrontendProperties frontendProperties() {
            return new FrontendProperties("https://example.test/callback", "https://example.test");
        }

        @Bean ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        // 목 필터는 체인을 잇지 않아 인가 검사 자체가 건너뛰어진다. 실제 필터를 쓴다.
        @Bean JwtAuthenticationFilter jwtAuthenticationFilter(JwtProvider jwtProvider,
                                                             MemberRepository memberRepository,
                                                             SecurityErrorResponseWriter writer) {
            return new JwtAuthenticationFilter(jwtProvider, memberRepository, writer);
        }
    }

    private static Authentication asMember() {
        return new UsernamePasswordAuthenticationToken(new UserPrincipal(MEMBER_ID), null, List.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/rooms/active",
            "/api/rooms/1/course",
            "/api/rooms/1/votes/tally",
            "/api/rooms/1/extra-votes",
            "/api/courses",
            "/api/courses/1",
            "/api/places/125452/favorite",
    })
    void 보호된_경로는_인증_없이_호출하면_401(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/places?page=1&size=1",
            "/api/invites/sample-token",
    })
    void 공개_경로는_인증_없이도_열려_있다(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void 방_생성은_인증_없이_호출하면_401() throws Exception {
        mockMvc.perform(post("/api/rooms").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // 초대는 링크 확인(GET)만 열어 두고 참여(POST)는 막는다.
    @Test
    void 초대_참여는_인증_없이_호출하면_401() throws Exception {
        mockMvc.perform(post("/api/invites/sample-token/participants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 추가투표_조회는_principal의_memberId를_쓴다() throws Exception {
        mockMvc.perform(get("/api/rooms/1/extra-votes").with(authentication(asMember())))
                .andExpect(status().isOk());

        verify(extraVoteService).getCandidates(1L, MEMBER_ID);
    }

    @Test
    void 투표_현황_조회는_principal의_memberId를_쓴다() throws Exception {
        mockMvc.perform(get("/api/rooms/1/votes/tally").with(authentication(asMember())))
                .andExpect(status().isOk());

        verify(voteService).getTally(1L, MEMBER_ID);
    }

    @Test
    void 진행중인_방_조회는_principal의_memberId를_쓴다() throws Exception {
        mockMvc.perform(get("/api/rooms/active").with(authentication(asMember())))
                .andExpect(status().isOk());

        verify(activeRoomService).getActiveRoom(MEMBER_ID);
    }

    @Test
    void 관광지_찜_조회는_principal의_memberId를_쓴다() throws Exception {
        mockMvc.perform(get("/api/places/125452/favorite").with(authentication(asMember())))
                .andExpect(status().isOk());

        verify(placeFavoriteService).getFavorite(MEMBER_ID, "125452");
    }

    @Test
    void 여행_기록_목록은_1_based_페이지를_그대로_넘긴다() throws Exception {
        mockMvc.perform(get("/api/courses").param("page", "1").param("size", "20")
                        .with(authentication(asMember())))
                .andExpect(status().isOk());

        verify(travelHistoryService).getHistories(MEMBER_ID, 1, 20);
    }

    @Test
    void 여행_기록_목록은_0페이지를_거부한다() throws Exception {
        mockMvc.perform(get("/api/courses").param("page", "0").param("size", "20")
                        .with(authentication(asMember())))
                .andExpect(status().isBadRequest());
    }
}
