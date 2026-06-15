document.addEventListener("DOMContentLoaded", function() {
    // 성별 도넛 그래프
    const genderCanvas = document.getElementById('genderDonutChart');
    if (genderCanvas) {
        const genderCtx = genderCanvas.getContext('2d');
        new Chart(genderCtx, {
            type: 'doughnut',
            data: {
                labels: ['남성', '여성'],
                datasets: [{
                    data: [49.2, 50.8],
                    backgroundColor: ['#3b82f6', '#ec4899']
                }]
            }
        });
    }

    // 연령별 막대 그래프
    const ageCanvas = document.getElementById('ageBarChart');
    if (ageCanvas) {
        const ageCtx = ageCanvas.getContext('2d');
        new Chart(ageCtx, {
            type: 'bar',
            data: {
                labels: ['10대', '20대', '30대', '40대', '50대'],
                datasets: [{
                    label: '연령대별 예매자 수',
                    data: [10, 35, 30, 15, 10],
                    backgroundColor: '#818cf8'
                }]
            }
        });
    }
});