"""서울시 121장소 Shapefile을 MySQL 8 UPSERT SQL로 변환한다."""

from pathlib import Path
import sys
import shapefile


def sql_text(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def rings(shape):
    starts = list(shape.parts) + [len(shape.points)]
    return [shape.points[starts[i]:starts[i + 1]] for i in range(len(shape.parts))]


def polygon_centroid(polygon_rings):
    weighted_x = weighted_y = signed_area_total = 0.0
    for ring in polygon_rings:
        cross_sum = cx_sum = cy_sum = 0.0
        for index, (x1, y1) in enumerate(ring):
            x2, y2 = ring[(index + 1) % len(ring)]
            cross = x1 * y2 - x2 * y1
            cross_sum += cross
            cx_sum += (x1 + x2) * cross
            cy_sum += (y1 + y2) * cross
        signed_area = cross_sum / 2.0
        if signed_area:
            weighted_x += (cx_sum / (6.0 * signed_area)) * signed_area
            weighted_y += (cy_sum / (6.0 * signed_area)) * signed_area
            signed_area_total += signed_area
    if not signed_area_total:
        raise ValueError("면적이 0인 폴리곤입니다.")
    return weighted_x / signed_area_total, weighted_y / signed_area_total


def ring_wkt(ring):
    points = list(ring)
    if points[0] != points[-1]:
        points.append(points[0])
    return "(" + ",".join(f"{x:.7f} {y:.7f}" for x, y in points) + ")"


def generate(shapefile_path: Path, output_path: Path):
    reader = shapefile.Reader(str(shapefile_path), encoding="utf-8")
    if len(reader) != 121:
        raise ValueError(f"121개 영역이 필요하지만 {len(reader)}개를 찾았습니다.")

    statements = [
        "-- 서울시 주요 121장소 영역 기준정보 (WGS84, SRID 4326)",
        "-- area_code 기준 재실행 가능한 UPSERT이며 기존 혼잡도 이력은 삭제하지 않는다.",
        "START TRANSACTION;",
        "",
    ]
    for shape_record in reader.iterShapeRecords():
        record = shape_record.record
        polygon_rings = rings(shape_record.shape)
        longitude, latitude = polygon_centroid(polygon_rings)
        wkt = "POLYGON(" + ",".join(ring_wkt(ring) for ring in polygon_rings) + ")"
        statements.extend([
            "INSERT INTO seoul_realtime_area",
            "    (area_code, area_name, category, latitude, longitude, boundary)",
            f"VALUES ({sql_text(record.AREA_CD)}, {sql_text(record.AREA_NM)}, "
            f"{sql_text(record.CATEGORY)}, {latitude:.7f}, {longitude:.7f}, "
            f"ST_GeomFromText({sql_text(wkt)}, 4326, 'axis-order=long-lat'))",
            "ON DUPLICATE KEY UPDATE",
            "    area_name = VALUES(area_name),",
            "    category = VALUES(category),",
            "    latitude = VALUES(latitude),",
            "    longitude = VALUES(longitude),",
            "    boundary = VALUES(boundary);",
            "",
        ])
    statements.extend([
        "COMMIT;",
        "",
        "SELECT COUNT(*) AS seoul_realtime_area_count FROM seoul_realtime_area;",
    ])
    output_path.write_text("\n".join(statements), encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("사용법: generate_seoul_realtime_area_sql.py 입력.shp 출력.sql")
    generate(Path(sys.argv[1]), Path(sys.argv[2]))
