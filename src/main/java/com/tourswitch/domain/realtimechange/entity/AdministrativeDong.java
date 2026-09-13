package com.tourswitch.domain.realtimechange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서울 자치구에 속한 행정동 기준정보.
 *
 * <p>대표 위·경도는 GPS 대신 사용자가 선택한 동을 기준으로 3km 거리 검색을 수행할 때 사용한다.</p>
 */
@Entity
@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "administrative_dong", uniqueConstraints = {
        @UniqueConstraint(name = "uk_administrative_dong_code", columnNames = "dong_code"),
        @UniqueConstraint(name = "uk_administrative_dong_region_name", columnNames = {"region_id", "dong_name"})
})
public class AdministrativeDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "dong_code", nullable = false, length = 20)
    private String dongCode;

    @Column(name = "dong_name", nullable = false, length = 50)
    private String dongName;

    @Column(name = "center_latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLongitude;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    private AdministrativeDong(Long regionId, String dongCode, String dongName,
                               BigDecimal centerLatitude, BigDecimal centerLongitude) {
        this.regionId = regionId;
        this.dongCode = dongCode;
        this.dongName = dongName;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.isActive = true;
    }

    public static AdministrativeDong create(Long regionId, String dongCode, String dongName,
                                            BigDecimal centerLatitude, BigDecimal centerLongitude) {
        return new AdministrativeDong(regionId, dongCode, dongName, centerLatitude, centerLongitude);
    }

    public void deactivate() {
        this.isActive = false;
    }
}
