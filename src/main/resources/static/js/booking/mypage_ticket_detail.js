let targetCancelPayNo = null;
// ==========================================
// 1. 취소 버튼을 눌렀을 때: 수수료를 먼저 확인하고 의사를 묻는 함수
// ==========================================
function checkAndCancel(payNo) {
    if (!payNo || payNo === 'null' || payNo === 'undefined') {
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

            document.getElementById('modal-fee').textContent = data.fee.toLocaleString() + '원';
            document.getElementById('modal-refund').textContent = data.refundAmount.toLocaleString() + '원';

            // 나중에 '네, 취소합니다' 버튼을 눌렀을 때 쓸 수 있도록 번호를 기억해둡니다.
            targetCancelPayNo = payNo;

            // 짜잔! 모달 창을 화면에 보여줍니다.
            document.getElementById('cancelConfirmModal').style.display = 'flex';
        })
        .catch(error => {
            console.error("❌ 취소 수수료 조회 중 에러 발생:", error);
            alert(error.message);
        });
}

// ==========================================
// 모달 제어용 헬퍼 함수들
// ==========================================

// 닫기 버튼을 누르면 모달을 숨기고 기억해둔 번호를 지웁니다.
function closeCancelModal() {
    document.getElementById('cancelConfirmModal').style.display = 'none';
    targetCancelPayNo = null;
}

// 화면이 다 그려지고 나면, '네, 취소합니다' 버튼에 클릭 이벤트를 달아줍니다.
document.addEventListener('DOMContentLoaded', function() {
    const confirmBtn = document.getElementById('btn-execute-cancel');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function() {
            if (targetCancelPayNo) {

                const payNoToCancel = targetCancelPayNo;

                closeCancelModal();

                executeCancel(payNoToCancel);
            }
        });
    }
});

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
            document.getElementById('cancelSuccessModal').style.display = 'flex';

        })
        .catch(error => {
            console.error("❌ 예매 취소 실행 중 에러 발생:", error);
            alert(error.message);
        });
}

// ==========================================
// 성공 모달의 '확인' 버튼을 눌렀을 때 실행되는 함수
// ==========================================
function closeSuccessAndReload() {
    // 1. 모달 창을 다시 숨깁니다.
    document.getElementById('cancelSuccessModal').style.display = 'none';

    // 2. 유저가 메시지를 다 읽고 [확인]을 눌렀을 때 비로소 화면을 새로고침 합니다!
    window.location.reload();
}