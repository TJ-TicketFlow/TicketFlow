let genderChart, ageChart;

document.addEventListener('DOMContentLoaded', function() {
    const dataStore = document.getElementById("data-store");
    if (!dataStore) return;

    const concertId = dataStore.dataset.concertId;
    // 공연 기간 데이터 가져오기
    const startDate = dataStore.dataset.startDate;
    const endDate = dataStore.dataset.endDate;
    const bookingForm = document.getElementById('bookingForm');

    const submitBtn = bookingForm ? bookingForm.querySelector('button[type="submit"]') : null;
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.style.opacity = "0.5";
        submitBtn.style.cursor = "not-allowed";
    }

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

    fetch(`/concert/${concertId}/stats-json`)
        .then(res => res.json())
        .then(data => {
            if (data && data.genderData && data.ageData) {
                initCharts(data);
                setInterval(pollStats, 3000);
            }
        })
        .catch(err => console.log("통계 데이터 로드 생략"));

    const calendarEl = document.querySelector('.concert-calendar');
    if (calendarEl) {
        fetch(`/concert/${concertId}/available-dates`)
            .then(res => res.json())
            .then(availableDates => {
                const calendar = new FullCalendar.Calendar(calendarEl, {
                    locale: 'ko',
                    initialView: 'dayGridMonth',
                    // [수정] 공연 시작일로 자동 이동
                    initialDate: startDate || new Date().toISOString().split('T')[0],
                    selectable: true,
                    dateClick: (info) => {
                        if (availableDates.includes(info.dateStr)) {
                            loadSessions(info.dateStr, concertId);
                        } else {
                            alert("해당 날짜에는 공연이 없습니다.");
                        }
                    },
                    dayCellDidMount: (info) => {
                        // [수정] 공연 기간 내 날짜에 배경색 적용
                        if (info.dateStr >= startDate && info.dateStr <= endDate) {
                            info.el.style.backgroundColor = '#e1f5fe';
                        }
                        // 기존: 사용 가능한 날짜 테두리 강조
                        if (availableDates.includes(info.dateStr)) {
                            info.el.style.border = '1px solid #3b82f6';
                        }
                    }
                });
                calendar.render();
            });
    }
});

function initCharts(stats) {
    const genderCtx = document.getElementById('genderChart');
    const ageCtx = document.getElementById('ageChart');
    if (!genderCtx || !ageCtx) return;

    genderChart = new Chart(genderCtx, {
        type: 'doughnut',
        data: {
            labels: ['남성', '여성'],
            datasets: [{ data: stats.genderData, backgroundColor: ['#3b82f6', '#f43f5e'] }]
        }
    });

    ageChart = new Chart(ageCtx, {
        type: 'bar',
        data: {
            labels: ['10대', '20대', '30대', '40대', '50대'],
            datasets: [{ label: '연령대별 예매율', data: stats.ageData, backgroundColor: '#3b82f6' }]
        },
        options: { scales: { y: { beginAtZero: true, max: 100 } } }
    });
}

function pollStats() {
    const dataStore = document.getElementById("data-store");
    if (!dataStore) return;
    fetch(`/concert/${dataStore.dataset.concertId}/stats-json`)
        .then(res => res.json())
        .then(data => {
            if (data && genderChart && ageChart) {
                genderChart.data.datasets[0].data = data.genderData;
                ageChart.data.datasets[0].data = data.ageData;
                genderChart.update();
                ageChart.update();
            }
        });
}

function loadSessions(date, concertId) {
    document.getElementById('selectedDate').value = date;
    document.getElementById('selectedSessionId').value = '';

    const submitBtn = document.querySelector('#bookingForm button[type="submit"]');
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.style.opacity = "0.5";
        submitBtn.style.cursor = "not-allowed";
    }

    fetch(`/concert/${concertId}/sessions?date=${date}`)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById('session-container');
            container.innerHTML = '';
            data.forEach(s => {
                const btn = document.createElement('button');
                btn.type = 'button'; btn.className = 'btn-session';
                btn.innerText = s.time;
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
                container.appendChild(btn);
            });
        });
}

function toggleWishlist(concertId) {
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    fetch(`/concert/${concertId}/like`, {
        method: 'POST',
        headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json' }
    })
        .then(res => res.status === 401 ? (alert("로그인이 필요합니다."), window.location.href="/login", null) : res.json())
        .then(data => {
            if (!data) return;
            document.getElementById(`wish-icon-${concertId}`).src = data.isLiked ? "/images/favicon.png" : "/images/notfavicon.png";
            document.getElementById(`wish-count-${concertId}`).innerText = data.newCount;
        });
}

function openCouponPopup() { document.getElementById('couponPopup').style.display = 'block'; }
function closeCouponPopup() { document.getElementById('couponPopup').style.display = 'none'; }