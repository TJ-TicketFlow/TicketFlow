// ==========================================
// 🔌 1. WebSocket(STOMP) 실시간 클라이언트 설정
// ==========================================
// 현재 로그인한 사용자 번호 (없으면 비로그인 상태)
const currentUserNo = (() => {
    const raw = document.querySelector('meta[name="_userNo"]')?.getAttribute('content');
    return raw ? Number(raw) : null;
})();

let stompClient = null;
let isConcertClosed = false; // 매진(마감) 여부 - true가 되면 좌석 선택 자체를 막음

// 페이지 로드 시 소켓 연결 및 구독 시작
function connectSeatSocket(targetConcertId) {
    // 💡 백엔드 Spring WebSocket Endpoint인 /ws-seat로 SockJS 연결 생성
    const socket = new SockJS('/ws-seat');
    stompClient = new StompJs.Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000, // 연결이 끊기면 5초마다 자동 재접속

        onConnect: () => {
            console.log("✅ [WebSocket] 좌석 실시간 알림 서버에 연결되었습니다.");

            // [구독 1] 다른 사용자가 이 공연의 좌석을 선점/해제할 때마다 알림 수신
            stompClient.subscribe(`/topic/seat/${targetConcertId}`, (message) => {
                const data = JSON.parse(message.body);
                handleRemoteSeatEvent(data);
            });

            // [구독 2] 이 공연의 전 좌석이 마감(매진)되었을 때 알림 수신
            stompClient.subscribe(`/topic/concert/${targetConcertId}/notice`, (message) => {
                const data = JSON.parse(message.body);
                handleConcertNotice(data);
            });
        },

        onStompError: (frame) => {
            console.error('🚨 [WebSocket] STOMP 프로토콜 에러 발생:', frame.headers['message']);
        }
    });

    stompClient.activate();
}

