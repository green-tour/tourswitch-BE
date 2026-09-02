package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/**
 * tatsCnctrRatedList 응답 항목. touristSpotName(tAtsNm)으로 areaBasedList2/locationBasedList2 결과의
 * title과 이름 매칭한다 - TatsCnctrRateService는 contentId를 제공하지 않는다(관광지명 기준 서비스).
 */
public record TourApiCongestionItem(String touristSpotName, String baseYmd, BigDecimal concentrationRate) {

    static TourApiCongestionItem from(JsonNode node) {
        return new TourApiCongestionItem(
                node.path("tAtsNm").asText(),
                node.path("baseYmd").asText(),
                new BigDecimal(node.path("cnctrRate").asText("0")));
    }
}
