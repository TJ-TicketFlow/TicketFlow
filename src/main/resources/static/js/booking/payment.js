// 1. 자바스크립트 파일이 제대로 연결되었는지 확인하는 메시지
console.log("✅ payment.js 파일이 성공적으로 로드되었습니다!");

// 2. HTML 화면이 모두 준비될 때까지 기다리는 안전장치
document.addEventListener("DOMContentLoaded", function() {

    console.log("✅ 화면의 모든 요소가 준비되었습니다.");

    // 버튼 찾기
    const payButton = document.getElementById('createPayment');

    // 버튼이 잘 찾아졌는지 확인
    if (payButton) {
        console.log("✅ '결제하기' 버튼을 찾았습니다. 이벤트를 연결합니다.");

        payButton.addEventListener('click', function() {
            alert("결제창을 불러오는 중입니다. 잠시만 기다려주세요!");

            const requestData = {
                reservationKey: 1,
                payName: "아이유 콘서트 VIP석",
                payAmount: 135000,
                buyerName: "홍길동",
                buyerEmail: "hong@gmail.com",
                payDelName: "홍길동",
                payDelCall: "010-1234-5678",
                payDelPostcode: "01234",
                payDelAddr: "서울시 강남구 어딘가"
            };

            fetch('/booking/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(requestData)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('서버 통신 실패 (상태 코드: ' + response.status + ')');
                    }
                    return response.text();
                })
                .then(checkoutUrl => {
                    console.log("✅ 받아온 결제창 URL:", checkoutUrl);
                    window.location.href = checkoutUrl;
                })
                .catch(error => {
                    console.error('❌ 결제 준비 중 오류 발생:', error);
                    alert('결제창을 불러오는 데 실패했습니다.');
                });
        });
    } else {
        console.error("❌ 오류: HTML에서 id가 'createPayment'인 버튼을 찾을 수 없습니다.");
    }
});