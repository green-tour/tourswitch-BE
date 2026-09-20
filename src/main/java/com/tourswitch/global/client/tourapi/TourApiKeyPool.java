package com.tourswitch.global.client.tourapi;

import com.tourswitch.global.error.CustomResponseCode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/**
 * 한 오퍼레이션에 쓸 수 있는 서비스키 묶음. 개발계정은 오퍼레이션당 하루 호출 수가 묶여 있어
 * 키 하나가 소진되면 그 날은 더 못 부른다. 여러 키를 순서대로 물려 두고 소진된 키를 만나면
 * 다음 키로 같은 요청을 다시 보낸다.
 *
 * 키 문제가 아닌 오류(파라미터 오류 등)는 다른 키로 바꿔도 똑같이 실패하므로 그대로 올린다.
 */
@Slf4j
public class TourApiKeyPool {

    /** 키를 바꾸면 풀릴 수 있는 오류들. 한도 초과, 키 정지·미등록·만료, 허용되지 않은 호출 등이다. */
    private static final Set<CustomResponseCode> KEY_RELATED_CODES = EnumSet.of(
            CustomResponseCode.LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR,
            CustomResponseCode.TEMPORARILY_DISABLE_THE_SERVICEKEY_ERROR,
            CustomResponseCode.SERVICE_ACCESS_DENIED_ERROR,
            CustomResponseCode.SERVICE_KEY_IS_NOT_REGISTERED_ERROR,
            CustomResponseCode.DEADLINE_HAS_EXPIRED_ERROR,
            CustomResponseCode.UNREGISTERED_IP_ERROR,
            CustomResponseCode.UNSIGNED_CALL_ERROR);

    private final String name;
    private final List<String> keys;
    // 한 번 통한 키에서 다음 호출을 시작한다. 소진된 키를 매번 다시 찔러 한도를 낭비하지 않는다.
    private final AtomicInteger cursor = new AtomicInteger(0);

    public TourApiKeyPool(String name, List<String> serviceKeys) {
        if (serviceKeys == null || serviceKeys.isEmpty()) {
            throw new IllegalArgumentException(name + "에 사용할 TourAPI 서비스키가 없습니다.");
        }
        this.name = name;
        this.keys = serviceKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .map(key -> URLDecoder.decode(key.trim(), StandardCharsets.UTF_8))
                .toList();
        if (this.keys.isEmpty()) {
            throw new IllegalArgumentException(name + "에 사용할 TourAPI 서비스키가 없습니다.");
        }
    }

    public int size() {
        return keys.size();
    }

    /**
     * 서비스키를 받아 호출하는 함수를 실행한다. 키 때문에 막히면 다음 키로 같은 호출을 다시 한다.
     * 모든 키가 막히면 마지막 오류를 올린다.
     */
    public <T> T execute(Function<String, T> call) {
        int start = cursor.get();
        TourApiClientException lastFailure = null;

        for (int attempt = 0; attempt < keys.size(); attempt++) {
            int index = (start + attempt) % keys.size();
            try {
                T result = call.apply(keys.get(index));
                cursor.set(index);
                return result;
            } catch (TourApiClientException e) {
                if (!KEY_RELATED_CODES.contains(e.getCode())) {
                    throw e;
                }
                lastFailure = e;
                log.warn("{} 서비스키 {}/{}가 막혀 다음 키로 넘어간다. 사유={}",
                        name, index + 1, keys.size(), e.getMessage());
            }
        }

        log.error("{} 서비스키 {}개가 모두 막혔다.", name, keys.size());
        throw lastFailure;
    }
}
