/*
 * ============================================================
 * [common.js] 모든 화면이 함께 쓰는 자바스크립트 함수 (팀장 담당)
 * ============================================================
 * fragments/layout.html 의 footer 조각이 이 파일을 불러오므로 모든 화면에서 사용할 수 있다.
 * 서버에서 받은 글(맛집 이름, 리뷰, AI 답 등)을 innerHTML 로 넣을 때는 반드시 esc() 를 거친다.
 * (DB 에는 원문이 저장되어 있으므로, 출력할 때 이스케이프하지 않으면 XSS 가 발생한다)
 *
 * 함수 목록
 *   esc(value)                         HTML 이스케이프
 *   renderStars(rating, withNumber)    별점 아이콘 (0.5 단위 반올림, 반쪽 별)
 *   markActiveNav()                    상단 메뉴 현재 위치 강조 (layout.html footer 가 호출)
 *   formatDateTime(value)              "2026-10-28T14:30:00" → "2026-10-28 14:30"
 *   formatBusinessNumber(value)        "1234567891" → "123-45-67891" (사장 신청 화면)
 *   formatPhone(value)                 "01012345678" → "010-1234-5678" (사장 신청 화면)
 *   restaurantCardHtml(r, extra, opts) 맛집 카드 HTML (list.html 검색 결과, home.html AI 추천)
 *   renderPagination(...)              페이지 번호 버튼 (mypage, 관리자 목록 화면)
 *   fetchJson(url, options)            fetch 결과를 { ok, status, body } 로 (세션 만료도 401 로)
 * 함수를 고칠 때는 이 함수를 부르는 화면을 모두 검색해서 영향이 없는지 확인한다 (여러 팀원 화면이 함께 씀)
 * ============================================================
 */

