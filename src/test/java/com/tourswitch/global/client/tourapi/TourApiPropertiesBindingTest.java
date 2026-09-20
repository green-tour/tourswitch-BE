package com.tourswitch.global.client.tourapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * 서비스키를 환경변수 하나에 쉼표로 이어 넣는다. 그 문자열이 목록으로 바인딩되지 않으면
 * 기동이 깨지므로 규약을 고정해 둔다.
 */
class TourApiPropertiesBindingTest {

    private static TourApiProperties bind(Map<String, Object> properties) {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addFirst(new MapPropertySource("test", properties));
        return new Binder(ConfigurationPropertySources.from(sources))
                .bind("tourapi", TourApiProperties.class)
                .orElseThrow(() -> new IllegalStateException("바인딩 실패"));
    }

    @Test
    void 쉼표로_이은_문자열이_키_목록으로_바인딩된다() {
        TourApiProperties properties = bind(Map.of(
                "tourapi.base-url", "https://example.test",
                "tourapi.connect-timeout-ms", "3000",
                "tourapi.read-timeout-ms", "5000",
                "tourapi.kor-service.service-keys", "key-1,key-2",
                "tourapi.tats-cnctr-rate.service-keys", "key-3"));

        assertThat(properties.korService().serviceKeys()).containsExactly("key-1", "key-2");
        assertThat(properties.tatsCnctrRate().serviceKeys()).containsExactly("key-3");
    }

    @Test
    void 바인딩한_키_목록으로_풀을_만들_수_있다() {
        TourApiProperties properties = bind(Map.of(
                "tourapi.base-url", "https://example.test",
                "tourapi.connect-timeout-ms", "3000",
                "tourapi.read-timeout-ms", "5000",
                "tourapi.kor-service.service-keys", "key-1,key-2",
                "tourapi.tats-cnctr-rate.service-keys", "key-3"));

        assertThat(new TourApiKeyPool("KorService2", properties.korService().serviceKeys()).size()).isEqualTo(2);
        assertThat(new TourApiKeyPool("TatsCnctrRateService", properties.tatsCnctrRate().serviceKeys()).size())
                .isEqualTo(1);
    }
}
