// ==========================================
// 1. 취소 버튼을 눌렀을 때: 수수료를 먼저 확인하고 의사를 묻는 함수 (수정 버전)
// ==========================================
function checkAndCancel(payNo) {
    if (!payNo) {
        alert("잘못된 접근입니다. 결제 번호를 찾을 수 없습니다.");
        return;
    }

    fetch(`/booking/${payNo}/cancelable`)
        .then(async response => {
            if (!response.ok) {
                // 1. 백엔드가 보낸 데이터를 일단 문자열로 읽습니다.
                const errorText = await response.text();
                try {
                    // 2. 만약 데이터가 {"message":"..."} 형태의 상자(JSON)라면?
                    const errorJson = JSON.parse(errorText);
                    // 3. 상자 안에서 'message' 알맹이만 쏙 빼서 던집니다!
                    throw new Error(errorJson.message || "취소 처리에 실패했습니다.");
                } catch (e) {
                    // 4. JSON 상자가 아니라 일반 글자라면 그냥 던집니다.
                    if (e.name !== "SyntaxError") throw e;
                    throw new Error(errorText || "취소 정보를 불러오는데 실패했습니다.");
                }
            }
            return response.json();
        })
        .then(data => {
            // 💡 [핵심] 200 OK 정상 통신이지만, 백엔드에서 cancelable: false를 준 경우!
            if (data.cancelable === false) {
                // 백엔드가 포장해서 보내준 진짜 한국어 메시지를 꺼내서 보여줍니다.
                alert(data.message || "현재 예매를 취소할 수 없는 상태이거나, 마감 시간이 지났습니다.");
                return;
            }

            const confirmMsg = `정말 예매를 취소하시겠습니까?\n\n` +
                `📉 예상 취소 수수료: ${data.fee.toLocaleString()}원\n` +
                `💸 최종 환불 금액: ${data.refundAmount.toLocaleString()}원`;

            if (confirm(confirmMsg)) {
                executeCancel(payNo);
            }
        })
        .catch(error => {
            console.error("❌ 취소 수수료 조회 중 에러 발생:", error);
            // 드디어 깔끔하게 "취소 마감시간이 지나 취소할 수 없습니다." 만 화면에 뜹니다!
            alert(error.message);
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