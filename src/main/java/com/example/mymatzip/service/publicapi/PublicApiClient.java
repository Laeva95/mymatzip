package com.example.mymatzip.service.publicapi;

import com.example.mymatzip.entity.Source;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;              // [추가]2026.11.11권성애 응답 헤더 묶음 (Content-Type 등)
import org.springframework.http.HttpStatusCode;           // [추가]2026.11.11권성애 응답 상태 코드 (200, 404 ...)
import org.springframework.http.MediaType;                // [추가]2026.11.11권성애 text/plain, application/json 같은 형식 이름
import org.springframework.http.client.ClientHttpResponse; // [추가]2026.11.11권성애 RestClient가 받은 응답 1건
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;                               // [추가]2026.11.11권성애 응답 읽기 실패 예외
import java.io.InputStream;                               // [추가]2026.11.11권성애 응답 내용(바이트 흐름)
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// ============================================================
// [PublicApiClient] 부산 공공 API 호출 (부산맛집 · 모범음식점)
// ============================================================
// 역할: API를 페이지 단위로 호출해서 item 목록(Map)을 모두 모아 돌려줍니다.
//       응답 JSON은 Map으로 받습니다 (Jackson 클래스를 직접 쓰지 않아 버전 영향이 없음).
//
// 서비스 키: developer.properties 의 publicdata.servicekey (디코딩키)
//   - 키가 없어도 앱은 실행되도록 기본값을 빈 문자열로 둡니다.
//   - 디코딩키를 쓰는 이유: 아래 encode() + URI 변수 방식이 키의 +, /, = 를 %2B 등으로 한 번 인코딩합니다.
//     인코딩키를 넣으면 이중 인코딩 → SERVICE_KEY_IS_NOT_REGISTERED_ERROR
//
// 응답 모양 (resultType=json)
//   부산맛집  : {"getFoodKr":{"header":{"code":"00"},"item":[...],"totalCount":536}}
//   모범음식점: {"response":{"header":{"resultCode":"00"},"body":{"totalCount":"268","items":{"item":[...]}}}}
//   ※ 결과가 1건이면 item이 배열이 아니라 객체 하나로 오는 경우가 있어 둘 다 처리합니다.
//   ※ 키 오류 등은 JSON이 아니라 XML로 오는 경우가 있어 읽기 실패를 안내 메시지로 바꿉니다.
//   [추가]2026.11.11권성애 부산맛집은 내용은 JSON인데 Content-Type을 text/plain으로 보냄
//     → 인터셉터가 text/plain을 application/json으로 바꿔 기존 JSON 변환기가 읽게 함 (JsonTypeResponse)
// ============================================================
@Slf4j
@Component
public class PublicApiClient {

    private static final String FOOD_URL = "https://apis.data.go.kr/6260000/FoodService/getFoodKr";
    private static final String MODEL_URL = "https://apis.data.go.kr/6260000/BusanTblFnrstrnStusService/getTblFnrstrnStusInfo";
    private static final int PAGE_SIZE = 100;     // 한 번에 받을 건수
    private static final int MAX_PAGES = 50;      // 무한 반복 방지 (100 x 50 = 5,000건)

    private final RestClient restClient;
    private final String serviceKey;

    public PublicApiClient(@Value("${publicdata.servicekey:}") String serviceKey) {
        this.serviceKey = serviceKey;
        // 연결 5초, 응답 20초 제한 — 공공 API가 느릴 때 요청이 무한정 기다리지 않게
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .requestFactory(factory)                          // 연결 5초 · 응답 20초 제한
                // [추가]2026.11.11권성애 부산맛집 API는 JSON을 text/plain으로 보내서 Map 변환 실패
                //   → 응답 헤더가 text/plain이면 application/json으로 바꿔 읽게 함
                .requestInterceptor((request, body, execution) -> {           // 요청을 보낼 때마다 이 코드를 거침
                    ClientHttpResponse response = execution.execute(request, body); // 실제로 공공 API에 요청 → 응답 받기
                    MediaType type = response.getHeaders().getContentType();     // 응답 헤더의 형식
                    if (type != null && MediaType.TEXT_PLAIN.isCompatibleWith(type)) { // text/plain이면 (charset 무시)
                        return new JsonTypeResponse(response);                    // 헤더만 바꾼 응답
                    }
                    return response;                                              // 그 밖은 그대로
                })
                .build();
    }

