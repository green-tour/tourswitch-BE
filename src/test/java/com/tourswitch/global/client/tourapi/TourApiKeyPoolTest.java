package com.tourswitch.global.client.tourapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tourswitch.global.error.CustomResponseCode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 키 하나가 하루 한도에 걸려도 다음 키로 이어 부르는지 확인한다.
 */
class TourApiKeyPoolTest {

    private static final String FIRST = "key-1";
    private static final String SECOND = "key-2";

    private static TourApiClientException quotaExceeded() {
        return new TourApiClientException(
                CustomResponseCode.LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR, "한도 초과");
    }

    @Test
    void 첫_키가_되면_그_키만_쓴다() {
        List<String> used = new ArrayList<>();
        TourApiKeyPool pool = new TourApiKeyPool("test", List.of(FIRST, SECOND));

        String result = pool.execute(key -> {
            used.add(key);
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(used).containsExactly(FIRST);
    }

    @Test
    void 한도에_걸리면_다음_키로_같은_요청을_다시_보낸다() {
        List<String> used = new ArrayList<>();
        TourApiKeyPool pool = new TourApiKeyPool("test", List.of(FIRST, SECOND));

        String result = pool.execute(key -> {
            used.add(key);
            if (FIRST.equals(key)) throw quotaExceeded();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(used).containsExactly(FIRST, SECOND);
    }

    @Test
    void 한번_넘어간_뒤에는_막힌_키를_다시_찌르지_않는다() {
        List<String> used = new ArrayList<>();
        TourApiKeyPool pool = new TourApiKeyPool("test", List.of(FIRST, SECOND));
        pool.execute(key -> {
            if (FIRST.equals(key)) throw quotaExceeded();
            return "ok";
        });

        pool.execute(key -> {
            used.add(key);
            return "ok";
        });

        assertThat(used).containsExactly(SECOND);
    }

    @Test
    void 키_문제가_아닌_오류는_다른_키로_재시도하지_않는다() {
        List<String> used = new ArrayList<>();
        TourApiKeyPool pool = new TourApiKeyPool("test", List.of(FIRST, SECOND));

        assertThatThrownBy(() -> pool.execute(key -> {
            used.add(key);
            throw new TourApiClientException(CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR, "파라미터 오류");
        })).isInstanceOf(TourApiClientException.class);

        assertThat(used).containsExactly(FIRST);
    }

    @Test
    void 모든_키가_막히면_마지막_오류를_올린다() {
        TourApiKeyPool pool = new TourApiKeyPool("test", List.of(FIRST, SECOND));

        assertThatThrownBy(() -> pool.execute(key -> {
            throw quotaExceeded();
        }))
                .isInstanceOf(TourApiClientException.class)
                .hasMessage("한도 초과");
    }

    @Test
    void 키가_하나도_없으면_기동에서_막는다() {
        assertThatThrownBy(() -> new TourApiKeyPool("test", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TourApiKeyPool("test", List.of("  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
