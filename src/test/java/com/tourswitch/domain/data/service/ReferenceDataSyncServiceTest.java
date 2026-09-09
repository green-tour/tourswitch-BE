package com.tourswitch.domain.data.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import com.tourswitch.domain.data.repository.ExternalDataSyncRepository.InvalidAreaBoundary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataSyncServiceTest {

    @Mock
    private ExternalDataSyncRepository repository;

    @Test
    void synchronize_invalidPolygon_repairsBoundaryAndKeeps121Areas() {
        // Given
        when(repository.findInvalidAreaBoundaries()).thenReturn(List.of(
                new InvalidAreaBoundary(70L, "쌍문역", "POLYGON((0 0,2 2,0 2,2 0,0 0))")
        ));
        when(repository.countRealtimeAreas()).thenReturn(121);
        ReferenceDataSyncService service = new ReferenceDataSyncService(repository);

        // When
        ReferenceDataSyncService.ReferenceDataSyncResult result = service.synchronize();

        // Then
        verify(repository).updateAreaBoundary(org.mockito.ArgumentMatchers.eq(70L), anyString());
        assertThat(result.regions()).isEqualTo(25);
        assertThat(result.realtimeAreas()).isEqualTo(121);
        assertThat(result.repairedBoundaries()).isEqualTo(1);
    }
}
