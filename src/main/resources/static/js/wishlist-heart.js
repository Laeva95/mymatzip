/*
 * ============================================================
 * [wishlist-heart.js] 맛집 카드 썸네일의 하트(찜) 버튼 (D 찜 담당)
 * ============================================================
 * 사용 화면: restaurant/list.html (맛집 목록 카드 + 검색 결과 카드)
 * 새 API 없이 기존 WishlistApiController 를 그대로 사용한다.
 *   GET    /api/wishlists                 내 찜 목록 → 처음 화면의 하트 상태
 *   POST   /api/wishlists/{restaurantId}  찜하기
 *   DELETE /api/wishlists/{restaurantId}  찜 취소
 *
 * 사용법
 *   1) 카드(.card) 안에 wishHeartButtonHtml(id) 버튼을 넣는다 (서버 렌더링 카드는 같은 마크업을 직접 씀)
 *   2) WishHeart.init({ loggedIn: true/false }) 를 한 번 부른다
 *   3) 검색 등으로 카드를 다시 그린 뒤에는 WishHeart.refresh() 로 하트 상태를 다시 칠한다
 *
 * 규칙
 *   - API 가 성공했을 때만 화면 상태를 바꾼다 (실패하면 그대로 두고 안내)
 *   - 요청 중에는 그 버튼을 잠가 중복 클릭을 막는다
 *   - 비로그인(또는 세션 만료) 이면 로그인 화면 이동을 안내한다
 * ============================================================
 */

/* 하트 버튼 HTML. 카드(.card, position:relative)의 오른쪽 위에 겹쳐진다. */
function wishHeartButtonHtml(restaurantId) {
    return '<button type="button" class="btn btn-light btn-sm rounded-circle shadow-sm wish-heart"' +
        ' data-restaurant-id="' + Number(restaurantId) + '" data-wished="false"' +
        ' aria-pressed="false" aria-label="찜하기" title="찜하기"' +
        ' style="position:absolute;top:8px;right:8px;z-index:2;width:36px;height:36px;padding:0;">' +
        '<i class="bi bi-heart text-danger"></i></button>';
}

const WishHeart = (function () {
    const LOGIN_URL = '/members/login';
    let loggedIn = false;
    let wishedIds = new Set();   // 내가 찜한 맛집 번호 (처음 GET 결과 + 클릭 결과로 갱신)

    /* 버튼 하나의 모양을 찜 상태에 맞게 바꾼다 */
    function paint(btn, wished) {
        btn.setAttribute('data-wished', String(wished));
        btn.setAttribute('aria-pressed', String(wished));
        const label = wished ? '찜 취소' : '찜하기';
        btn.setAttribute('aria-label', label);
        btn.setAttribute('title', label);
        const icon = btn.querySelector('i');
        if (icon) icon.className = 'bi text-danger ' + (wished ? 'bi-heart-fill' : 'bi-heart');
    }

    /* 같은 맛집의 하트가 여러 개여도 모두 같은 상태로 */
    function setState(restaurantId, wished) {
        if (wished) wishedIds.add(restaurantId); else wishedIds.delete(restaurantId);
        document.querySelectorAll('.wish-heart[data-restaurant-id="' + restaurantId + '"]')
            .forEach(function (b) { paint(b, wished); });
    }

    /* 화면의 모든 하트를 wishedIds 기준으로 다시 칠한다 */
    function refresh() {
        document.querySelectorAll('.wish-heart').forEach(function (b) {
            paint(b, wishedIds.has(Number(b.getAttribute('data-restaurant-id'))));
        });
    }

    function askLogin() {
        if (confirm('로그인이 필요합니다. 로그인 화면으로 이동할까요?')) {
            window.location.href = LOGIN_URL;
        }
    }

    /* fetch 결과를 { ok, status, body, needLogin } 로.
       세션이 끊기면 Spring Security 가 로그인 화면으로 redirect 해서 200 HTML 이 오므로 그것도 needLogin 으로 본다. */
    function request(url, method) {
        return fetch(url, { method: method, headers: { 'Accept': 'application/json' } }).then(function (res) {
            const needLogin = res.status === 401 ||
                (res.redirected && res.url.indexOf(LOGIN_URL) !== -1);
            return res.text().then(function (text) {
                let body = {};
                try { body = text ? JSON.parse(text) : {}; } catch (e) { body = {}; }
                return { ok: res.ok && !needLogin, status: res.status, body: body, needLogin: needLogin };
            });
        });
    }

    function toggle(btn) {
        if (btn.getAttribute('data-busy') === 'true') return;   // 중복 클릭 방지
        if (!loggedIn) { askLogin(); return; }

        const restaurantId = Number(btn.getAttribute('data-restaurant-id'));
        const wished = btn.getAttribute('data-wished') === 'true';
        btn.setAttribute('data-busy', 'true');
        btn.disabled = true;

        request('/api/wishlists/' + restaurantId, wished ? 'DELETE' : 'POST').then(function (r) {
            if (r.needLogin) { askLogin(); return; }
            if (r.ok) { setState(restaurantId, !wished); return; }
            // 서버 상태가 이미 원하는 상태인 경우 → 화면만 서버에 맞춘다
            if (!wished && r.status === 409) { setState(restaurantId, true); return; }   // 이미 찜한 맛집
            if (wished && r.status === 404) { setState(restaurantId, false); return; }   // 찜 정보 없음(이미 취소됨)
            alert((r.body && r.body.error) || '찜 처리에 실패했습니다.');
        }).catch(function () {
            alert('서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.');
        }).finally(function () {
            btn.removeAttribute('data-busy');
            btn.disabled = false;
        });
    }

    /* 처음 한 번: 클릭 위임 등록 + 내 찜 목록으로 하트 상태 반영 */
    function init(options) {
        loggedIn = !!(options && options.loggedIn);
        document.addEventListener('click', function (e) {
            const btn = e.target.closest('.wish-heart');
            if (!btn) return;
            e.preventDefault();
            e.stopPropagation();
            toggle(btn);
        });
        if (!loggedIn) return;

        // 상태를 불러오는 동안은 잘못된 요청(찜한 맛집에 POST 등)을 막기 위해 잠가 둔다
        document.querySelectorAll('.wish-heart').forEach(function (b) { b.disabled = true; });
        request('/api/wishlists', 'GET').then(function (r) {
            if (r.ok && Array.isArray(r.body)) {
                wishedIds = new Set(r.body
                    .map(function (w) { return w.restaurant ? Number(w.restaurant.id) : NaN; })
                    .filter(function (id) { return !isNaN(id); }));
            }
        }).catch(function () {
            /* 목록을 못 불러오면 빈 하트로 둔다. 클릭 시 409/404 응답으로 실제 상태에 맞춰진다 */
        }).finally(function () {
            refresh();
            document.querySelectorAll('.wish-heart').forEach(function (b) {
                if (b.getAttribute('data-busy') !== 'true') b.disabled = false;
            });
        });
    }

    return { init: init, refresh: refresh };
})();
