package com.tourswitch.global.client.tourapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.global.error.CustomResponseCode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * data.go.kr 응답은 실제로 세 가지 다른 JSON 껍데기를 쓴다(2026-09-02 실제 호출로 확인):
 * ① 정상: {"response":{"header":{resultCode,resultMsg},"body":{...}}}
 * ② 게이트웨이 파라미터 오류: {"responseTime":..,"resultCode":"10","resultMsg":".."} (response 래퍼 없음)
 * ③ 인증키 오류: {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{errMsg,returnAuthMsg,returnReasonCode}}}
 * 이 셋을 구분해서 처리하지 않으면 인증키 문제를 일반 파싱 실패로 오인하게 된다.
 */
final class TourApiResponseParser {

    private TourApiResponseParser() {
    }

    static JsonNode parseBody(ObjectMapper objectMapper, String rawJson) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new TourApiClientException(CustomResponseCode.HTTP_ERROR, "TourAPI 응답을 파싱하지 못했습니다.");
        }

        if (root.has("OpenAPI_ServiceResponse")) {
            JsonNode header = root.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
            throw new TourApiClientException(resolveCode(header.path("returnReasonCode").asText()),
                    header.path("returnAuthMsg").asText("TourAPI 인증 오류"));
        }
        if (!root.has("response")) {
            throw new TourApiClientException(resolveCode(root.path("resultCode").asText()),
                    root.path("resultMsg").asText("TourAPI 요청 오류"));
        }

        JsonNode header = root.path("response").path("header");
        String resultCode = header.path("resultCode").asText();
        if (!"0000".equals(resultCode)) {
            throw new TourApiClientException(resolveCode(resultCode), header.path("resultMsg").asText("TourAPI 오류"));
        }
        return root.path("response").path("body");
    }

    static <T> List<T> mapItems(JsonNode body, Function<JsonNode, T> mapper) {
        JsonNode item = body.path("items").path("item");
        if (!item.isArray()) {
            if (item.isMissingNode() || item.isNull()) {
                return List.of();
            }
            return List.of(mapper.apply(item));
        }
        List<T> result = new ArrayList<>(item.size());
        item.forEach(node -> result.add(mapper.apply(node)));
        return result;
    }

    static int totalCount(JsonNode body) {
        return body.path("totalCount").asInt(0);
    }

    private static CustomResponseCode resolveCode(String rawCode) {
        return switch (rawCode) {
            case "10" -> CustomResponseCode.INVALID_REQUEST_PARAMETER_ERROR;
            case "11" -> CustomResponseCode.NO_MANDATORY_REQUEST_PARAMETERS_ERROR;
            case "12" -> CustomResponseCode.NO_OPENAPI_SERVICE_ERROR;
            case "20" -> CustomResponseCode.SERVICE_ACCESS_DENIED_ERROR;
            case "21" -> CustomResponseCode.TEMPORARILY_DISABLE_THE_SERVICEKEY_ERROR;
            case "22" -> CustomResponseCode.LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR;
            case "30" -> CustomResponseCode.SERVICE_KEY_IS_NOT_REGISTERED_ERROR;
            case "31" -> CustomResponseCode.DEADLINE_HAS_EXPIRED_ERROR;
            case "32" -> CustomResponseCode.UNREGISTERED_IP_ERROR;
            case "33" -> CustomResponseCode.UNSIGNED_CALL_ERROR;
            default -> CustomResponseCode.UNKNOWN_ERROR;
        };
    }
}
