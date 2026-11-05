/*
 * ============================================================
 * [common.js] 모든 화면이 함께 쓰는 자바스크립트 함수 (팀장 담당)
 * ============================================================
 * fragments/layout.html 의 footer 조각이 이 파일을 불러오므로 모든 화면에서 사용할 수 있다.
 * 서버에서 받은 글(맛집 이름, 리뷰, AI 답 등)을 innerHTML 로 넣을 때는 반드시 esc() 를 거친다.
 * (DB 에는 원문이 저장되어 있으므로, 출력할 때 이스케이프하지 않으면 XSS 가 발생한다)
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

/* 별점(0~5)을 별 아이콘 5개 HTML 로 만든다. withNumber=true 이면 (4.3) 숫자도 붙인다. */
function renderStars(rating, withNumber) {
    let html = '';
    for (let i = 1; i <= 5; i++) {
        html += '<i class="bi ' + (i <= rating ? 'bi-star-fill' : 'bi-star') + '"></i>';
    }
    if (withNumber) {
        html += ' <span class="text-muted small">(' + esc(rating) + ')</span>';
    }
    return html;
}

/* 2026-10-28T14:30:00 형식을 2026-10-28 14:30 으로 줄인다. */
function formatDateTime(value) {
    if (!value) return '';
    return String(value).replace('T', ' ').substring(0, 16);
}

/* 맛집 카드 1개 HTML. list.html 검색 결과, home.html AI 추천, wishlist 에서 사용 */
function restaurantCardHtml(r, extraHtml) {
    return '<div class="col-md-6 col-lg-4">' +
        '<div class="card h-100">' +
        (r.imageUrl ? '<img src="' + esc(r.imageUrl) + '" class="card-img-top" alt="" style="height:180px;object-fit:cover;">' : '') +
        '<div class="card-body">' +
        '<div class="d-flex justify-content-between align-items-start mb-2">' +
        '<h5 class="card-title mb-0"><a href="/restaurants/' + Number(r.id) + '" class="text-decoration-none text-dark">' + esc(r.name) + '</a></h5>' +
        '<span class="badge bg-primary">' + esc(r.categoryDisplayName) + '</span>' +
        '</div>' +
        '<p class="card-text text-muted small mb-2"><i class="bi bi-geo-alt"></i> ' + esc(r.address) + '</p>' +
        (r.description ? '<p class="card-text small">' + esc(r.description) + '</p>' : '') +
        '<div class="d-flex justify-content-between align-items-center">' +
        '<div class="rating-stars">' + renderStars(r.averageRating, true) + '</div>' +
        '<small class="text-muted">by ' + esc(r.memberNickname) + '</small>' +
        '</div>' +
        (extraHtml || '') +
        '</div></div></div>';
}

/* 페이지 번호 버튼(«, 1, 2, 3, ») 을 만들고, 누르면 loadFunction(페이지번호) 를 호출한다.
   mypage, admin/members, admin/restaurants 에서 사용 */
function renderPagination(navId, totalPages, currentPage, loadFunction) {
    const ul = document.getElementById(navId).querySelector('ul');
    if (totalPages <= 1) { ul.innerHTML = ''; return; }
    let html = '<li class="page-item ' + (currentPage === 0 ? 'disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + (currentPage - 1) + '">&laquo;</a></li>';
    for (let i = 0; i < totalPages; i++) {
        html += '<li class="page-item ' + (i === currentPage ? 'active' : '') + '">' +
            '<a class="page-link" href="#" data-page="' + i + '">' + (i + 1) + '</a></li>';
    }
    html += '<li class="page-item ' + (currentPage === totalPages - 1 ? 'disabled' : '') + '">' +
        '<a class="page-link" href="#" data-page="' + (currentPage + 1) + '">&raquo;</a></li>';
    ul.innerHTML = html;
    ul.querySelectorAll('a.page-link').forEach(function (link) {
        link.addEventListener('click', function (e) {
            e.preventDefault();
            const page = parseInt(this.getAttribute('data-page'));
            if (page >= 0 && page < totalPages) loadFunction(page);
        });
    });
}

/* fetch 결과를 { ok, status, body } 로 묶어 돌려준다. 응답이 JSON 이 아니어도 오류 없이 처리 */
function fetchJson(url, options) {
    return fetch(url, options).then(function (res) {
        return res.text().then(function (text) {
            let body = {};
            try { body = text ? JSON.parse(text) : {}; } catch (e) { body = {}; }
            return { ok: res.ok, status: res.status, body: body };
        });
    });
}