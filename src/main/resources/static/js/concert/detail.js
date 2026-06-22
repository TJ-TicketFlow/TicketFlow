let genderChart, ageChart;

document.addEventListener('DOMContentLoaded', function() {
    const dataStore = document.getElementById("data-store");
    if (!dataStore) return;

    const concertId = dataStore.dataset.concertId;

    // 1. 통계 데이터 API 호출
    fetch(`/concert/${concertId}/stats-json`)
        .then(res => res.json())
        .then(data => {
            if (data && data.genderData && data.ageData) {
                initCharts(data);
                setInterval(pollStats, 3000);
            }
        })
        .catch(err => console.error("통계 데이터 로드 실패", err));

    // 2. 달력 생성
    const calendarEl = document.querySelector('.concert-calendar');
    if (calendarEl) {
        fetch(`/concert/${concertId}/available-dates`)
            .then(res => res.json())
            .then(availableDates => {
                const calendar = new FullCalendar.Calendar(calendarEl, {
                    locale: 'ko',
                    initialView: 'dayGridMonth',
                    selectable: true,
                    dateClick: (info) => {
                        if (availableDates.includes(info.dateStr)) {
                            loadSessions(info.dateStr, concertId);
                        } else {
                            alert("해당 날짜에는 공연이 없습니다.");
                        }
                    },
                    dayCellDidMount: (info) => {
                        if (availableDates.includes(info.dateStr)) {
                            info.el.style.backgroundColor = '#f0f7ff';
                        }
                    }
                });
                calendar.render();
            });
    }
});

function initCharts(stats) {
    genderChart = new Chart(document.getElementById('genderChart'), {
        type: 'doughnut',
        data: {
            labels: ['남성', '여성'],
            datasets: [{
                data: stats.genderData,
                backgroundColor: ['#3b82f6', '#f43f5e']
            }]
        }
    });

    ageChart = new Chart(document.getElementById('ageChart'), {
        type: 'bar',
        data: {
            labels: ['10대', '20대', '30대', '40대', '50대'],
            datasets: [{
                label: '연령대별 예매율',
                data: stats.ageData,
                backgroundColor: '#3b82f6'
            }]
        },
        options: {
            scales: { y: { beginAtZero: true, max: 100 } }
        }
    });
}

function pollStats() {
    const concertId = document.getElementById("data-store").dataset.concertId;
    fetch(`/concert/${concertId}/stats-json`)
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
        headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json' },
        credentials: 'include'
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