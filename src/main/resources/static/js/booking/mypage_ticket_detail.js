// ==========================================
// 1. 취소 버튼을 눌렀을 때: 수수료를 먼저 확인하고 의사를 묻는 함수
// ==========================================
function checkAndCancel(payNo) {
    // 혹시라도 HTML에서 결제 번호가 안 넘어왔다면 여기서 막아줍니다.
    if (!payNo) {
        alert("잘못된 접근입니다. 결제 번호를 찾을 수 없습니다.");
        return;
    }

    // 💡 백엔드에 만들어둔 '미리보기(getCancelFeeInfo)' 컨트롤러로 GET 요청을 보냅니다.
    // 주의: 개발자님의 실제 컨트롤러 매핑 주소에 맞춰 URL을 확인해 주세요!
    fetch(`/booking/${payNo}/cancelable`)
        .then(response => {
            if (!response.ok) {
                throw new Error("취소 수수료 정보를 불러오는데 실패했습니다.");
            }
            return response.json(); // 백엔드가 보내준 Map 데이터(cancelable, fee, refundAmount)를 JSON으로 변환
        })
        .then(data => {
            // 백엔드에서 방어 로직으로 "취소 기한 지남" 등을 판단하여 cancelable: false를 주었을 경우
            if (data.cancelable === false) {
                alert("현재 예매를 취소할 수 없는 상태이거나, 취소 마감 시간이 지났습니다.");
                return;
            }

            // 화면에 띄워줄 확인 메시지 예쁘게 조립하기 (toLocaleString()으로 콤마 찍기)
            const confirmMsg = `정말 예매를 취소하시겠습니까?\n\n` +
                `📉 예상 취소 수수료: ${data.fee.toLocaleString()}원\n` +
                `💸 최종 환불 금액: ${data.refundAmount.toLocaleString()}원`;

            // 사용자가 알림창에서 '확인'을 누르면!
            if (confirm(confirmMsg)) {
                executeCancel(payNo); // 2단계인 '진짜 취소' 함수로 넘깁니다.
            }
        })
        .catch(error => {
            console.error("❌ 취소 수수료 조회 중 에러 발생:", error);
            alert("취소 수수료 정보를 확인하는 중 문제가 발생했습니다.");
        });
}

// ==========================================
// 2. 사용자가 최종 동의했을 때: 진짜로 취소를 실행하는 함수
// ==========================================
function executeCancel(payNo) {

    // 🛡️ 스프링 시큐리티 방어막 통과를 위한 CSRF 토큰 챙기기 (HTML에 있는 meta 태그에서 꺼내옵니다)
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = { "Content-Type": "application/json" };

    // 토큰이 존재한다면 헤더에 안전하게 담아줍니다.
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }

    // 💡 백엔드에 만들어둔 '진짜 취소(cancelTicket)' 컨트롤러로 POST 요청을 보냅니다.
    fetch(`/booking/${payNo}/cancel`, {
        method: "POST",
        headers: headers
    })
        .then(async response => {
            if (!response.ok) {
                // 백엔드가 400 등의 에러 코드와 함께 글자를 던져주면 그걸 꺼내서 경고창으로 띄웁니다.
                const errorText = await response.text();
                throw new Error(errorText || "예매 취소 처리에 실패했습니다.");
            }
            return response.text(); // 성공했을 때 백엔드가 주는 메시지
        })
        .then(message => {
            // 성공!
            alert("성공적으로 예매 취소가 완료되었습니다. 좌석이 다시 풀렸습니다.");

            // 새로고침을 해서 HTML 화면이 '결제 취소' 상태로 바뀌도록 합니다.
            window.location.reload();
        })
        .catch(error => {
            console.error("❌ 예매 취소 실행 중 에러 발생:", error);
            alert(error.message); // 예: "이미 취소되었거나 결제 완료 상태가 아닙니다."
        });
}