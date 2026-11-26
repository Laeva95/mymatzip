package com.example.mymatzip.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

// ============================================================
// [NtsBusinessApiClient.java] 국세청 사업자등록정보 진위확인 · 상태조회 API 호출
// ============================================================
// 공공데이터포털 "국세청_사업자등록정보 진위확인 및 상태조회 서비스"
//   진위확인 POST https://api.odcloud.kr/api/nts-businessman/v1/validate
//            보내는 값: 사업자번호(b_no, 숫자 10자리), 개업일자(start_dt, YYYYMMDD), 대표자 성명(p_nm)
//            받는 값  : valid "01" = 일치, "02" = 확인할 수 없음 + 일치하면 사업 상태(status)
//   상태조회 POST https://api.odcloud.kr/api/nts-businessman/v1/status
//            보내는 값: 사업자번호 목록(b_no) / 받는 값: b_stt_cd "01" 계속 · "02" 휴업 · "03" 폐업
//
// 서비스 키: developer.properties(로컬) 또는 서버 환경 변수의 OWNER_VALIDATE
//   - 디코딩키(%, 없음)면 한 번 인코딩해서 보내고, 인코딩키(% 포함)면 그대로 보냄 → 어느 쪽을 넣어도 동작
//   - 키가 없어도 앱은 실행됨 (신청할 때만 "확인 불가" 503)
//
// 테스트 신청: verifyForTest() 는 API 를 부르지 않고 번호표로 결과를 정함 (시연용, 배포 서버에서도 사용 가능)
//   테스트 신청은 국세청 확인이 아니므로 "확인됨"으로 저장하지 않고 testApplication = true 로 구분
//
// 보안: 요청 URL 에 서비스 키가 들어가므로 예외 메시지(e.getMessage())를 로그나 응답에 그대로 쓰지 않음.
//       대표자 성명도 응답(request_param)에 다시 담겨 오므로 응답 내용을 로그에 남기지 않음.
// ============================================================
@Slf4j
@Component
public class NtsBusinessApiClient {

    private static final String VALIDATE_URL = "https://api.odcloud.kr/api/nts-businessman/v1/validate";
    private static final String STATUS_URL = "https://api.odcloud.kr/api/nts-businessman/v1/status";

    private final RestClient restClient;
    private final String serviceKey;

