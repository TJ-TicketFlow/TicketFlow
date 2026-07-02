/**
 * 공연 상세 페이지 스크립트 (최종본 - 매진 처리 로직 포함)
 */
let genderChart, ageChart;

document.addEventListener('DOMContentLoaded', function() {
    const dataStore = document.getElementById("data-store");
    if (!dataStore) return;

    const concertId = dataStore.dataset.concertId;
    const startDate = dataStore.dataset.startDate;
    const originalEndDate = dataStore.dataset.endDate;
    const submitBtn = document.getElementById('submitBtn');

    // 1. 초기 상태: 예매 버튼 비활성화
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.style.opacity = "0.5";
        submitBtn.style.cursor = "not-allowed";
    }

    // 2. 예매 폼 제출 전 검증
    const bookingForm = document.getElementById('bookingForm');
    if (bookingForm) {
        bookingForm.addEventListener('submit', function(event) {
            const date = document.getElementById('selectedDate').value;
            const sessionId = document.getElementById('selectedSessionId').value;
            if (!date || !sessionId) {
                event.preventDefault();
                alert("관람일과 회차를 모두 선택해주세요.");
            }
        });
    }
    //⭐
    loadAiCancelRate(concertId);
    // 3. 통계 데이터 로드 및 폴링
    fetch(`/concert/${concertId}/stats-json`)
        .then(res => {
            if (!res.ok) throw new Error('Stats not found'); // 404 처리
            return res.json();
        })
        .then(data => {
            if (data && data.genderData && data.ageData) {
                initCharts(data);
                setInterval(pollStats, 3000);
            }
        })
        .catch(err => {
            console.warn("통계 데이터를 불러올 수 없습니다 (무시해도 되는 에러일 수 있음):", err);
        });

    // 4. FullCalendar 설정
    const calendarEl = document.querySelector('.concert-calendar');
    if (calendarEl) {
        fetch(`/concert/${concertId}/available-dates`)
            .then(res => res.json())
            .then(availableDates => {
                const calendar = new FullCalendar.Calendar(calendarEl, {
                    locale: 'ko',
                    initialView: 'dayGridMonth',
                    initialDate: startDate || new Date().toISOString().split('T')[0],
                    selectable: true,
                    height: 'auto',
                    aspectRatio: 1.8,
                    headerToolbar: {
                        left: 'prev,next',
                        center: 'title',
                        right: 'today'
                    },

                    // 달력 렌더링 시 스타일 및 클릭 제어
                    datesSet: function() {
                        document.querySelectorAll('.fc-daygrid-day').forEach(cell => {
                            const dateStr = cell.getAttribute('data-date');
                            if (!dateStr) return;

                            const isAvailable = availableDates.includes(dateStr);

                            if (isAvailable) {
                                cell.style.opacity = '1';
                                cell.style.backgroundColor = '#e1f5fe';
                                cell.style.pointerEvents = 'auto';
                                cell.style.cursor = 'pointer';
                            } else {
                                cell.style.opacity = '0.4';
                                cell.style.pointerEvents = 'none';
                                cell.style.backgroundColor = 'transparent';
                                cell.style.cursor = 'default';
                            }
                        });
                    },
                    dateClick: (info) => {
                        if (availableDates.includes(info.dateStr)) {
                            loadSessions(info.dateStr, concertId);
                        } else {
                            document.getElementById('session-container').innerHTML =
                                '<p style="padding:10px; color:#f43f5e; font-size: 0.9rem;">예매가 불가능한 날짜입니다.</p>';
                            document.getElementById('selectedDate').value = '';
                        }
                    }
                });
                calendar.render();
            });
    }
});

/**
 * 회차 정보 로드 함수 (매진 처리 포함)
 */