// 실시간 소켓 이벤트 핸들러: 타인의 선점/취소 액션 처리
function handleRemoteSeatEvent(data) {
    if (!data || !data.seatId) return;

    // 본인이 발생시킨 이벤트는 브라우저 자체 UI 로직에서 선처리하므로 타인의 이벤트만 수용
    if (currentUserNo && Number(data.userNo) === currentUserNo) {
        return;
    }

    const targetSeat = document.querySelector(`[data-seat-id="${data.seatId}"]`);
    if (!targetSeat) return;

    if (data.status === "SELECTED") {
        console.log(`🔒 타인이 좌석 선점함: ${data.seatId}`);
        targetSeat.style.background = "#d1d5db";
        targetSeat.style.border = "1px solid #9ca3af";
        targetSeat.style.cursor = "not-allowed";
        targetSeat.dataset.status = "locked";

        // 만약 내가 선택 중이던 좌석을 뺏긴 경우 선택 상태 해제
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

// 실시간 마감 공지 핸들러
function handleConcertNotice(data) {
    if (data && data.noticeType === "SOLD_OUT") {
        alert(`📢 공지사항: ${data.message}`);
        isConcertClosed = true;
    }
}

// ==========================================
// 공연 ID 및 전역 설정
// ==========================================
console.log("seatmap.js 실행됨 (실시간 STOMP 소켓 및 DB 데이터 연동 활성화)");

const seatContainer = document.getElementById("seat-container");
const pathSegments = window.location.pathname.split('/');
const concertId = pathSegments[pathSegments.length - 1];

let seatLayout;
let vipRows = [];

// 중복 선언 에러 방지를 위한 글로벌 윈도우 스코프 바인딩
window.isSinglePrice = false;
window.defaultSinglePrice = 0;
window.concertPriceMap = {};
window.dbSeatsData = [];

// 소켓 기동
if (concertId) {
    connectSeatSocket(concertId);
}

// ==========================================
// 📥 2. 공연 정보 & 레이아웃 순서 제어 (지정석 vs 스탠딩)
// ==========================================
if (concertId) {
    console.log("🔍 [디버깅] 현재 자바스크립트가 파싱한 concertId:", concertId);

    // [Step 1] 공연 기본 정보 가져오기
    fetch(`/seat/api/concert/${concertId}`)
        .then(res => {
            if (!res.ok) throw new Error(`공연 정보 요청 실패 (Status: ${res.status})`);
            return res.json();
        })
        .then(concert => {
            console.log("✈️ [디버깅] 백엔드에서 받은 원본 공연 데이터:", concert);

            // 안전하게 전역 저장
            window.currentLayoutType = concert.layoutType || "SEAT";

            const nameEl = document.getElementById("concert-name");
            const posterEl = document.getElementById("concert-poster");
            const runtimeEl = document.getElementById("concert-runtime");
            const dateEl = document.getElementById("concert-date");

            if (nameEl) nameEl.innerText = concert.concertName || "공연명 없음";
            if (posterEl) posterEl.src = concert.concertPosterUrl || "";
            if (dateEl) dateEl.innerText = concert.concertDate || "날짜 정보 없음";
            if (runtimeEl) runtimeEl.innerText = concert.concertTime || concert.concertRuntime || "시간 정보 없음";

            window.concertPriceMap = {};

            if (concert.concertPriceInfo) {
                const priceInfo = concert.concertPriceInfo.trim();
                console.log("🔍 [가격 파싱] 원본 문자열:", priceInfo);

                const items = priceInfo.split(',');
                items.forEach(item => {
                    const target = item.trim();
                    if (!target) return;

                    const pureNumbers = target.replace(/[^0-9]/g, "");
                    if (pureNumbers) {
                        let priceValue = parseInt(pureNumbers, 10);
                        if (priceValue > 0 && priceValue < 10000) {
                            priceValue = priceValue * 1000;
                        }
                        const gradeName = target.replace(/[0-9원\s:]/g, "").trim() || "일반석";
                        if (priceValue > 0) {
                            window.concertPriceMap[gradeName] = priceValue;
                        }
                    }
                });

                if (Object.keys(window.concertPriceMap).length === 0) {
                    window.isSinglePrice = true;
                    let singlePrice = parseInt(priceInfo.replace(/[^0-9]/g, ""), 10);
                    if (!isNaN(singlePrice) && singlePrice > 0) {
                        if (singlePrice < 10000) singlePrice = singlePrice * 1000;
                        window.defaultSinglePrice = singlePrice;
                        const extractedLabel = priceInfo.replace(/[0-9원\s:]/g, "").trim() || "전석 일반석";
                        window.concertPriceMap[extractedLabel] = window.defaultSinglePrice;
                    }
                }
            }

            console.log("✅ [가격 파싱 완료] 구조화된 가격 객체:", window.concertPriceMap);

            // [Step 2] 백엔드 DB에서 실제 좌석 목록 가져오기
            return fetch(`/seat/api/seats/${concertId}`);
        })
        .then(res => {
            if (!res.ok) throw new Error(`좌석 목록 요청 실패 (Status: ${res.status})`);
            return res.json();
        })
        .then(seats => {
            console.log("✈️ [디버깅] 백엔드 DB에서 조회된 실제 좌석 개수:", seats.length, "개");
            window.dbSeatsData = seats;

            const cleanType = (window.currentLayoutType || "SEAT").trim().toUpperCase();
            console.log("🔍 [디버깅] 최종 판별된 레이아웃 타입:", cleanType);

            // 🎫 지정석(SEAT) vs 스탠딩(STANDING) 타입별 화면 분기 제어
            if (cleanType.includes("STANDING") || cleanType.startsWith("STAND")) {
                console.log("🚀 스탠딩 타입 확인: 등급별 수량 선택 폼을 렌더링합니다.");
                showQuantitySelectionForm();
            } else {
                console.log("🚀 지정석 타입 확인: 13행 18열 좌석 배치를 시작합니다.");
                seatLayout = seatLayouts.map1;
                renderSeat();
            }
        })
        .catch(err => {
            console.error("🚨 데이터 로드 중 치명적 에러 발생:", err);
            if (seatContainer) {
                seatContainer.innerHTML = `<h3 style="color:red;">데이터 로드 에러: ${err.message}</h3>`;
            }
        });
}

// ==========================
// 공연별 고정 좌석 배치 데이터 (13행 18열)
// ==========================
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

// ==========================================
// 🛠️ 3. 지정석 배치도 렌더링 함수
// ==========================================
function renderSeat() {
    seatContainer.innerHTML = "";

    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";
    stageDiv.style.width = "70%"; stageDiv.style.maxWidth = "500px"; stageDiv.style.height = "40px";
    stageDiv.style.background = "#1e293b"; stageDiv.style.color = "#ffffff"; stageDiv.style.fontSize = "16px";
    stageDiv.style.fontWeight = "bold"; stageDiv.style.display = "flex"; stageDiv.style.alignItems = "center";
    stageDiv.style.justifyContent = "center"; stageDiv.style.margin = "0 auto 40px auto"; stageDiv.style.borderRadius = "4px";
    seatContainer.appendChild(stageDiv);

    const priceList = Object.values(window.concertPriceMap).sort((a, b) => b - a);
    const defaultPrice = priceList[0] || window.defaultSinglePrice || 0;

    seatLayout.forEach((row, rowIndex) => {
        const rowDiv = document.createElement("div");
        rowDiv.style.display = "flex"; rowDiv.style.justifyContent = "center"; rowDiv.style.gap = "4px"; rowDiv.style.marginBottom = "4px";

        let seatColIndex = 0;
        row.forEach((cell) => {
            const seatDiv = document.createElement("div");
            seatDiv.style.width = "30px"; seatDiv.style.height = "30px"; seatDiv.style.borderRadius = "4px";
            seatDiv.style.display = "flex"; seatDiv.style.alignItems = "center"; seatDiv.style.justifyContent = "center"; seatDiv.style.fontSize = "10px";

            if (cell === "N") {
                seatDiv.style.background = "transparent";
                rowDiv.appendChild(seatDiv);
                return;
            }

            const actualRow = rowIndex + 1;
            const actualCol = seatColIndex + 1;
            const seatId = `SEAT_R${actualRow}_C${actualCol}`;

            const foundSeat = window.dbSeatsData.find(s => s.seatId === seatId || (s.seatRow == String(actualRow) && s.seatCol == String(actualCol)));
            let targetSeat = foundSeat;
            if (!targetSeat) {
                targetSeat = { seatClass: "STANDARD", price: defaultPrice, seatStatus: 1 };
            }

            seatDiv.dataset.seatId = seatId;
            seatDiv.title = seatId;
            seatDiv.dataset.selected = "false";
            seatDiv.dataset.seatClass = targetSeat.seatClass || "STANDARD";
            seatDiv.dataset.price = targetSeat.price || defaultPrice;

            if (targetSeat.userNo && currentUserNo && Number(targetSeat.userNo) === currentUserNo) {
                // 내가 선점했던 자리이므로 '선택됨' 상태로 복원해 줍니다.
                seatDiv.dataset.status = "available";
                seatDiv.dataset.selected = "true";
                seatDiv.style.cursor = "pointer";
                seatDiv.style.background = "#1d4ed8"; // 선택된 진한 파란색
                seatDiv.style.border = "1px solid #1e40af";
            }
// 남이 선점해서 닫힌 자리인 경우 (status가 0이거나 유저 번호가 다를 때)
            else if (targetSeat.seatStatus === 0 || targetSeat.seatStatus === "0") {
                seatDiv.dataset.selected = "false";
                seatDiv.dataset.status = "locked";
                seatDiv.style.background = "#d1d5db"; // 회색 잠금
                seatDiv.style.border = "1px solid #9ca3af";
                seatDiv.style.cursor = "not-allowed";
            }
// 아무도 안 잡은 순수 빈 자리인 경우
            else {
                seatDiv.dataset.selected = "false";
                seatDiv.dataset.status = "available";
                seatDiv.style.cursor = "pointer";
                seatDiv.style.background = "#3b82f6"; // 기본 파란색
                seatDiv.style.border = "1px solid #2563eb";
            }

            seatDiv.addEventListener("click", () => {
                if (isConcertClosed) {
                    alert("해당 공연의 좌석 예매가 마감되었습니다.");
                    return;
                }
                if (seatDiv.dataset.status === "locked") {
                    alert("선택할 수 없는 좌석입니다.");
                    return;
                }

                const isSelected = seatDiv.dataset.selected === "true";

                if (!isSelected) {
                    const currentCount = seatContainer.querySelectorAll('[data-selected="true"]').length;
                    if (currentCount >= 4) {
                        alert("좌석은 최대 4개까지 선택 가능합니다.");
                        return;
                    }
                    seatDiv.dataset.selected = "true";
                    seatDiv.style.background = "#1d4ed8"; seatDiv.style.border = "1px solid #1e40af";

                    // 💡 Spring STOMP 양식에 맞춘 선택 이벤트 전송
                    if (stompClient && stompClient.connected) {
                        stompClient.publish({
                            destination: "/app/seat/select",
                            body: JSON.stringify({ concertId: concertId, seatId: seatId, userNo: currentUserNo })
                        });
                    }
                } else {
                    seatDiv.dataset.selected = "false";
                    seatDiv.style.background = "#3b82f6"; seatDiv.style.border = "1px solid #2563eb";

                    // 💡 Spring STOMP 양식에 맞춘 취소 이벤트 전송
                    if (stompClient && stompClient.connected) {
                        stompClient.publish({
                            destination: "/app/seat/cancel",
                            body: JSON.stringify({ concertId: concertId, seatId: seatId, userNo: currentUserNo })
                        });
                    }
                }

                const activeSelectedSeats = seatContainer.querySelectorAll('[data-selected="true"]');
                updateSelectedSeatsUI(activeSelectedSeats);
                calculateAndDisplayTotalPrice(activeSelectedSeats);
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
    legend.style.display = "flex"; legend.style.gap = "20px"; legend.style.justifyContent = "center"; legend.style.marginTop = "16px";
    legend.innerHTML = `
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#3b82f6;border:1px solid #2563eb;border-radius:3px;"></div><span>선택 가능</span></div>
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#1d4ed8;border:1px solid #1e40af;border-radius:3px;"></div><span>선택됨</span></div>
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#d1d5db;border:1px solid #9ca3af;border-radius:3px;"></div><span>선택불가</span></div>
    `;
    seatContainer.appendChild(legend);
}

function updateSelectedSeatsUI(selectedElements) {
    const displayContainer = document.getElementById("selected-seats-display");
    if (!displayContainer) return;
    displayContainer.innerHTML = "";

    if (!selectedElements || selectedElements.length === 0) {
        displayContainer.innerHTML = '<span class="no-seat-msg">좌석을 선택해 주세요. (최대 4개)</span>';
        return;
    }
    selectedElements.forEach(seatEl => {
        const chip = document.createElement("span");
        chip.className = "seat-chip";
        chip.innerText = seatEl.dataset.seatId;
        displayContainer.appendChild(chip);
    });
}

function calculateAndDisplayTotalPrice(selectedElements) {
    let total = 0;
    selectedElements.forEach(el => { total += parseInt(el.dataset.price || 0, 10); });
    const priceDisplay = document.getElementById("total-price-display");
    if (priceDisplay) priceDisplay.innerText = total.toLocaleString() + "원";
    return total;
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
    formWrapper.style.padding = "40px 30px"; formWrapper.style.background = "#ffffff"; formWrapper.style.borderRadius = "12px";
    formWrapper.style.width = "100%"; formWrapper.style.maxWidth = "650px"; formWrapper.style.margin = "40px auto";
    formWrapper.style.boxShadow = "0 10px 25px -5px rgba(0, 0, 0, 0.1)"; formWrapper.style.boxSizing = "border-box";

    formWrapper.innerHTML = `
        <h2 style="margin-bottom: 8px; text-align: center; color: #1e293b; font-size: 24px; font-weight: bold;">티켓 수량 선택</h2>
        <p style="margin-bottom: 35px; text-align: center; color: #64748b; font-size: 14px;">원하시는 티켓의 등급과 수량을 선택해 주세요. (인당 최대 4장)</p>
    `;

    const grades = Object.keys(window.concertPriceMap);

    if (grades.length > 0) {
        grades.forEach(gradeName => {
            const price = window.concertPriceMap[gradeName];
            createQuantityRow(formWrapper, gradeName, price, gradeName);
        });
    } else if (window.isSinglePrice && window.defaultSinglePrice > 0) {
        createQuantityRow(formWrapper, "전석 일반석", window.defaultSinglePrice, "GENERAL");
    } else {
        formWrapper.innerHTML += `<p style="text-align:center; color:#ef4444; font-weight:bold; margin-top:20px;">공연 가격 정보를 읽어오지 못했습니다.</p>`;
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
        <div style="flex: 1; min-width: 0;">
            <div style="font-weight: bold; color: #334155; font-size: 16px;">${label}</div>
            <div style="font-size: 14px; color: #64748b; margin-top: 4px;">${price.toLocaleString()}원</div>
        </div>
        <div style="flex-shrink: 0;">
            <select class="ticket-qty-select" data-grade="${gradeCode}" data-price="${price}" style="padding: 8px 12px; border-radius: 6px; border: 1px solid #cbd5e1; background: #fff; font-size: 15px; font-weight: 500; cursor: pointer;">
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

// ==========================================
// ✈️ 5. 백엔드 가선점 장부 데이터 전송 서버 통신부
// ==========================================
function submitBooking() {
    if (isConcertClosed) {
        alert("해당 공연의 좌석 예매가 마감되었습니다.");
        return;
    }

    const qtySelects = seatContainer.querySelectorAll(".ticket-qty-select");
    const isStanding = qtySelects.length > 0;

    let bookingData = {
        concertId: concertId,
        ticketType: isStanding ? "STANDING" : "SEAT",
        quantities: {},
        selectedSeats: [],
        totalPrice: 0
    };

    if (isStanding) {
        let totalQty = 0;
        let calculatedPrice = 0;

        qtySelects.forEach(select => {
            const qty = parseInt(select.value, 10);
            const price = parseInt(select.dataset.price, 10) || 0;
            if (qty > 0) {
                bookingData.quantities[select.dataset.grade] = qty;
                totalQty += qty;
                calculatedPrice += (qty * price);

                for(let i=0; i<qty; i++) {
                    bookingData.selectedSeats.push(`STANDING_${select.dataset.grade}_${i+1}`);
                }
            }
        });
        if (totalQty === 0) {
            alert("티켓 수량을 1장 이상 선택해 주세요.");
            return;
        }
        bookingData.totalPrice = calculatedPrice;

    } else {
        const activeSelectedSeats = seatContainer.querySelectorAll('[data-selected="true"]');

        if (activeSelectedSeats.length === 0) {
            alert("좌석을 1개 이상 선택해 주세요.");
            return;
        }

        let calculatedPrice = 0;
        activeSelectedSeats.forEach(seatEl => {
            bookingData.selectedSeats.push(seatEl.dataset.seatId);
            calculatedPrice += parseInt(seatEl.dataset.price || 0, 10);
        });

        bookingData.totalPrice = calculatedPrice;
    }

    // ==========================================
    // ✈️ 백엔드 전송 서버 통신부 (Undefined 철통 방어형)
    // ==========================================
    console.log("✈ 백엔드로 전송할 최종 예매 데이터:", bookingData);

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = { "Content-Type": "application/json" };
    if(csrfHeader && csrfToken) { headers[csrfHeader] = csrfToken; }

    fetch("/seat/api/booking/prepare", {
        method: "POST",
        headers: headers,
        body: JSON.stringify(bookingData)
    })
        .then(res => {
            // 서버 에러(500 등) 발생 시 response body의 에러 메시지를 꺼내기 위해 바로 throw하지 않음
            return res.json().then(data => {
                if (!res.ok) {
                    throw new Error(data.message || "서버 예매 임시 등록 실패");
                }
                return data;
            });
        })
        .then(data => {
            console.log("📥 [디버깅] 백엔드 응답 데이터 전체 구조:", JSON.stringify(data));

            // 1. 객체가 비어있거나 실패 메시지가 온 경우 방어
            if (!data || data.message) {
                alert("예매 실패: " + (data.message || "서버에서 빈 데이터를 반환했습니다."));
                return;
            }

            // 2. 백엔드 Map 객체에서 다양한 형태의 키를 안전하게 추출 시도
            // 대소문자 변환 이슈나 데이터 구조 뒤틀림 현상을 전부 방어합니다.
            const finalKey = data.reservationKey || data.ReservationKey || data.bookingId || data.id;

            console.log("🎯 최종 판별된 예약 번호 (finalKey):", finalKey);

            if (finalKey === undefined || finalKey === null || finalKey === "undefined") {
                console.error("🚨 [치명적] 백엔드가 SUCCESS는 줬으나 예약 키를 담아주지 않았습니다. 응답을 다시 확인하세요.");
                alert("서버에서 올바른 예약 번호를 받지 못했습니다. F12 개발자 도구 콘솔을 확인하세요.");
                return;
            }

            alert("좌석이 가선점되었습니다. 결제 단계로 이동합니다.");
            // 🚀 완벽하게 확보된 숫자를 주소창에 넘김
            window.location.href = `/booking/payment?reservationKey=${finalKey}`;
        })
        .catch(err => {
            alert("예매 처리 중 오류가 발생했습니다: " + err.message);
            console.error("🚨 [전송 실패] 에러 디버깅 로그:", err);
        });

    window.addEventListener('pageshow', function(event) {
        // 1. BFCache(뒤로가기 시 브라우저가 이전 화면을 그대로 얼려둔 캐시)로 로드되었는지 확인
        if (event.persisted) {
            console.log("↩️ [BFCache] 뒤로가기 감지: 페이지를 새로고침합니다.");
            window.location.reload();
            return;
        }

        // 2. 최신 웹 표준 API를 사용하여 뒤로가기/앞으로가기(back_forward)인지 확인
        const navigationEntries = performance.getEntriesByType("navigation");
        if (navigationEntries.length > 0) {
            const navType = navigationEntries[0].type;
            if (navType === "back_forward") {
                console.log("↩️ [Performance API] 뒤로가기 이동 감지: 최신 좌석판을 위해 새로고침합니다.");
                window.location.reload();
            }
        }
    });

}