    public NtsBusinessApiClient(@Value("${OWNER_VALIDATE:}") String serviceKey) {
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        // 연결 5초, 응답 10초 제한 — 국세청 API 가 느려도 신청 요청이 오래 붙잡히지 않게
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 사업자번호 · 개업일자 · 대표자 성명이 국세청 등록 정보와 일치하는지, 지금 영업 상태가 무엇인지 확인한다.
     * @param businessNumber "123-45-67890" 또는 "1234567890"
     * @param openDate       "YYYYMMDD"
     * @throws UnavailableException 키 없음 · 네트워크 오류 · API 오류 (일치 여부를 알 수 없음)
     */
    public Result verify(String businessNumber, String ownerName, String openDate) {
        if (serviceKey.isBlank()) {
            log.warn("[국세청 API] OWNER_VALIDATE 서비스 키가 설정되지 않았습니다.");
            throw new UnavailableException();
        }
        String bNo = businessNumber.replaceAll("\\D", "");

        Map<String, Object> validate = post(VALIDATE_URL, Map.of("businesses",
                List.of(Map.of("b_no", bNo, "start_dt", openDate, "p_nm", ownerName))));
        Map<String, Object> item = firstItem(validate);
        if (!"01".equals(text(item.get("valid")))) {
            return Result.notMatched();
        }

        // 진위확인 응답에 보통 상태가 함께 오지만, 없으면 상태조회 API 로 한 번 더 확인
        Map<String, Object> status = asMap(item.get("status"));
        if (text(status.get("b_stt_cd")).isBlank()) {
            status = firstItem(post(STATUS_URL, Map.of("b_no", List.of(bNo))));
        }
        String statusCode = text(status.get("b_stt_cd"));
        if (statusCode.isBlank()) {
            log.warn("[국세청 API] 진위확인은 일치했지만 사업 상태를 받지 못했습니다.");
            throw new UnavailableException();
        }
        return new Result(true, statusCode, text(status.get("b_stt")), text(status.get("tax_type")));
    }

    /**
     * 시연 · 테스트용: 국세청 API 를 부르지 않고 사업자번호로 결과를 정한다. (신청 폼의 "테스트 신청" 체크)
     *   111-11-11119 → 휴업,  222-22-22227 → 폐업,  333-33-33336 → 정보 불일치,  444-44-44444 → 확인 불가(503)
     *   그 밖의 번호(체크섬이 맞는 것, 예: 1234567891) → 일치 · 계속사업자
     * 체크섬(1단계)은 서비스에서 먼저 검사하므로 위 번호들은 모두 체크섬이 맞게 골라 둠
     * 신청 폼은 숫자만 입력받으므로(apply.html 의 테스트 번호표도 숫자만) 하이픈을 지운 값으로 비교함
     * 대표자 성명 · 개업일자는 보지 않음 (검사하지 않으므로 아무 값이나 넣어도 됨, 개업일자 형식 검사는 서비스에서)
     */
    public Result verifyForTest(String businessNumber) {
        return switch (businessNumber.replaceAll("\\D", "")) {
            case "1111111119" -> new Result(true, "02", "휴업자", "테스트");
            case "2222222227" -> new Result(true, "03", "폐업자", "테스트");
            case "3333333336" -> Result.notMatched();
            case "4444444444" -> throw new UnavailableException();
            default -> new Result(true, "01", "계속사업자", "테스트");
        };
    }

    // ── POST 호출 + 공통 응답 확인 ──────────────────────────────
    private Map<String, Object> post(String url, Map<String, Object> body) {
        Map<String, Object> json;
        try {
            json = restClient.post()
                    .uri(uri(url))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            // 401 · 403: 키 오류 또는 활용신청 미승인, 400: 요청 형식 오류, 5xx: 국세청 쪽 장애
            log.warn("[국세청 API] HTTP {} 응답", e.getStatusCode().value());
            throw new UnavailableException();
        } catch (RestClientException e) {
            // 메시지에 서비스 키가 든 URL 이 포함될 수 있어 예외 종류만 기록
            log.warn("[국세청 API] 호출 실패: {}", e.getClass().getSimpleName());
            throw new UnavailableException();
        }
        if (json == null || !"OK".equals(text(json.get("status_code")))) {
            log.warn("[국세청 API] 비정상 응답 status_code={}", json == null ? null : json.get("status_code"));
            throw new UnavailableException();
        }
        return json;
    }

    // PublicApiClient 와 같은 방식: 디코딩키는 URI 변수로 넣어 + / = 를 확실히 인코딩
    private URI uri(String url) {
        try {
            if (serviceKey.contains("%")) {
                // 인코딩키: 이미 인코딩된 값 → 다시 인코딩하면 %25 로 깨지므로 그대로 붙임
                return URI.create(url + "?serviceKey=" + serviceKey + "&returnType=JSON");
            }
            return UriComponentsBuilder.fromUriString(url)
                    .queryParam("serviceKey", "{key}")
                    .queryParam("returnType", "JSON")
                    .encode()
                    .buildAndExpand(serviceKey)
                    .toUri();
        } catch (IllegalArgumentException e) {
            // URI.create 의 오류 메시지에는 키가 그대로 들어가므로 기록하지 않음
            log.warn("[국세청 API] OWNER_VALIDATE 서비스 키 형식이 올바르지 않습니다.");
            throw new UnavailableException();
        }
    }

    // {"data":[{...}]} 의 첫 번째 항목
    private Map<String, Object> firstItem(Map<String, Object> json) {
        Object data = json.get("data");
        if (data instanceof List<?> list && !list.isEmpty()) {
            return asMap(list.get(0));
        }
        log.warn("[국세청 API] 응답에 data 가 없습니다.");
        throw new UnavailableException();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return (value instanceof Map) ? (Map<String, Object>) value : Map.of();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    // ── 결과 ────────────────────────────────────────────────────
    // matched     : 사업자번호 · 개업일자 · 대표자 성명이 모두 일치하는지
    // statusCode  : "01" 계속사업자, "02" 휴업자, "03" 폐업자
    // statusName  : 예) "계속사업자"
    // taxType     : 예) "부가가치세 일반과세자"
    public record Result(boolean matched, String statusCode, String statusName, String taxType) {
        static Result notMatched() {
            return new Result(false, "", "", "");
        }
    }

    // 국세청 확인을 할 수 없음 (일치하지 않는다는 뜻이 아님) → 컨트롤러가 503 으로 응답
    public static class UnavailableException extends RuntimeException {
        public UnavailableException() {
            super("국세청 사업자 확인 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
