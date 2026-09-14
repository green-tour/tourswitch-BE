package com.tourswitch.domain.data.service;

import static org.mockito.Mockito.verify;

import com.tourswitch.domain.data.repository.ExternalDataSyncRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceDataSyncServiceTest {

    @Mock
    private ExternalDataSyncRepository repository;

    @Test
    void synchronize_seedsSeoulRegions() {
        // Given
        ReferenceDataSyncService service = new ReferenceDataSyncService(repository);

        // When
        service.synchronize();

        // Then
        verify(repository).seedRegions(org.mockito.ArgumentMatchers.argThat(regions -> regions.size() == 25));
    }
}
