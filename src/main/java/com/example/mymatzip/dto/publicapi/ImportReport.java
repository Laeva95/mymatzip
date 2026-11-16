package com.example.mymatzip.dto.publicapi;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// ============================================================
// [ImportReport] 공공 API 조사(preview) · 저장(import) 결과 요약
// ============================================================
// 관리자 API 응답(JSON)으로 그대로 나갑니다. 같은 내용이 콘솔과 CSV 파일에도 남습니다.
//
// lines         : 성공을 뺀 모든 기록 ("[중복] 출처 | 업체명 | 주소 | 이유")
//                 preview에서는 분류 결과([분류])도 함께 담습니다.
// rawCategoryCounts : 모범음식점 bsnsCond 값별 건수 (매핑표 확정용)
// categoryCounts    : 우리 Category별 건수
// checks            : 데이터 점검 (좌표 없음, 전화 없음, 이미지 없음 등)
// ============================================================
@Getter
public class ImportReport {

    private final String source;          // 출처 한글 이름
    private final String mode;            // "preview" 또는 "import"
    private int fetched;                  // API에서 받은 건수
    private int success;                  // 저장 성공 (preview는 저장 예정)
    private int duplicate;                // 중복
    private int excluded;                 // 제외 (휴업 등)
    private int failed;                   // 실패
    private int unmapped;                 // 매핑없음 (ETC로 저장)
    private int confirmNeeded;            // [추가]2026.11.11권성애 (v3) 확인필요 (저장은 함, 관리자 확인용)
    private String resultFile;            // CSV 파일 경로
    private final List<String> lines = new ArrayList<>();
    private final Map<String, Integer> rawCategoryCounts = new LinkedHashMap<>();
    private final Map<String, Integer> categoryCounts = new LinkedHashMap<>();
    private final Map<String, Integer> checks = new LinkedHashMap<>();

    public ImportReport(String source, String mode) {
        this.source = source;
        this.mode = mode;
    }

    public void setFetched(int fetched) { this.fetched = fetched; }
    public void setResultFile(String resultFile) { this.resultFile = resultFile; }

    public void addSuccess() { success++; }
    public void addDuplicate() { duplicate++; }
    public void addExcluded() { excluded++; }
    public void addFailed() { failed++; }
    public void addUnmapped() { unmapped++; }
    public void addConfirmNeeded() { confirmNeeded++; }   // [추가]2026.11.11권성애 (v3)

    public void addLine(String line) { lines.add(line); }

    public void countRawCategory(String value) {
        rawCategoryCounts.merge(value == null ? "(없음)" : value, 1, Integer::sum);
    }

    public void countCategory(String value) {
        categoryCounts.merge(value, 1, Integer::sum);
    }

    public void check(String name) {
        checks.merge(name, 1, Integer::sum);
    }

    // [요약] 한 줄
    public String summary() {
        return "[요약] " + source + " | " + mode + " | 전체 " + fetched
                + " | 성공 " + success + " | 중복 " + duplicate + " | 제외 " + excluded
                + " | 실패 " + failed + " | 매핑없음 " + unmapped
                + " | 확인필요 " + confirmNeeded;   // [추가]2026.11.11권성애 (v3)
    }
}
