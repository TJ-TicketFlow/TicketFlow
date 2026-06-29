const urlParams = new URLSearchParams(window.location.search);
const resKey = urlParams.get('reservationKey');

window.addEventListener('pagehide', function (event) {
    if (resKey && resKey !== "undefined" && resKey !== "null") {
        console.log("↩️ 페이지 이탈 감지: 임시 좌석 해제 요청을 전송합니다. Key:", resKey);

        const url = `/seat/api/booking/cancel-ajax`;

        // 🌟 [핵심 개선] sendBeacon이 규격화된 Form 데이터 형태로 보내도록 랩핑합니다.
        // 이렇게 보내야 Spring의 @RequestParam("reservationKey")이 100% 안 터지고 인식합니다.
        const formData = new FormData();
        formData.append("reservationKey", resKey);

        // CSRF 토큰이 필요할 경우 함께 실어 보냅니다.
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
        if (csrfToken) {
            formData.append("_csrf", csrfToken);
        }

        // 백엔드로 안전하게 비동기 비콘 전송
        navigator.sendBeacon(url, formData);
    }



});

function goBackToSeat() {
    console.log("품격 있는 이탈: 브라우저 뒤로가기를 시뮬레이션합니다.");
    // 브라우저의 실제 뒤로가기 버튼을 누른 것과 100% 동일한 효과를 냅니다.
    window.history.back();
}
