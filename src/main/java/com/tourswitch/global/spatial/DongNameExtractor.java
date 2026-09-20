package com.tourswitch.global.spatial;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TourAPI 주소에서 동 이름을 뽑는다.
 *
 * 관광지 목록 응답에는 동 단위 코드가 없고 주소만 있다. 도로명 주소는 괄호 안에,
 * 지번 주소는 자치구 뒤에 동이 온다(2026-09-21 실제 응답으로 확인).
 *   "서울특별시 종로구 북촌로 57 (가회동)"  -> 가회동
 *   "서울특별시 종로구 가회동 1-1"          -> 가회동
 *
 * 여기서 얻는 것은 법정동이다. 대체 장소 찾기는 이름과 중심 좌표만 쓰므로 충분하지만,
 * 행정동과 같은 단위가 아니라는 점은 알고 써야 한다.
 */
public final class DongNameExtractor {

    // 괄호 안에 동이 오는 도로명 주소. "(가회동)", "(종로1가, 세운상가)" 모두 잡는다.
    private static final Pattern ROAD_ADDRESS = Pattern.compile("\\(([^)]*?)\\)");
    // 지번 주소. "서울특별시 종로구 가회동 1-1"
    // 자바 정규식의 \b는 ASCII 기준이라 한글과 공백 사이를 경계로 보지 않는다.
    // 뒤에 한글이 더 붙지 않는다는 조건으로 대신한다.
    private static final Pattern LOT_ADDRESS =
            Pattern.compile("^\\S*시\\s+\\S+[구군]\\s+([가-힣]+(?:\\d+가|동|읍|면))(?![가-힣])");
    // 괄호 안에서 동만 골라낸다. 쉼표로 여러 개가 올 수 있어 첫 번째만 쓴다.
    private static final Pattern DONG_TOKEN = Pattern.compile("([가-힣]+(?:\\d+가|동|읍|면))");

    private DongNameExtractor() {
    }

    public static Optional<String> extract(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        Matcher road = ROAD_ADDRESS.matcher(address);
        while (road.find()) {
            Matcher dong = DONG_TOKEN.matcher(road.group(1));
            if (dong.find()) {
                return Optional.of(dong.group(1));
            }
        }

        Matcher lot = LOT_ADDRESS.matcher(address.trim());
        return lot.find() ? Optional.of(lot.group(1)) : Optional.empty();
    }
}
