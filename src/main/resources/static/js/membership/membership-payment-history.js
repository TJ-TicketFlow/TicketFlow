function requestRefund() {
    if (!confirm('환불을 신청하시겠습니까? 신청 시 멤버십이 즉시 해지됩니다.')) return;
    fetch('/api/payment/refund', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            alert(data.reason);
            if (data.eligible) {
                location.reload();
            }
        });
}