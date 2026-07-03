// ==========================================
// 0. 전역 설정 및 URL 파라미터 파싱
// ==========================================
const seatContainer = document.getElementById("seat-container");
const pathSegments = window.location.pathname.split('/');
const concertId = pathSegments[pathSegments.length - 1];

const urlParams = new URLSearchParams(window.location.search);
const selectedDate = urlParams.get('date');
const selectedSessionId = urlParams.get('sessionId');

console.log("좌석 페이지 로드 - concertId:", concertId, "날짜:", selectedDate, "회차:", selectedSessionId);

const currentUserNo = (() => {
    const raw = document.querySelector('meta[name="_userNo"]')?.getAttribute('content');
    return raw ? Number(raw) : null;
})();

let stompClient = null;
let isConcertClosed = false;

// ==========================================
// 1. WebSocket(STOMP) 실시간 클라이언트 설정
// ==========================================
function connectSeatSocket(targetConcertId) {
    const socket = new SockJS('/ws-seat');
    stompClient = new StompJs.Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
            console.log("✅ [WebSocket] 좌석 실시간 알림 서버에 연결되었습니다.");
            stompClient.subscribe(`/topic/seat/${targetConcertId}`, (message) => handleRemoteSeatEvent(JSON.parse(message.body)));
            stompClient.subscribe(`/topic/concert/${targetConcertId}/notice`, (message) => handleConcertNotice(JSON.parse(message.body)));
        },
        onStompError: (frame) => {
            console.error('🚨 [WebSocket] STOMP 프로토콜 에러 발생:', frame.headers['message']);
        }
    });
    stompClient.activate();
}

function handleRemoteSeatEvent(data) {
    if (!data || !data.seatId) return;
    if (currentUserNo && Number(data.userNo) === currentUserNo) return;

    const targetSeat = document.querySelector(`[data-seat-id="${data.seatId}"]`);
    if (!targetSeat) return;

    if (data.status === "SELECTED") {
        console.log(`🔒 타인이 좌석 선점함: ${data.seatId}`);
        targetSeat.style.background = "#d1d5db";
        targetSeat.style.border = "1px solid #9ca3af";
        targetSeat.style.cursor = "not-allowed";
        targetSeat.dataset.status = "locked";
        if (targetSeat.dataset.selected === "true") {
            targetSeat.dataset.selected = "false";
            const activeSelectedSeats = seatContainer.querySelectorAll('[data-selected="true"]');
            updateSelectedSeatsUI(activeSelectedSeats);
            calculateAndDisplayTotalPrice(activeSelectedSeats);
        }
    } else if (data.status === "CANCELLED") {
        console.log(`🔓 타인이 좌석 해제함: ${data.seatId}`);
        targetSeat.dataset.status = "available";
        targetSeat.style.cursor = "pointer";
        targetSeat.style.background = "#3b82f6";
        targetSeat.style.border = "1px solid #2563eb";
    }
}

function handleConcertNotice(data) {
    if (data && data.noticeType === "SOLD_OUT") {
        alert(`📢 공지사항: ${data.message}`);
        isConcertClosed = true;
    }
}

// ==========================================
// 2. 초기 데이터 로딩
// ==========================================
window.isSinglePrice = false;
window.defaultSinglePrice = 0;
window.concertPriceMap = {};
window.dbSeatsData = [];

const seatLayouts = {
    map1: [
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['N','N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','N','N'],
        ['N','N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','N','N'],
        ['N','N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N']
    ]
};

