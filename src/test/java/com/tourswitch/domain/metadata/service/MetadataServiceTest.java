package com.tourswitch.domain.metadata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.metadata.repository.KeywordMetadataRow;
import com.tourswitch.domain.metadata.repository.MetadataQueryRepository;
import com.tourswitch.domain.metadata.repository.RegionMetadataRow;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {
    @Mock MetadataQueryRepository metadataQueryRepository;

    @Test
    void 지역과_활성_키워드를_프론트_코드로_변환한다() {
        when(metadataQueryRepository.findRegions()).thenReturn(List.of(new RegionMetadataRow(1L, "종로구")));
        when(metadataQueryRepository.findActiveKeywords())
                .thenReturn(List.of(new KeywordMetadataRow(3L, "도시공원")));
        MetadataService service = new MetadataService(metadataQueryRepository);

        assertThat(service.getRegions().getFirst().name()).isEqualTo("종로구");
        assertThat(service.getKeywords().getFirst().code()).isEqualTo("CITY_PARK");
    }
}
