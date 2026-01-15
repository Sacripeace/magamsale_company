package com.magamsale.store.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/location")
public class LocationController {

    @Value("${kakao.api.key}")
    private String kakaoRestApiKey;

    @PostMapping("/geocode")
    public ResponseEntity<?> getGeoCode(@RequestBody Map<String, String> request) {
        String address = request.get("address");

        if (address == null || address.isEmpty()) {
            return ResponseEntity.badRequest().body("주소가 비어있습니다.");
        }

        String url = "https://dapi.kakao.com/v2/local/search/address.json?query=" + address;

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() == null || response.getBody().get("documents") == null) {
                return ResponseEntity.status(500).body("좌표 정보를 가져올 수 없습니다.");
            }

            var documents = (java.util.List<Map<String, Object>>) response.getBody().get("documents");

            if (documents.isEmpty()) {
                return ResponseEntity.status(404).body("검색된 주소가 없습니다.");
            }

            // 첫 번째 검색 결과
            Map<String, Object> location = documents.get(0);

            Map<String, Object> roadAddress = (Map<String, Object>) location.get("road_address");
            Map<String, Object> geoData = new HashMap<>();

            if (roadAddress != null) {
                geoData.put("lat", Double.parseDouble((String) roadAddress.get("y")));
                geoData.put("lng", Double.parseDouble((String) roadAddress.get("x")));
            } else {
                // 지번 주소일 경우
                Map<String, Object> addressInfo = (Map<String, Object>) location.get("address");
                geoData.put("lat", Double.parseDouble((String) addressInfo.get("y")));
                geoData.put("lng", Double.parseDouble((String) addressInfo.get("x")));
            }

            log.info("주소 변환 완료: {} → lat={}, lng={}", address, geoData.get("lat"), geoData.get("lng"));

            return ResponseEntity.ok(geoData);

        } catch (Exception e) {
            log.error("주소 변환 중 오류", e);
            return ResponseEntity.status(500).body("주소 변환 실패: " + e.getMessage());
        }
    }
}