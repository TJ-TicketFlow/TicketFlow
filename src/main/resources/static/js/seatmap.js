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
            console.log("[WebSocket] 좌석 실시간 알림 서버에 연결되었습니다.");
            stompClient.subscribe(`/topic/seat/${targetConcertId}`, (message) => handleRemoteSeatEvent(JSON.parse(message.body)));
            stompClient.subscribe(`/topic/concert/${targetConcertId}/notice`, (message) => handleConcertNotice(JSON.parse(message.body)));
        },
        onStompError: (frame) => {
            console.error('[WebSocket] STOMP 프로토콜 에러 발생:', frame.headers['message']);
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
            console.log("① [수신 데이터] concert:", concert);

            window.currentLayoutType = concert.layoutType || concert.concertStatus || "SEAT";

            // [수정] 서버가 실제 DB를 기준으로 계산해서 내려준, 내가 이미 보유한 티켓 매수.
            // (기존에는 좌석 목록(seats)에 존재하지도 않는 s.userNo 필드를 기준으로 계산해서
            //  항상 0으로 취급되는 버그가 있었습니다. 이제 서버가 내려주는 값을 그대로 사용합니다.)
            window.myBookedCount = Number(concert.myBookedCount) || 0;

            const titleEl = document.getElementById("concert-name");
            const dateEl = document.getElementById("concert-date");
            const timeEl = document.getElementById("concert-runtime");
            const posterEl = document.getElementById("concert-poster");

            if (titleEl) titleEl.innerText = concert.concertName || "공연명";
            if (dateEl) dateEl.innerText = selectedDate || "날짜 미정";
            if (timeEl) timeEl.innerText = selectedSessionId || "시간 미정";
            if (posterEl) posterEl.src = concert.concertPosterUrl || "";

            if (concert.concertPriceInfo) {
                concert.concertPriceInfo.split('|').forEach(item => {
                    const target = item.trim();
                    if (!target) return;
                    const pureNumbers = target.replace(/[^0-9]/g, "");
                    if (pureNumbers) {
                        let priceValue = parseInt(pureNumbers, 10);
                        if (priceValue > 0 && priceValue < 10000) {
                            priceValue = priceValue * 1000;
                        }
                        const gradeName = target.replace(/[0-9원\s:,]/g, "").trim() || "일반석";
                        if (priceValue > 0) {
                            window.concertPriceMap[gradeName] = priceValue;
                        }
                    }
                });
            }
            return fetch(`/seat/api/seats/${concertId}?date=${selectedDate}&sessionId=${selectedSessionId}`);
        })
        .then(res => res.json())
        .then(seats => {
            console.log("② [수신 데이터] seats 목록:", seats);
            window.dbSeatsData = Array.isArray(seats) ? seats : [];

            const cleanType = String(window.currentLayoutType || "").trim().toUpperCase();
            console.log("③ [레이아웃 판정] 원본값:", window.currentLayoutType, "-> 변환값:", cleanType);

            if (cleanType.includes("STANDING") || cleanType.startsWith("STAND")) {
                console.log("➔ 스탠딩 UI(수량 선택) 모드로 진입합니다.");
                showQuantitySelectionForm();
            } else {
                console.log("➔ 지정석 배치도(renderSeat) 모드로 진입합니다.");
                renderSeat();
            }
        })
        .catch(err => {
            console.error("[통신 에러] 데이터를 읽는 중 실패했습니다. 백엔드 로그를 확인하세요:", err);
        });
}


