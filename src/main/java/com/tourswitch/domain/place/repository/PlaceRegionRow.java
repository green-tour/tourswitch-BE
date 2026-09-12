package com.tourswitch.domain.place.repository;

/**
 * region은 이 도메인이 소유하지 않는 지역 기준정보라 네이티브 쿼리로 읽기 전용 조회만 한다(B1 규칙).
 * legalDongAreaCode/legalDongDistrictCode는 KorService2용 3자리 법정동 코드, districtCode는
 * TatsCnctrRateService용 5자리 시군구 코드로 서로 다르다(TourAPI 실시간전환 계획 문서 11절).
 */
public record PlaceRegionRow(String districtName, String legalDongAreaCode, String legalDongDistrictCode,
                              String districtCode) {
}
