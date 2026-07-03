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
                // 1. 🌟 쉼표(,)가 아니라 파이프(|) 기호를 기준으로 좌석을 나눕니다!
                concert.concertPriceInfo.split('|').forEach(item => {
                    const target = item.trim();
                    if (!target) return;

                    // 2. 문자열 안의 숫자만 쏙 뽑아냅니다. (천 단위 쉼표 무시)
                    const pureNumbers = target.replace(/[^0-9]/g, "");

                    if (pureNumbers) {
                        let priceValue = parseInt(pureNumbers, 10);

                        // DB에 178로 적혔을 때를 대비한 기존 1000 곱하기 로직 유지
                        if (priceValue > 0 && priceValue < 10000) {
                            priceValue = priceValue * 1000;
                        }

                        // 3. 좌석 이름만 뽑아냅니다. (숫자, '원', 쉼표, 콜론 등 불필요한 기호 제거)
                        const gradeName = target.replace(/[0-9원\s:,]/g, "").trim() || "일반석";

                        // 4. 🌟 가격이 0원인 유령 좌석은 화면에 안 나오게 컷!
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

// ===================================================
// 🎫 4. 티켓 장수 선택형 UI 렌더링 영역 (스탠딩 전용 - 예쁜 파란/하양 CSS 복원!)
// ===================================================
function showQuantitySelectionForm() {
    // 1. 지정석 전용 우측 사이드바 숨기기 및 레이아웃 정렬
    const rightSidebar = document.querySelector(".right-sidebar");
    if (rightSidebar) rightSidebar.style.display = "none";

    const seatPage = document.querySelector(".seat-page");
    if (seatPage) { seatPage.style.width = "100%"; seatPage.style.flex = "1"; }

    seatContainer.innerHTML = "";

    // 2. 🌟 잃어버렸던 예쁜 하얀색 둥근 박스 껍데기 복원!
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

    // 3. 등급별 수량 선택 박스 예쁘게 채워넣기
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

    // 4. 🌟 파란색 총 결제 금액 박스 복원!
    const totalBox = document.createElement("div");
    totalBox.style.marginTop = "30px"; totalBox.style.paddingTop = "20px"; totalBox.style.borderTop = "2px dashed #e2e8f0";
    totalBox.style.display = "flex"; totalBox.style.justifyContent = "space-between"; totalBox.style.alignItems = "center";
    totalBox.innerHTML = `
        <span style="font-weight: bold; color: #475569; font-size: 16px;">총 결제 금액</span>
        <span id="standing-total-price" style="font-weight: bold; color: #3b82f6; font-size: 26px;">0원</span>
    `;
    formWrapper.appendChild(totalBox);

    // 5. 🌟 꽉 차는 파란색 예매하기 버튼 복원!
    const submitBtn = document.createElement("button");
    submitBtn.innerText = "예매하기";
    submitBtn.id = "submit-btn";
    submitBtn.style.width = "100%"; submitBtn.style.marginTop = "24px"; submitBtn.style.padding = "15px";
    submitBtn.style.background = "#3b82f6"; submitBtn.style.color = "#fff"; submitBtn.style.border = "none";
    submitBtn.style.borderRadius = "6px"; submitBtn.style.fontSize = "16px"; submitBtn.style.fontWeight = "bold";
    submitBtn.style.cursor = "pointer";

    // 클릭 시, 이미 완벽하게 고쳐둔 submitBooking 함수 실행!
    submitBtn.addEventListener("click", () => { submitBooking(); });
    formWrapper.appendChild(submitBtn);

    seatContainer.appendChild(formWrapper);
}

// 개별 행(Row) 디자인을 담당하는 함수 복원!
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

// 4장 초과 방지 및 총액 계산 로직 복원!
function handleQuantityChange(changedSelect) {
    const selects = seatContainer.querySelectorAll(".ticket-qty-select");
    let totalSelectedTickets = 0;
    let totalPrice = 0;

    selects.forEach(select => { totalSelectedTickets += parseInt(select.value, 10); });

    if (totalSelectedTickets > 4) {
        alert("티켓은 모든 등급을 합산하여 최대 4장까지만 선택 가능합니다.");
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