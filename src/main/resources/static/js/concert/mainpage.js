document.addEventListener("DOMContentLoaded", function() {
    // 1. data-store에서 로그인 정보 가져오기
    const dataStore = document.getElementById("data-store");
    const isLoggedIn = dataStore ? (dataStore.dataset.isLoggedIn === 'true') : false;

    function loadRecommendations() {
        const listDiv = document.getElementById('recommended-list');
        const section = document.querySelector('.recommendation-section');

        if (!listDiv) return;

        const fetchUrl = isLoggedIn ? '/concert/ai-recommend' : '/concert/recommended';
        const dataKey = isLoggedIn ? 'aiRecommended' : 'recommended';

        fetch(fetchUrl)
            .then(response => response.json())
            .then(data => {
                const concerts = data[dataKey] || [];
                if (concerts.length > 0) {
                    listDiv.innerHTML = "";
                    concerts.slice(0, 3).forEach(concert => {
                        const dateText = `${concert.startDate} ~ ${concert.endDate}`;
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
                            </div>
                        `;
                        listDiv.innerHTML += card;
                    });
                } else {
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