function loadSessions(date, concertId) {
    document.getElementById('selectedDate').value = date;
    document.getElementById('selectedSessionId').value = '';

    const container = document.getElementById('session-container');
    container.innerHTML = '';

    const submitBtn = document.getElementById('submitBtn');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.style.opacity = "0.5";
        submitBtn.style.cursor = "not-allowed";
    }

    fetch(`/concert/${concertId}/sessions?date=${date}`)
        .then(res => res.json())
        .then(data => {
            if (data.length === 0) {
                container.innerHTML = '<p style="padding:10px; color:#888;">선택 가능한 회차가 없습니다.</p>';
                return;
            }
            data.forEach(s => {
                const btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn-session';
                btn.innerText = s.time;

                // 매진 여부 확인 후 버튼 제어
                if (s.soldOut) {
                    btn.disabled = true;
                    btn.innerText += " (매진)";
                    btn.style.opacity = "0.4";
                    btn.style.cursor = "not-allowed";
                    btn.style.backgroundColor = "#e0e0e0";
                } else {
                    btn.onclick = () => {
                        document.querySelectorAll('.btn-session').forEach(b => b.classList.remove('active'));
                        btn.classList.add('active');
                        document.getElementById('selectedSessionId').value = s.id;
                        if (submitBtn) {
                            submitBtn.disabled = false;
                            submitBtn.style.opacity = "1";
                            submitBtn.style.cursor = "pointer";
                        }
                    };
                }
                container.appendChild(btn);
            });
        });
}
// ⭐ [새로 추가된 부분] AI 예측 취소율을 서버에서 가져와서 화면에 그리는 함수
function loadAiCancelRate(concertId) {
    const rateElement = document.getElementById("ai-cancel-rate");
    // HTML에 취소율 박스가 없다면 이 함수는 실행하지 않고 조용히 넘어갑니다.
    if (!rateElement) return;

    fetch(`/concert/${concertId}/cancel-rate`)
        .then(response => {
            if (!response.ok) {
                throw new Error("서버 응답 오류");
            }
            return response.text();
        })
        .then(rate => {
            const parsedRate = parseFloat(rate);

            // 취소율 수치에 따라 숫자의 색상을 변경합니다.
            if (parsedRate >= 30.0) {
                rateElement.style.color = "#e74c3c"; // 위험 (빨강)
            } else if (parsedRate >= 15.0) {
                rateElement.style.color = "#f39c12"; // 주의 (주황)
            } else {
                rateElement.style.color = "#2ecc71"; // 안전 (초록)
            }

            // 가져온 숫자 뒤에 %를 붙여서 화면에 보여줍니다.
            rateElement.innerText = rate + "%";
        })
        .catch(error => {
            console.warn("취소율 데이터를 불러오는 중 오류 발생:", error);
            rateElement.innerText = "확인 불가";
            rateElement.style.fontSize = "1rem";
            rateElement.style.color = "#999";
        });
}
/**
 * 기타 유틸리티 함수
 */
function initCharts(stats) {
    const genderCtx = document.getElementById('genderChart');
    const ageCtx = document.getElementById('ageChart');
    if (!genderCtx || !ageCtx) return;
    genderChart = new Chart(genderCtx, { type: 'doughnut', data: { labels: ['남성', '여성'], datasets: [{ data: stats.genderData, backgroundColor: ['#3b82f6', '#f43f5e'] }] } });
    ageChart = new Chart(ageCtx, { type: 'bar', data: { labels: ['10대', '20대', '30대', '40대', '50대'], datasets: [{ label: '연령대별 예매율', data: stats.ageData, backgroundColor: '#3b82f6' }] }, options: { scales: { y: { beginAtZero: true, max: 100 } } } });
}

function pollStats() {
    const dataStore = document.getElementById("data-store");
    if (!dataStore) return;
    fetch(`/concert/${dataStore.dataset.concertId}/stats-json`).then(res => res.json()).then(data => { if (data && genderChart && ageChart) { genderChart.data.datasets[0].data = data.genderData; ageChart.data.datasets[0].data = data.ageData; genderChart.update(); ageChart.update(); } });
}

function toggleWishlist(concertId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    fetch(`/concert/${concertId}/like`, { method: 'POST', headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json' } })
        .then(res => res.status === 401 ? (alert("로그인이 필요합니다."), window.location.href="/login", null) : res.json())
        .then(data => { if (!data) return; document.getElementById(`wish-icon-${concertId}`).src = data.isLiked ? "/images/favicon.png" : "/images/notfavicon.png"; document.getElementById(`wish-count-${concertId}`).innerText = data.newCount; });
}

function openCouponPopup() { document.getElementById('couponPopup').style.display = 'block'; }
function closeCouponPopup() { document.getElementById('couponPopup').style.display = 'none'; }