/* HTML 특수문자 5개를 문자 참조로 바꾼다. 예: <script> → &lt;script&gt; */
function esc(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/* 별점(0~5)을 별 아이콘 5개 HTML 로 만든다. withNumber=true 이면 (4.3) 숫자도 붙인다.
   평균 별점은 0.5 단위로 반올림해서 그린다 (4.2 → ★★★★☆, 4.3 → ★★★★½, 4.8 → ★★★★★)
     i 번째 별: rating ≥ i - 0.25 이면 꽉 찬 별, rating ≥ i - 0.75 이면 반쪽 별, 아니면 빈 별
   리뷰 1개의 별점처럼 정수(1~5)면 예전과 똑같이 그려진다. home.html 의 타임리프 별점과 같은 규칙 */
function renderStars(rating, withNumber) {
    const value = Number(rating) || 0;
    let html = '';
    for (let i = 1; i <= 5; i++) {
        const icon = value >= i - 0.25 ? 'bi-star-fill' : (value >= i - 0.75 ? 'bi-star-half' : 'bi-star');
        html += '<i class="bi ' + icon + '"></i>';
    }
    if (withNumber) {
        html += ' <span class="text-muted small">(' + esc(rating) + ')</span>';
    }
    return html;
}

/* 상단 메뉴(layout.html header)에서 지금 주소에 해당하는 메뉴에 active 표시
   가장 길게 맞는 메뉴 하나만 고름: /restaurants/new → "맛집 등록" (더 짧은 /restaurants "맛집 목록"이 아님)
                                  /restaurants/3   → "맛집 목록",  /admin/members → "관리자"
   로그아웃 링크는 화면이 아니므로 제외. layout.html footer 조각이 페이지마다 한 번 부름 */
function markActiveNav() {
    const path = location.pathname;
    let best = null;
    let bestLength = 0;
    document.querySelectorAll('#navbarNav a.nav-link').forEach(function (link) {
        const href = link.getAttribute('href');
        if (!href || href.indexOf('/logout') >= 0) return;
        const matched = path === href || path.indexOf(href.replace(/\/$/, '') + '/') === 0;
        if (matched && href.length > bestLength) {
            best = link;
            bestLength = href.length;
        }
    });
    if (best) {
        best.classList.add('active');
        best.setAttribute('aria-current', 'page');
    }
}

/* 2026-10-28T14:30:00 형식을 2026-10-28 14:30 으로 줄인다. */
function formatDateTime(value) {
    if (!value) return '';
    return String(value).replace('T', ' ').substring(0, 16);
}

/* 사업자등록번호를 화면에 보여줄 모양으로: "1234567891" → "123-45-67891"
   DB 에는 숫자만 저장 (OwnerApplicationService.digitsOnly). 10자리가 아니면 받은 값 그대로 */
function formatBusinessNumber(value) {
    if (!value) return '';
    const d = String(value).replace(/\D/g, '');
    return d.length === 10 ? d.substring(0, 3) + '-' + d.substring(3, 5) + '-' + d.substring(5) : String(value);
}

/* 전화번호를 화면에 보여줄 모양으로 (DB 에는 숫자만 저장)
   15881234 → 1588-1234 (대표번호)        021234567 / 0212345678 → 02-123-4567 / 02-1234-5678 (서울)
   0511234567 → 051-123-4567              01012345678 → 010-1234-5678
   050712345678 → 0507-1234-5678 (안심번호)  규칙에 맞지 않으면 받은 값 그대로 */
function formatPhone(value) {
    if (!value) return '';
    const d = String(value).replace(/\D/g, '');
    let m;
    if ((m = d.match(/^(1[5-9]\d{2})(\d{4})$/))) return m[1] + '-' + m[2];
    if ((m = d.match(/^(02)(\d{3,4})(\d{4})$/))) return m[1] + '-' + m[2] + '-' + m[3];
    if ((m = d.match(/^(0\d{3})(\d{4})(\d{4})$/))) return m[1] + '-' + m[2] + '-' + m[3];   // 12자리: 0507 등
    if ((m = d.match(/^(0\d{2})(\d{3,4})(\d{4})$/))) return m[1] + '-' + m[2] + '-' + m[3];  // 10 · 11자리
    return String(value);
}

/* 맛집 카드 1개 HTML. list.html 검색 결과, home.html AI 추천에서 사용
   withDefaultImage (선택, 기본 false)
     true  : 사진이 없거나 깨지면 카테고리 기본 이미지(r.defaultImageUrl) — home.html 처음 화면 카드와 같게
     false : 예전 그대로 (사진이 없으면 사진 칸을 만들지 않음)
   ※ list.html 은 restaurantCardHtmlAddBasicImage() 로 기본 이미지를 직접 끼워 넣으므로 false 로 부른다.
     (true 로 부르면 기본 이미지가 두 번 들어감. 그 함수를 없앨 때 true 로 바꾸면 됨)
   [추가]2026.11.24 세 번째 값에 객체도 받음: { defaultImage: true, counts: true }
     counts: true : 별점 옆에 리뷰 수 · 찜 수 (r.reviewCount · r.wishlistCount, 없으면 0) — home.html AI 추천
     ※ list.html 검색 결과는 restaurantCardHtmlAddBasicImage() 가 개수를 직접 끼워 넣으므로 켜지 않는다 (두 번 들어감)
     wishHeart: true : 썸네일 오른쪽 위 하트(찜) 버튼 — wishlist-heart.js 의 wishHeartButtonHtml() (D 찜 담당)
       그 스크립트를 불러온 화면에서만 들어감. 버튼을 그린 뒤 WishHeart.refresh() 로 찜 상태를 칠해야 함
       ※ list.html 검색 결과는 하트를 직접 끼워 넣으므로 켜지 않는다 (두 번 들어감)
     true / false 만 넘기는 예전 방식도 그대로 동작 (= { defaultImage: true / false }) */
function restaurantCardHtml(r, extraHtml, options) {
    const opts = (options && typeof options === 'object') ? options : { defaultImage: !!options };
    const withDefaultImage = !!opts.defaultImage;
    // 리뷰 수 · 찜 수 (home.html 최근 맛집 카드, list.html 카드와 같은 모양 · 같은 class)
    const countsHtml = opts.counts
        ? ' <span class="card-counts"><i class="bi bi-chat"></i> ' + (Number(r.reviewCount) || 0) +
          ' · <i class="bi bi-heart"></i> ' + (Number(r.wishlistCount) || 0) + '</span>'
        : '';
    const heartHtml = (opts.wishHeart && typeof wishHeartButtonHtml === 'function')
        ? wishHeartButtonHtml(r.id)
        : '';
    let imgHtml = '';
    if (withDefaultImage && (r.imageUrl || r.defaultImageUrl)) {
        const fallback = r.defaultImageUrl
            ? ' data-default="' + esc(r.defaultImageUrl) + '" onerror="this.onerror=null;this.src=this.dataset.default;"'
            : '';
        imgHtml = '<img src="' + esc(r.imageUrl || r.defaultImageUrl) + '"' + fallback +
            ' class="card-img-top" alt="" style="height:180px;object-fit:cover;">';
    } else if (r.imageUrl) {
        imgHtml = '<img src="' + esc(r.imageUrl) + '" class="card-img-top" alt="" style="height:180px;object-fit:cover;">';
    }
    return '<div class="col-md-6 col-lg-4">' +
        '<div class="card h-100">' +
        imgHtml +
        heartHtml +
        '<div class="card-body">' +
        '<div class="d-flex justify-content-between align-items-start mb-2">' +
        '<h5 class="card-title mb-0"><a href="/restaurants/' + Number(r.id) + '" class="text-decoration-none text-dark">' + esc(r.name) + '</a></h5>' +
        '<span class="badge bg-primary">' + esc(r.categoryDisplayName) + '</span>' +
        '</div>' +
        '<p class="card-text text-muted small mb-2"><i class="bi bi-geo-alt"></i> ' + esc(r.address) + '</p>' +
        (r.description ? '<p class="card-text small">' + esc(r.description) + '</p>' : '') +
        '<div class="d-flex justify-content-between align-items-center">' +
        '<div class="rating-stars">' + renderStars(r.averageRating, true) + countsHtml + '</div>' +
        '<small class="text-muted">by ' + esc(r.memberNickname) + '</small>' +
        '</div>' +
        (extraHtml || '') +
        '</div></div></div>';
}

/* 페이지 번호 버튼(«, 1, 2, 3, ..., »)을 만든다.
   전체 페이지 번호를 나열하지 않고 현재 페이지 주변 최대 5개만 표시한다.
   첫 페이지와 마지막 페이지는 필요한 경우 별도로 표시한다.
   버튼 클릭 시 loadFunction(페이지번호)을 호출한다.
   사용 화면: mypage, admin/members, admin/restaurants */

function renderPagination(navId, totalPages, currentPage, loadFunction) {
    const ul = document.getElementById(navId).querySelector('ul');

    if (totalPages <= 1) {
        ul.innerHTML = '';
        return;
    }

    // 한 구간에 표시할 페이지 번호 개수
    const pageGroupSize = 10;

    // 현재 페이지가 속한 구간의 시작과 끝
    const startPage = Math.floor(currentPage / pageGroupSize) * pageGroupSize;
    const endPage = Math.min(startPage + pageGroupSize, totalPages);

    let html = '';

    // 이전 구간 버튼
    const prevGroupPage = startPage - 1;
    html += '<li class="page-item ' + (startPage === 0 ? 'disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + prevGroupPage + '">&laquo;</a></li>';

    // 페이지 번호 10개씩 표시
    for (let i = startPage; i < endPage; i++) {
        html += '<li class="page-item ' + (i === currentPage ? 'active' : '') + '">' +
            '<a class="page-link" href="#" data-page="' + i + '">' + (i + 1) + '</a></li>';
    }

    // 다음 구간 버튼
    const nextGroupPage = endPage;
    html += '<li class="page-item ' + (endPage >= totalPages ? 'disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + nextGroupPage + '">&raquo;</a></li>';

    ul.innerHTML = html;

    ul.querySelectorAll('a.page-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
            e.preventDefault();

            const page = parseInt(this.getAttribute('data-page'));

            if (page >= 0 && page < totalPages) {
                loadFunction(page);
            }
        });
    });
}


/* fetch 결과를 { ok, status, body } 로 묶어 돌려준다. 응답이 JSON 이 아니어도 오류 없이 처리
   세션이 끊긴 상태로 API 를 부르면 Spring Security 가 /members/login 으로 redirect 하고,
   fetch 는 그 로그인 화면 HTML 을 200 으로 받는다. 이걸 성공으로 착각하지 않도록
   { ok: false, status: 401 } 로 바꿔서 돌려준다. (wishlist-heart.js 의 request() 와 같은 방식) */
function fetchJson(url, options) {
    return fetch(url, options).then(function (res) {
        if (res.redirected && new URL(res.url).pathname === '/members/login') {
            return { ok: false, status: 401, body: { error: '로그인이 만료되었습니다. 다시 로그인해주세요.' } };
        }
        return res.text().then(function (text) {
            let body = {};
            try { body = text ? JSON.parse(text) : {}; } catch (e) { body = {}; }
            return { ok: res.ok, status: res.status, body: body };
        });
    });
}