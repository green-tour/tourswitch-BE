package com.tourswitch.domain.metadata.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tourswitch.domain.metadata.response.KeywordMetadataResponse;
import com.tourswitch.domain.metadata.response.RegionMetadataResponse;
import com.tourswitch.domain.metadata.service.MetadataService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MetadataControllerTest {
    @Mock MetadataService metadataService;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MetadataController(metadataService)).build();
    }

    @Test
    void 지역_목록_API를_반환한다() throws Exception {
        when(metadataService.getRegions()).thenReturn(List.of(new RegionMetadataResponse(1L, "종로구")));
        mockMvc.perform(get("/api/metadata/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("종로구"));
    }

    @Test
    void 활성_키워드_목록_API를_반환한다() throws Exception {
        when(metadataService.getKeywords())
                .thenReturn(List.of(new KeywordMetadataResponse(3L, "도시공원", "CITY_PARK")));
        mockMvc.perform(get("/api/metadata/keywords"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].code").value("CITY_PARK"));
    }
}