if (concertId) {
    connectSeatSocket(concertId);
    fetch(`/seat/api/concert/${concertId}`)
        .then(res => res.json())
        .then(concert => {
            window.currentLayoutType = concert.layoutType || "SEAT";
            // 🌟 추가: 공연 정보 UI 업데이트 (HTML에 해당 ID가 있어야 합니다)
            // 🌟 ?. 를 사용해서 안전하게 수정 (태그가 없으면 에러 없이 그냥 지나감)
            // 🌟 안전하게 HTML 요소 찾기
            const titleEl = document.getElementById("concert-title");
            const dateEl = document.getElementById("concert-date");
            const timeEl = document.getElementById("concert-runtime");
            const posterEl = document.getElementById("concert-poster");

            // 🌟 요소가 존재할 때만 값을 대입 (에러 방지)
            if (titleEl) titleEl.innerText = concert.concertName || "공연명";
            if (dateEl) dateEl.innerText = selectedDate || "날짜 미정";
            if (timeEl) timeEl.innerText = selectedSessionId || "시간 미정";
            if (posterEl) posterEl.src = concert.concertPosterUrl || "";
            if (concert.concertPriceInfo) {
                concert.concertPriceInfo.split(',').forEach(item => {
                    const pureNumbers = item.replace(/[^0-9]/g, "");
                    if (pureNumbers) {
                        let priceValue = parseInt(pureNumbers, 10);
                        if (priceValue < 10000) priceValue *= 1000;
                        const gradeName = item.replace(/[0-9원\s:]/g, "").trim() || "일반석";
                        window.concertPriceMap[gradeName] = priceValue;
                    }
                });
            }
            return fetch(`/seat/api/seats/${concertId}?date=${selectedDate}&sessionId=${selectedSessionId}`);
        })
        .then(res => res.json())
        .then(seats => {
            window.dbSeatsData = seats;
            const cleanType = (window.currentLayoutType || "SEAT").trim().toUpperCase();
            if (cleanType.includes("STANDING") || cleanType.startsWith("STAND")) {
                showQuantitySelectionForm();
            } else {
                renderSeat();
            }
        })
        .catch(err => console.error("🚨 데이터 로드 중 에러 발생:", err));
}

// ==========================================
// 3. 렌더링 함수
// ==========================================
function renderSeat() {
    seatContainer.innerHTML = "";

    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";
    stageDiv.style.cssText = "width:70%; max-width:500px; height:40px; background:#1e293b; color:#ffffff; font-size:16px; font-weight:bold; display:flex; align-items:center; justify-content:center; margin:0 auto 40px auto; border-radius:4px;";
    seatContainer.appendChild(stageDiv);

    const priceList = Object.values(window.concertPriceMap).sort((a, b) => b - a);
    const defaultPrice = priceList[0] || window.defaultSinglePrice || 0;

    seatLayouts.map1.forEach((row, rowIndex) => {
        const rowDiv = document.createElement("div");
        rowDiv.style.cssText = "display:flex; justify-content:center; gap:4px; margin-bottom:4px;";

        let seatColIndex = 0;
        row.forEach((cell) => {
            const seatDiv = document.createElement("div");
            seatDiv.style.cssText = "width:30px; height:30px; border-radius:4px; display:flex; align-items:center; justify-content:center; font-size:10px;";

            if (cell === "N") {
                seatDiv.style.background = "transparent";
                rowDiv.appendChild(seatDiv);
                return;
            }

            const actualRow = rowIndex + 1;
            const actualCol = seatColIndex + 1;
            const seatId = `SEAT_R${actualRow}_C${actualCol}`;
            const foundSeat = window.dbSeatsData.find(s => s.seatId === seatId || (s.seatRow == String(actualRow) && s.seatCol == String(actualCol)));
            let targetSeat = foundSeat || { seatClass: "STANDARD", price: defaultPrice, seatStatus: 1 };

            seatDiv.dataset.seatId = seatId;
            seatDiv.title = seatId;
            seatDiv.dataset.selected = "false";
            seatDiv.dataset.price = targetSeat.price || defaultPrice;

            // 초기 상태 설정
            if (targetSeat.userNo && currentUserNo && Number(targetSeat.userNo) === currentUserNo) {
                seatDiv.dataset.status = "available";
                seatDiv.dataset.selected = "true";
                seatDiv.style.background = "#1d4ed8";
                seatDiv.style.border = "1px solid #1e40af";
            } else if (targetSeat.seatStatus == 0 || targetSeat.seatStatus === "0") {
                seatDiv.dataset.status = "locked";
                seatDiv.style.background = "#d1d5db";
                seatDiv.style.border = "1px solid #9ca3af";
                seatDiv.style.cursor = "not-allowed";
            } else {
                seatDiv.dataset.status = "available";
                seatDiv.style.background = "#3b82f6";
                seatDiv.style.border = "1px solid #2563eb";
                seatDiv.style.cursor = "pointer";
            }

            // 🌟 [수정된 클릭 이벤트 전체 로직]
            seatDiv.addEventListener("click", () => {
                if (isConcertClosed) { alert("예매가 마감되었습니다."); return; }
                if (seatDiv.dataset.status === "locked") { alert("선택할 수 없는 좌석입니다."); return; }

                const isSelected = seatDiv.dataset.selected === "true";

                if (!isSelected) {
                    if (seatContainer.querySelectorAll('[data-selected="true"]').length >= 4) {
                        alert("최대 4개까지 선택 가능합니다.");
                        return;
                    }
                    // 선택 시
                    seatDiv.dataset.selected = "true";
                    seatDiv.style.background = "#1d4ed8";
                    seatDiv.style.border = "1px solid #1e40af";

                    if (stompClient?.connected) {
                        stompClient.publish({ destination: "/app/seat/select", body: JSON.stringify({ concertId, seatId, userNo: currentUserNo }) });
                    }
                } else {
                    // 해제 시
                    seatDiv.dataset.selected = "false";
                    seatDiv.style.background = "#3b82f6";
                    seatDiv.style.border = "1px solid #2563eb";

                    if (stompClient?.connected) {
                        stompClient.publish({ destination: "/app/seat/cancel", body: JSON.stringify({ concertId, seatId, userNo: currentUserNo }) });
                    }
                }

                // UI 업데이트
                const active = seatContainer.querySelectorAll('[data-selected="true"]');
                updateSelectedSeatsUI(active);
                calculateAndDisplayTotalPrice(active);
            });

            seatColIndex++;
            rowDiv.appendChild(seatDiv);
        });
        seatContainer.appendChild(rowDiv);
    });
    renderLegend();
}

