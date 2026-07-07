document.addEventListener("DOMContentLoaded", function() {
    const dataStore = document.getElementById("data-store");
    const isLoggedIn = dataStore ? (dataStore.dataset.isLoggedIn === 'true') : false;

    function loadRecommendations() {
        const listDiv = document.getElementById('recommended-list');
        const section = document.querySelector('.recommendation-section');

        if (!listDiv) return;

        // [수정 핵심]
        // 1. 위시리스트가 없거나 비로그인이면 모두 '/concert/recommended'를 호출합니다.
        // 2. 서버 컨트롤러에서 'isLoggedIn' 파라미터를 넘겨주거나,
        //    위시리스트 여부를 체크해서 없으면 자동으로 기본 추천을 내보내게 하세요.
        const fetchUrl = '/concert/recommended';
        const dataKey = 'recommended';

        fetch(fetchUrl)
            .then(response => response.json())
            .then(data => {
                // 서버로부터 받은 데이터가 있으면 사용, 없으면 빈 배열
                const concerts = data[dataKey] || [];

                if (concerts.length > 0) {
                    listDiv.innerHTML = "";
                    concerts.slice(0, 3).forEach(concert => {
                        const dateText = `${concert.startDate} ~ ${concert.endDate}`;
                        // HTML 생성 부분은 그대로 유지
                        const card = `
    <div class="small-card">
        <a href="/concert/${concert.concertId}/detail-page" style="text-decoration: none; color: inherit; display: block;">
            <img src="${concert.posterUrl}" alt="포스터">
            <p class="concert-name">${concert.concertName}</p>
            <div class="concert-meta">
                <p>${dateText}</p>
                <p>${concert.hallName}</p>
            </div>
        </a>
    </div>`;
                        listDiv.innerHTML += card;
                    });
                } else {
                    // 데이터가 하나도 없을 때만 섹션을 숨깁니다.
                    if (section) section.style.display = 'none';
                }
            })
            .catch(err => {
                console.error("추천 로딩 실패:", err);
                if (section) section.style.display = 'none';
            });
    }

    loadRecommendations();
});

// 필터 버튼 로직은 별도 데이터 의존성이 없으므로 그대로 유지
function filterConcerts(type, btn) {
    document.querySelectorAll('.btn-filter').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const upcoming = document.getElementById('upcoming-list');
    const past = document.getElementById('past-list');
    if (upcoming && past) {
        if (type === 'upcoming') {
            upcoming.style.display = 'grid';
            past.style.display = 'none';
        } else {
            upcoming.style.display = 'none';
            past.style.display = 'grid';
        }
    }
}