// ==========================================
// 3. 렌더링 함수 (지정석 배치도 완벽 제어 버전)
// ==========================================
function renderSeat() {
    console.log("🎬 renderSeat() 함수 시작됨");
    seatContainer.innerHTML = "";

    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";
    stageDiv.style.cssText = "width:70%; max-width:500px; height:40px; background:#1e293b; color:#ffffff; font-size:16px; font-weight:bold; display:flex; align-items:center; justify-content:center; margin:0 auto 40px auto; border-radius:4px;";
    seatContainer.appendChild(stageDiv);

    const priceList = Object.values(window.concertPriceMap).sort((a, b) => b - a);
    const defaultPrice = priceList[0] || window.defaultSinglePrice || 100000;

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

            const foundSeat = window.dbSeatsData.find(s =>
                s && (s.seatId === seatId || (String(s.seatRow) === String(actualRow) && String(s.seatCol) === String(actualCol)))
            );

            let targetSeat = foundSeat || { seatClass: "STANDARD", price: defaultPrice, seatStatus: 1 };

            seatDiv.dataset.seatId = seatId;
            seatDiv.title = seatId;
            seatDiv.dataset.selected = "false";
            seatDiv.dataset.price = targetSeat.price || defaultPrice;

            // 초기 좌석 상태 UI 맵핑
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

            // [중복 결합 문제 완전 교정] 클릭 이벤트 리스너 단 한 번만 매핑
            seatDiv.addEventListener("click", () => {
                if (isConcertClosed) { alert("예매가 마감되었습니다."); return; }
                if (seatDiv.dataset.status === "locked") { alert("선택할 수 없는 좌석입니다."); return; }

                const isSelected = seatDiv.dataset.selected === "true";

                if (!isSelected) {
                    // ① 내가 이 공연에 대해 이미 보유 중인(결제 완료 + 결제 대기중) 티켓 수
                    // [수정] 좌석 목록에는 애초에 userNo 필드가 내려오지 않아 항상 0으로만
                    // 계산되던 버그를 수정했습니다. 이제 서버(/seat/api/concert/{concertId})가
                    // DB를 기준으로 정확히 계산해서 내려준 값(window.myBookedCount)을 사용합니다.
                    const alreadyBookedCount = window.myBookedCount || 0;

                    // ② 현재 이 브라우저 화면에서 실시간으로 파랗게 클릭해둔 선택 수
                    const currentlySelectingCount = seatContainer.querySelectorAll('[data-selected="true"]').length;

                    // ③ 총합 차단 제한 검증 (4장 이상이면 진행 차단)
                    // 최종적으로는 서버(SeatService)가 다시 한 번 확실하게 검증하므로,
                    // 여기서는 사용자에게 미리 안내해주는 역할입니다.
                    if ((alreadyBookedCount + currentlySelectingCount) >= 4) {
                        alert(`인당 최대 4장까지만 예매 가능합니다.\n(기존에 예매하신 티켓: ${alreadyBookedCount}장)`);
                        return;
                    }

                    // 예매 가능 한도 이내일 때 정상 처리
                    seatDiv.dataset.selected = "true";
                    seatDiv.style.background = "#1d4ed8";
                    seatDiv.style.border = "1px solid #1e40af";

                    if (stompClient?.connected) {
                        stompClient.publish({ destination: "/app/seat/select", body: JSON.stringify({ concertId, seatId, userNo: currentUserNo }) });
                    }
                } else {
                    // 선택되어 있던 좌석 해제 처리
                    seatDiv.dataset.selected = "false";
                    seatDiv.style.background = "#3b82f6";
                    seatDiv.style.border = "1px solid #2563eb";

                    if (stompClient?.connected) {
                        stompClient.publish({ destination: "/app/seat/cancel", body: JSON.stringify({ concertId, seatId, userNo: currentUserNo }) });
                    }
                }

                // UI 실시간 총액 반영 호출
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

        // 빈 좌석으로 예매 요청 방지
        if (bookingData.selectedSeats.length === 0) {
            alert("선택된 좌석이 없습니다.");
            return;
        }
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

// ===================================================
// 🎫 4. 티켓 장수 선택형 UI 렌더링 영역 (스탠딩 전용)
// ===================================================
function showQuantitySelectionForm() {
    const rightSidebar = document.querySelector(".right-sidebar");
    if (rightSidebar) rightSidebar.style.display = "none";

    const seatPage = document.querySelector(".seat-page");
    if (seatPage) { seatPage.style.width = "100%"; seatPage.style.flex = "1"; }

    seatContainer.innerHTML = "";

    const formWrapper = document.createElement("div");
    formWrapper.style.padding = "40px 30px";
    formWrapper.style.background = "#ffffff";
    formWrapper.style.borderRadius = "12px";
    formWrapper.style.width = "100%";
    formWrapper.style.maxWidth = "650px";
    formWrapper.style.margin = "40px auto";
    formWrapper.style.boxShadow = "0 10px 25px -5px rgba(0, 0, 0, 0.1)";
    formWrapper.style.boxSizing = "border-box";

    formWrapper.innerHTML = `
        <h2 style="margin-bottom: 8px; text-align: center; color: #1e293b; font-size: 24px; font-weight: bold;">티켓 수량 선택</h2>
        <p style="margin-bottom: 35px; text-align: center; color: #64748b; font-size: 14px;">원하시는 티켓의 등급과 수량을 선택해 주세요. (인당 최대 4장)</p>
        <div id="qty-rows"></div>
    `;

    const qtyRowsContainer = formWrapper.querySelector("#qty-rows");
    const grades = Object.keys(window.concertPriceMap);

    if (grades.length > 0) {
        grades.forEach(gradeName => {
            const price = window.concertPriceMap[gradeName];
            createQuantityRow(qtyRowsContainer, gradeName, price, gradeName);
        });
    } else if (window.isSinglePrice && window.defaultSinglePrice > 0) {
        createQuantityRow(qtyRowsContainer, "전석 일반석", window.defaultSinglePrice, "GENERAL");
    } else {
        qtyRowsContainer.innerHTML = `<p style="text-align:center; color:#ef4444; font-weight:bold; margin-top:20px;">공연 가격 정보를 읽어오지 못했습니다.</p>`;
    }

    const totalBox = document.createElement("div");
    totalBox.style.marginTop = "30px"; totalBox.style.paddingTop = "20px"; totalBox.style.borderTop = "2px dashed #e2e8f0";
    totalBox.style.display = "flex"; totalBox.style.justifyContent = "space-between"; totalBox.style.alignItems = "center";
    totalBox.innerHTML = `
        <span style="font-weight: bold; color: #475569; font-size: 16px;">총 결제 금액</span>
        <span id="standing-total-price" style="font-weight: bold; color: #3b82f6; font-size: 26px;">0원</span>
    `;
    formWrapper.appendChild(totalBox);

    const submitBtn = document.createElement("button");
    submitBtn.innerText = "예매하기";
    submitBtn.id = "submit-btn";
    submitBtn.style.width = "100%"; submitBtn.style.marginTop = "24px"; submitBtn.style.padding = "15px";
    submitBtn.style.background = "#3b82f6"; submitBtn.style.color = "#fff"; submitBtn.style.border = "none";
    submitBtn.style.borderRadius = "6px"; submitBtn.style.fontSize = "16px"; submitBtn.style.fontWeight = "bold";
    submitBtn.style.cursor = "pointer";

    submitBtn.addEventListener("click", () => { submitBooking(); });
    formWrapper.appendChild(submitBtn);

    seatContainer.appendChild(formWrapper);
}

function createQuantityRow(container, label, price, gradeCode) {
    const row = document.createElement("div");
    row.style.display = "flex"; row.style.justifyContent = "space-between"; row.style.alignItems = "center";
    row.style.marginBottom = "20px"; row.style.paddingBottom = "16px"; row.style.borderBottom = "1px solid #f1f5f9"; row.style.gap = "16px";

    row.innerHTML = `
        <div style="flex: 1; min-width: 0; text-align: left;">
            <div style="font-weight: bold; color: #334155; font-size: 16px;">${label}</div>
            <div style="font-size: 14px; color: #64748b; margin-top: 4px;">${price.toLocaleString()}원</div>
        </div>
        <div style="flex-shrink: 0;">
            <select class="ticket-qty-select" data-grade="${gradeCode}" data-price="${price}" style="padding: 8px 12px; border-radius: 6px; border: 1px solid #cbd5e1; background: #fff; font-size: 15px; font-weight: 500; cursor: pointer; outline: none;">
                <option value="0">0장</option> 
                <option value="1">1장</option>
                <option value="2">2장</option> 
                <option value="3">3장</option> 
                <option value="4">4장</option>
            </select>
        </div>
    `;

    const selectEl = row.querySelector(".ticket-qty-select");
    selectEl.addEventListener("change", (e) => { handleQuantityChange(e.target); });
    container.appendChild(row);
}

function handleQuantityChange(changedSelect) {
    const selects = seatContainer.querySelectorAll(".ticket-qty-select");
    let newlySelectedTickets = 0;
    let totalPrice = 0;

    selects.forEach(select => { newlySelectedTickets += parseInt(select.value, 10); });

    // [수정] 좌석 데이터에는 userNo가 없어 항상 0으로 계산되던 버그를 수정.
    // 서버가 내려준 실제 보유 티켓 수(window.myBookedCount)를 사용합니다.
    const alreadyBookedTickets = window.myBookedCount || 0;

    if ((alreadyBookedTickets + newlySelectedTickets) > 4) {
        alert(`티켓은 기존 예매 내역을 포함하여 최대 4장까지만 선택 가능합니다.\n(이미 예매하신 수량: ${alreadyBookedTickets}장)`);
        if (changedSelect) changedSelect.value = "0";
        handleQuantityChange(null);
        return;
    }

    selects.forEach(select => {
        const qty = parseInt(select.value, 10);
        const price = parseInt(select.dataset.price, 10);
        totalPrice += (qty * price);
    });

    const standingPriceEl = document.getElementById("standing-total-price");
    if (standingPriceEl) standingPriceEl.innerText = totalPrice.toLocaleString() + "원";
}