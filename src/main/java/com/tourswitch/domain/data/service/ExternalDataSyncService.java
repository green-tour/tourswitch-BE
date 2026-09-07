package com.tourswitch.domain.data.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourswitch.global.config.DataSyncProperties;
import com.tourswitch.global.config.ExternalApiProperties;
import com.tourswitch.global.config.SeoulApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 한지윤 담당 데이터 연동 영역.
 * 외부 호출은 트랜잭션 밖에서 수행하고, 각 원천 테이블은 자연키 기준으로 upsert한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalDataSyncService {
    private static final String TOUR_BASE = "https://apis.data.go.kr/B551011";
    private static final String SEOUL_BASE = "http://openapi.seoul.go.kr:8088";
    private static final String[] SEOUL_DISTRICTS = {
            "11110","11140","11170","11200","11215","11230","11260","11290","11305",
            "11320","11350","11380","11410","11440","11470","11500","11530","11545",
            "11560","11590","11620","11650","11680","11710","11740"
    };
    private static final int PAGE_SIZE = 1000;

    private final JdbcTemplate jdbc;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper mapper;
    private final ExternalApiProperties tourApi;
    private final SeoulApiProperties seoulApi;
    private final DataSyncProperties syncProperties;

    public int syncTouristSpots() {
        requireKey(tourApi.secretKey(), "TOUR_API_SERVICE_KEY");
        List<JsonNode> items = fetchTourPages("/KorService2/areaBasedList2",
                b -> b.queryParam("lDongRegnCd", "11"));
        // 문서의 재수집 정책: API에 더 이상 나타나지 않은 장소는 삭제하지 않고 비활성화한다.
        jdbc.update("UPDATE tourist_spot ts JOIN region r ON r.id=ts.region_id SET ts.is_active=FALSE WHERE r.area_code='11'");
        int count = 0;
        for (JsonNode item : items) {
            int type = intValue(item, "contenttypeid");
            if (!List.of(12, 14, 15, 28, 32, 38, 39).contains(type)) continue;
            BigDecimal lat = decimal(item, "mapy");
            BigDecimal lon = decimal(item, "mapx");
            String contentId = text(item, "contentid");
            String title = text(item, "title");
            if (!StringUtils.hasText(contentId) || !StringUtils.hasText(title) || lat == null || lon == null) continue;
            boolean coordinateValid = lat.compareTo(new BigDecimal("37")) >= 0
                    && lat.compareTo(new BigDecimal("38")) <= 0
                    && lon.compareTo(new BigDecimal("126")) >= 0
                    && lon.compareTo(new BigDecimal("128")) <= 0;
            jdbc.update("""
                INSERT INTO tourist_spot
                (content_id, content_type_id, title, normalized_title, address, latitude, longitude,
                 location_point, first_image_url, classification_level1_code, classification_level2_code,
                 classification_level3_code, region_id, is_coordinate_valid, has_crowd_data, is_active, data_synced_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ST_GeomFromText(CONCAT('POINT(', ?, ' ', ?, ')'), 4326, 'axis-order=long-lat'), ?, ?, ?, ?,
                        (SELECT id FROM region WHERE area_code = '11' AND district_code = ? LIMIT 1),
                        ?, FALSE, TRUE, UTC_TIMESTAMP())
                ON DUPLICATE KEY UPDATE
                  content_type_id=VALUES(content_type_id), title=VALUES(title), normalized_title=VALUES(normalized_title),
                  address=VALUES(address), latitude=VALUES(latitude), longitude=VALUES(longitude),
                  location_point=VALUES(location_point), first_image_url=VALUES(first_image_url),
                  classification_level1_code=VALUES(classification_level1_code),
                  classification_level2_code=VALUES(classification_level2_code), classification_level3_code=VALUES(classification_level3_code),
                  region_id=VALUES(region_id), is_coordinate_valid=VALUES(is_coordinate_valid),
                  is_active=TRUE, data_synced_at=UTC_TIMESTAMP()
                """, contentId, type, title, normalize(title), text(item,"addr1"), lat, lon, lon, lat,
                    text(item,"firstimage"), text(item,"lclsSystm1"), text(item,"lclsSystm2"), text(item,"lclsSystm3"),
                    text(item,"lDongSignguCd", text(item,"sigungucode")),
                    coordinateValid);
            count++;
        }
        return count;
    }

    public int syncCrowdForecasts() {
        requireKey(tourApi.secretKey(), "TOUR_API_SERVICE_KEY");
        int count = 0;
        for (String district : SEOUL_DISTRICTS) {
            for (JsonNode item : fetchTourPages("/TatsCnctrRateService/tatsCnctrRatedList",
                    b -> b.queryParam("areaCd", "11").queryParam("signguCd", district))) {
                String name = text(item, "tAtsNm");
                BigDecimal rate = decimal(item, "cnctrRate");
                LocalDate baseDate = date(text(item, "baseYmd"));
                if (!StringUtils.hasText(name) || rate == null || baseDate == null) continue;
                jdbc.update("""
                    INSERT INTO spot_crowd_forecast
                    (area_code, district_code, district_name, attraction_name, normalized_attraction_name,
                     forecast_date, concentration_rate, collected_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())
                    ON DUPLICATE KEY UPDATE district_name=VALUES(district_name), concentration_rate=VALUES(concentration_rate),
                      collected_at=UTC_TIMESTAMP()
                    """, text(item,"areaCd","11"), district, text(item,"signguNm"), name, normalize(name), baseDate, rate);
                count++;
            }
        }
        jdbc.update("""
            INSERT IGNORE INTO spot_crowd_link
              (tourist_spot_id, attraction_name, district_code, match_method, is_reviewed, matched_at)
            SELECT ts.id, f.attraction_name, f.district_code,
              CASE WHEN ts.title=f.attraction_name THEN 'EXACT' ELSE 'NORMALIZED' END,
              FALSE, UTC_TIMESTAMP()
            FROM tourist_spot ts
            JOIN region r ON r.id=ts.region_id
            JOIN spot_crowd_forecast f ON f.district_code=r.district_code
              AND f.normalized_attraction_name=ts.normalized_title
            WHERE NOT EXISTS (SELECT 1 FROM spot_duplicate_link d WHERE d.tourist_spot_id=ts.id)
            """);
        jdbc.update("""
            UPDATE tourist_spot ts JOIN spot_crowd_link scl ON scl.tourist_spot_id=ts.id
            SET ts.has_crowd_data=TRUE
            """);
        return count;
    }

    public int syncSeoulRealtime() {
        requireKey(seoulApi.secretKey(), "SEOUL_OPEN_API_KEY");
        int count = 0;
        for (String areaCode : syncProperties.areaCodes()) {
            URI uri = UriComponentsBuilder.fromUriString(seoulApi.resolvedBaseUrl())
                    .pathSegment(seoulApi.secretKey(), "json", "citydata_ppltn", "1", "5", areaCode)
                    .build().encode().toUri();
            JsonNode root = read(uri);
            JsonNode service = root.path("SeoulRtd");
            JsonNode items = root.path("SeoulRtd.citydata_ppltn");
            JsonNode result = root.path("RESULT");
            String resultCode = result.path("RESULT.CODE").asText(result.path("CODE").asText());
            String resultMessage = result.path("RESULT.MESSAGE").asText(result.path("MESSAGE").asText());
            if (result.isObject() && StringUtils.hasText(resultCode) && !"INFO-000".equals(resultCode)) {
                log.warn("서울 실시간 API 응답 오류: areaCode={}, code={}, message={}",
                        areaCode, resultCode, resultMessage);
            }
            JsonNode item = items.path(0);
            if (item.isMissingNode()) item = service.path("row").path(0);
            if (item.isMissingNode()) item = service.path("citydata_ppltn").path(0);
            if (item.isMissingNode()) {
                log.warn("서울 실시간 API 데이터 없음: areaCode={}", areaCode);
                continue;
            }
            String code = text(item,"AREA_CD", areaCode);
            List<Long> areaIds = jdbc.query("SELECT id FROM seoul_realtime_area WHERE area_code=?", (rs, row) -> rs.getLong(1), code);
            if (areaIds.isEmpty()) {
                // boundary는 서울 데이터셋 첨부 Shapefile에서 적재하는 기준정보다. API 호출 중 임의 폴리곤을 만들지 않는다.
                continue;
            }
            Long areaId = areaIds.getFirst();
            jdbc.update("""
              INSERT INTO seoul_realtime_population(seoul_realtime_area_id, congestion_level, congestion_message,
                population_min, population_max, resident_rate, non_resident_rate, observed_at, collected_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, UTC_TIMESTAMP())
              """, areaId, text(item,"AREA_CONGEST_LVL"), text(item,"AREA_CONGEST_MSG"),
                    intValue(item,"AREA_PPLTN_MIN"), intValue(item,"AREA_PPLTN_MAX"), decimal(item,"RESNT_PPLTN_RATE"),
                    decimal(item,"NON_RESNT_PPLTN_RATE"), dateTime(text(item,"PPLTN_TIME")));
            for (JsonNode forecast : item.path("FCST_PPLTN")) {
                jdbc.update("""
                  INSERT INTO seoul_realtime_forecast(seoul_realtime_area_id, forecast_time, congestion_level,
                    population_min, population_max, collected_at)
                  VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP())
                  """, areaId, dateTime(text(forecast,"FCST_TIME")), text(forecast,"FCST_CONGEST_LVL"),
                        intValue(forecast,"FCST_PPLTN_MIN"), intValue(forecast,"FCST_PPLTN_MAX"));
            }
            count++;
        }
        jdbc.update("DELETE FROM seoul_realtime_population WHERE collected_at < UTC_TIMESTAMP() - INTERVAL 30 DAY");
        jdbc.update("DELETE FROM seoul_realtime_forecast WHERE forecast_time < UTC_TIMESTAMP() OR collected_at < UTC_TIMESTAMP() - INTERVAL 3 DAY");
        syncAreaLinks();
        return count;
    }

    private void syncAreaLinks() {
        jdbc.update("""
          INSERT IGNORE INTO spot_area_link
            (tourist_spot_id, seoul_realtime_area_id, match_method, distance_meters,
             name_match_priority, is_primary, is_reviewed, matched_at)
          SELECT ts.id, a.id, 'INSIDE_BOUNDARY', 0, 1, FALSE, FALSE, UTC_TIMESTAMP()
          FROM tourist_spot ts JOIN seoul_realtime_area a
            ON ST_Contains(a.boundary, ts.location_point)
          WHERE ts.is_active=TRUE AND ts.is_coordinate_valid=TRUE
            AND NOT EXISTS (SELECT 1 FROM spot_duplicate_link d WHERE d.tourist_spot_id=ts.id)
            AND NOT EXISTS (SELECT 1 FROM spot_area_link x WHERE x.tourist_spot_id=ts.id AND x.seoul_realtime_area_id=a.id)
          """);
        jdbc.update("""
          INSERT IGNORE INTO spot_area_link
            (tourist_spot_id, seoul_realtime_area_id, match_method, distance_meters,
             name_match_priority, is_primary, is_reviewed, matched_at)
          SELECT ts.id, a.id, 'PROXIMITY',
            CAST(ST_Distance_Sphere(ts.location_point,
                ST_GeomFromText(CONCAT('POINT(', a.longitude, ' ', a.latitude, ')'), 4326, 'axis-order=long-lat')) AS UNSIGNED),
            3, FALSE, FALSE, UTC_TIMESTAMP()
          FROM tourist_spot ts JOIN seoul_realtime_area a
          WHERE ts.is_active=TRUE AND ts.is_coordinate_valid=TRUE
            AND NOT EXISTS (SELECT 1 FROM spot_duplicate_link d WHERE d.tourist_spot_id=ts.id)
            AND NOT EXISTS (SELECT 1 FROM spot_area_link x WHERE x.tourist_spot_id=ts.id AND x.seoul_realtime_area_id=a.id)
            AND a.latitude IS NOT NULL AND a.longitude IS NOT NULL
            AND ST_Distance_Sphere(ts.location_point,
                ST_GeomFromText(CONCAT('POINT(', a.longitude, ' ', a.latitude, ')'), 4326, 'axis-order=long-lat')) <= 1000
          """);
        jdbc.update("UPDATE spot_area_link SET is_primary=FALSE");
        jdbc.update("""
          UPDATE spot_area_link sal
          JOIN (SELECT tourist_spot_id, MIN(id) AS id FROM spot_area_link GROUP BY tourist_spot_id) picked
            ON picked.id=sal.id
          SET sal.is_primary=TRUE
          """);
    }

    private List<JsonNode> fetchTourPages(String path, java.util.function.Consumer<UriComponentsBuilder> extra) {
        List<JsonNode> all = new ArrayList<>();
        int page = 1, total;
        do {
            UriComponentsBuilder b = UriComponentsBuilder.fromUriString(TOUR_BASE + path)
                    .queryParam("serviceKey", tourApi.secretKey()).queryParam("numOfRows", PAGE_SIZE)
                    .queryParam("pageNo", page++).queryParam("MobileOS", tourApi.mobileOs())
                    .queryParam("MobileApp", tourApi.mobileApp()).queryParam("_type", "json");
            extra.accept(b);
            JsonNode body = read(b.build().encode().toUri()).path("response").path("body");
            JsonNode items = body.path("items").path("item");
            if (items.isArray()) items.forEach(all::add); else if (!items.isMissingNode() && !items.isNull()) all.add(items);
            total = body.path("totalCount").asInt(all.size());
        } while (all.size() < total && page < 1000);
        return all;
    }

    private JsonNode read(URI uri) {
        String body = restClientBuilder.build().get().uri(uri).retrieve().body(String.class);
        try { return mapper.readTree(body); } catch (Exception e) { throw new IllegalStateException("외부 API JSON 파싱 실패: " + uri, e); }
    }
    private static void requireKey(String key, String name) { if (!StringUtils.hasText(key)) throw new IllegalStateException(name + "가 설정되지 않았습니다."); }
    private static String text(JsonNode n, String k) { return text(n,k,null); }
    private static String text(JsonNode n, String k, String fallback) { String v=n.path(k).asText(null); return StringUtils.hasText(v)?v:fallback; }
    private static int intValue(JsonNode n, String k) { return n.path(k).asInt(0); }
    private static BigDecimal decimal(JsonNode n, String k) { String v=text(n,k); try{return v==null?null:new BigDecimal(v.replace(",",""));}catch(Exception e){return null;} }
    private static LocalDate date(String v) { try{return v==null?null:LocalDate.parse(v, DateTimeFormatter.BASIC_ISO_DATE);}catch(Exception e){return null;} }
    private static LocalDateTime dateTime(String v) { try{return v==null?null:LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));}catch(Exception e){return LocalDateTime.now();} }
    private static String normalize(String value) { return value.replaceAll("[^가-힣A-Za-z0-9]", "").toLowerCase(); }
}
