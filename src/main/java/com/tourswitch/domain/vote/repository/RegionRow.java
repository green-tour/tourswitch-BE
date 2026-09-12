package com.tourswitch.domain.vote.repository;

/**
 * region은 TourAPI 응답이 아니라 우리 자체 지역 기준정보(서울 25개 자치구)라 계속 로컬로 둔다
 * (TourAPI 실시간전환 계획 문서 2절). legalDongAreaCode/legalDongDistrictCode는 KorService2용
 * 3자리 법정동 코드, districtCode는 TatsCnctrRateService용 5자리 시군구 코드로 서로 다르다.
 */
public record RegionRow(String legalDongAreaCode, String legalDongDistrictCode, String districtCode) {
}
