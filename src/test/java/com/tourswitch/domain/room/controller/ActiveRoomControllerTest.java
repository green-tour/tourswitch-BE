package com.tourswitch.domain.room.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tourswitch.domain.room.response.ActiveRoomResponse;
import com.tourswitch.domain.room.service.ActiveRoomService;
import com.tourswitch.domain.room.service.TravelRoomService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ActiveRoomControllerTest {
    @Mock TravelRoomService travelRoomService;
    @Mock ActiveRoomService activeRoomService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TravelRoomController(travelRoomService, activeRoomService))
                .build();
    }

    @Test
    void 진행_중인_방을_반환한다() throws Exception {
        when(activeRoomService.getActiveRoom(7L)).thenReturn(new ActiveRoomResponse(1L, "성동구 여행",
                LocalDate.of(2026, 9, 23), 4L, "성동구", List.of(3L, 8L), List.of("도시공원", "자연·산"),
                "VOTING", 2, 1));
        mockMvc.perform(get("/api/rooms/active").param("memberId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.keywordIds[1]").value(8))
                .andExpect(jsonPath("$.data.completedParticipantCount").value(1));
    }

    @Test
    void 진행_중인_방이_없으면_data가_null이다() throws Exception {
        when(activeRoomService.getActiveRoom(7L)).thenReturn(null);
        mockMvc.perform(get("/api/rooms/active").param("memberId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