function renderLegend() {
    const legend = document.createElement("div");
    legend.style.cssText = "margin-top:20px; display:flex; justify-content:center; gap:20px; font-size:14px;";
    legend.innerHTML = `
        <div style="display:flex; align-items:center; gap:8px;"><div style="width:20px; height:20px; background:#3b82f6; border-radius:4px;"></div> 선택가능</div>
        <div style="display:flex; align-items:center; gap:8px;"><div style="width:20px; height:20px; background:#1d4ed8; border-radius:4px;"></div> 내 선택</div>
        <div style="display:flex; align-items:center; gap:8px;"><div style="width:20px; height:20px; background:#d1d5db; border-radius:4px;"></div> 예매완료</div>
    `;
    seatContainer.appendChild(legend);
}

function updateSelectedSeatsUI(elements) {
    const display = document.getElementById("selected-seats-display");
    if (display) display.innerHTML = Array.from(elements).map(e => e.dataset.seatId).join(', ');
}

function calculateAndDisplayTotalPrice(elements) {
    const total = Array.from(elements).reduce((sum, el) => sum + parseInt(el.dataset.price || 0), 0);
    const display = document.getElementById("total-price-display");
    if (display) display.innerText = total.toLocaleString() + "원";
}

function showQuantitySelectionForm() {
    seatContainer.innerHTML = `<h2>티켓 수량 선택</h2><div id="qty-rows"></div><div id="standing-total-price">총액: 0원</div><button id="submit-btn">예매하기</button>`;
    Object.keys(window.concertPriceMap).forEach(grade => {
        const row = document.createElement("div");
        row.innerHTML = `<span>${grade}</span><select class="ticket-qty-select" data-grade="${grade}" data-price="${window.concertPriceMap[grade]}"><option value="0">0장</option><option value="1">1장</option><option value="2">2장</option><option value="3">3장</option><option value="4">4장</option></select>`;
        seatContainer.querySelector("#qty-rows").appendChild(row);
    });
    seatContainer.querySelector("#submit-btn").onclick = submitBooking;
}

function submitBooking() {
    if (isConcertClosed) return;
    const qtySelects = seatContainer.querySelectorAll(".ticket-qty-select");
    const isStanding = qtySelects.length > 0;
    let bookingData = { concertId, date: selectedDate, sessionId: selectedSessionId, ticketType: isStanding ? "STANDING" : "SEAT", quantities: {}, selectedSeats: [], totalPrice: 0};
    if (isStanding) {
        qtySelects.forEach(s => {
            const qty = parseInt(s.value);
            if (qty > 0) {
                bookingData.quantities[s.dataset.grade] = qty;
                bookingData.totalPrice += (qty * parseInt(s.dataset.price));
            }
        });
    } else {
        const active = seatContainer.querySelectorAll('[data-selected="true"]');
        active.forEach(s => {
            bookingData.selectedSeats.push(s.dataset.seatId);
            bookingData.totalPrice += parseInt(s.dataset.price || 0);
        });
    }
    fetch("/seat/api/booking/prepare", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bookingData)
    })
        .then(res => res.json())
        .then(data => {
            const finalKey = data.reservationKey || data.bookingId;
            if (finalKey) window.location.href = `/booking/payment?reservationKey=${finalKey}`;
        });
}