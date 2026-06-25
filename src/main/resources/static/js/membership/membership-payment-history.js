window.cancelMembership = function() {
    if (!confirm('멤버십 정기 결제를 해지하시겠습니까? 이번 달 혜택까지만 유지됩니다.')) return;

    fetch('/api/payment/cancel', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
        .then(res => res.json())
        .then(data => {
            alert(data.reason);
            if (data.eligible) {
                location.reload();
            }
        });
};

window.restartMembership = function() {
    if (!confirm('멤버십 해지를 취소하고 다시 정기 구독을 유지하시겠습니까?')) return;

    fetch('/api/payment/resume', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(res => {
            if(res.ok) return res.text();
            throw new Error('서버 통신 에러');
        })
        .then(message => {
            alert(message);
            location.reload();
        })
        .catch(error => {
            alert('처리 중 오류가 발생했습니다.');
            console.error(error);
        });
};