package com.tourswitch.domain.room.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tourswitch.domain.room.response.InviteInfoResponse;
import com.tourswitch.domain.room.response.JoinRoomResponse;
import com.tourswitch.domain.room.service.InviteService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InviteControllerTest {
    @Mock InviteService inviteService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InviteController(inviteService)).build();
    }

    @Test
    void 초대_정보_조회_API가_여행방을_반환한다() throws Exception {
        when(inviteService.getInvite("token")).thenReturn(new InviteInfoResponse(1L, "성동구 여행",
                LocalDate.of(2026, 9, 20), "VOTING", true, true));

        mockMvc.perform(get("/api/invites/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.requiresAuthentication").value(true));
    }

    @Test
    void 초대_참여_API가_방_ID와_상태를_반환한다() throws Exception {
        when(inviteService.join("token", 7L)).thenReturn(new JoinRoomResponse(1L, "VOTING"));

        mockMvc.perform(post("/api/invites/token/participants").param("memberId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.status").value("VOTING"));
    }
}