    /**
     * 전체(또는 limit 건) item 목록을 받아 온다.
     * @param limit 0이면 전체, 1 이상이면 그 건수까지만 (처음 테스트용: 10)
     */
    public List<Map<String, Object>> fetchAll(Source source, int limit) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("공공 API 키가 설정되지 않았습니다. developer.properties에 publicdata.servicekey(디코딩키)를 넣어 주세요.");
        }
        int pageSize = (limit > 0) ? Math.min(limit, PAGE_SIZE) : PAGE_SIZE;

        List<Map<String, Object>> all = new ArrayList<>();
        PageResult first = callPage(source, 1, pageSize);
        all.addAll(first.items());
        int total = first.totalCount();
        log.info("[공공API] {} totalCount={} (1페이지 {}건)", source.getDisplayName(), total, first.items().size());

        int page = 2;
        while (all.size() < total && (limit <= 0 || all.size() < limit) && page <= MAX_PAGES) {
            PageResult next = callPage(source, page, pageSize);
            if (next.items().isEmpty()) {
                break;   // 더 받을 게 없으면 종료
            }
            all.addAll(next.items());
            page++;
        }

        if (limit > 0 && all.size() > limit) {
            return new ArrayList<>(all.subList(0, limit));
        }
        return all;
    }

    // ── 한 페이지 호출 ─────────────────────────────────────────
    private PageResult callPage(Source source, int pageNo, int numOfRows) {
        String baseUrl = (source == Source.PUBLIC_API_BUSAN_FOOD) ? FOOD_URL : MODEL_URL;

        // serviceKey를 URI 변수({key})로 넣어야 + / = 가 확실히 인코딩됩니다.
        // (queryParam에 값을 바로 넣으면 + 가 그대로 남아 서버에서 공백으로 바뀜)
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("serviceKey", "{key}")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("resultType", "json")
                .encode()
                .buildAndExpand(serviceKey)
                .toUri();

        Map<String, Object> json;
        try {
            json = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException e) {
            throw new IllegalStateException("공공 API 응답을 읽지 못했습니다 (" + source.getDisplayName() + ", " + pageNo
                    + "페이지). 서비스 키(디코딩키)와 활용 신청 승인 상태를 확인해 주세요. 원인: " + e.getMessage(), e);
        }
        if (json == null) {
            throw new IllegalStateException("공공 API 응답이 비어 있습니다: " + source.getDisplayName());
        }
        return (source == Source.PUBLIC_API_BUSAN_FOOD) ? parseFood(json) : parseModel(json);
    }

    // 부산맛집: {"getFoodKr":{"header":{"code","message"},"item":[...],"totalCount":536}}
    private PageResult parseFood(Map<String, Object> json) {
        Map<String, Object> root = asMap(json.get("getFoodKr"));
        Map<String, Object> header = asMap(root.get("header"));
        String code = String.valueOf(header.get("code"));
        if (!"00".equals(code)) {
            throw new IllegalStateException("부산맛집 API 오류: code=" + code + ", message=" + header.get("message"));
        }
        return new PageResult(toInt(root.get("totalCount")), toItemList(root.get("item")));
    }

    // 모범음식점: {"response":{"header":{"resultCode","resultMsg"},"body":{"totalCount":"268","items":{"item":[...]}}}}
    private PageResult parseModel(Map<String, Object> json) {
        Map<String, Object> response = asMap(json.get("response"));
        Map<String, Object> header = asMap(response.get("header"));
        String code = String.valueOf(header.get("resultCode"));
        if (!"00".equals(code)) {
            throw new IllegalStateException("모범음식점 API 오류: resultCode=" + code + ", resultMsg=" + header.get("resultMsg"));
        }
        Map<String, Object> body = asMap(response.get("body"));
        Map<String, Object> items = asMap(body.get("items"));   // 결과가 없으면 "" 로 올 수 있음 → 빈 Map
        return new PageResult(toInt(body.get("totalCount")), toItemList(items.get("item")));
    }

    // ── 변환 도우미 ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        return (value instanceof Map) ? (Map<String, Object>) value : Map.of();
    }

    // item이 배열이면 그대로, 객체 하나면 목록 1개로
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toItemList(Object value) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (value instanceof List<?> raw) {
            for (Object o : raw) {
                if (o instanceof Map) {
                    list.add((Map<String, Object>) o);
                }
            }
        } else if (value instanceof Map) {
            list.add((Map<String, Object>) value);
        }
        return list;
    }

    // totalCount가 숫자(536)로도, 문자열("268")로도 옴
    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
                // 아래에서 0 반환
            }
        }
        return 0;
    }

    private record PageResult(int totalCount, List<Map<String, Object>> items) {}

    // [추가]2026.11.11권성애 응답 내용은 그대로 두고 Content-Type 헤더만 application/json으로 바꾼 응답
    private static class JsonTypeResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;            // 원래 응답 (상태 · 내용은 여기서 꺼내 씀)
        private final HttpHeaders headers = new HttpHeaders(); // 바꾼 헤더

        JsonTypeResponse(ClientHttpResponse delegate) {
            this.delegate = delegate;
            this.headers.addAll(delegate.getHeaders());        // 원래 헤더 모두 복사
            this.headers.setContentType(MediaType.APPLICATION_JSON); // Content-Type만 덮어씀
        }

        @Override public HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public HttpHeaders getHeaders() { return headers; }
        @Override public InputStream getBody() throws IOException { return delegate.getBody(); }
        @Override public void close() { delegate.close(); }
    }
}
