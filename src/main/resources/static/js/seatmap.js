// ==========================================
// 🔌 1. Socket.io 실시간 클라이언트 설정 (Netty 서버 연동)
// ==========================================
// Spring Boot Netty-SocketIO 서버가 열려있는 9092 포트로 연결합니다.
const socket = io("http://localhost:9092");

// ==========================================
// 공연 ID 및 전역 설정
// ==========================================
console.log("seatmap.js 실행됨 (실시간 소켓 활성화)");

const seatContainer = document.getElementById("seat-container");
const pathSegments = window.location.pathname.split('/');
const concertId = pathSegments[pathSegments.length - 1];

let seatLayout;
let vipRows = []; // 기본 VIP 행 지정

// 전역 변수 초기화 (let 없이 기존 변수 활용)
isSinglePrice = false;
defaultSinglePrice = 0;
concertPriceMap = {};

// 페이지 로드 시 백엔드 소켓 서버의 해당 공연 전용 'Room'에 입장
if (concertId) {
    socket.emit('join_concert', concertId);
}

// ==========================================
// 📥 2. 서버로부터 수신하는 실시간 소켓 이벤트 리스너
// ==========================================

// [이벤트 A] 타인이 좌석을 선택(선점)했을 때 -> 해당 좌석을 회색으로 잠금
socket.on('seat_selected_by_other', (data) => {
    console.log(`🔒 타인이 좌석 선점함: ${data.seatId}`);
    const targetSeat = document.querySelector(`[data-seat-id="${data.seatId}"]`);
    if (targetSeat) {
        targetSeat.style.background = "#d1d5db"; // 회색 잠금 색상
        targetSeat.style.border = "1px solid #9ca3af";
        targetSeat.style.cursor = "not-allowed";
        targetSeat.dataset.status = "locked"; // 클릭 차단용 상태 심기
    }
});

// [이벤트 B] 타인이 선택했던 좌석을 해제(취소)했을 때 -> 다시 예매 가능하게 복구
socket.on('seat_cancelled_by_other', (data) => {
    console.log(`🔓 타인이 좌석 해제함: ${data.seatId}`);
    const targetSeat = document.querySelector(`[data-seat-id="${data.seatId}"]`);
    if (targetSeat) {
        targetSeat.dataset.status = "available"; // 상태 복구
        targetSeat.style.cursor = "pointer";

        // 등급별 원래 색상으로 롤백
        if (targetSeat.dataset.seatClass === "VIP") {
            targetSeat.style.background = "#facc15";
            targetSeat.style.border = "1px solid #ca8a04";
        } else {
            targetSeat.style.background = "#3b82f6"; // 기본 파란색
            targetSeat.style.border = "1px solid #2563eb";
        }
    }
});


// ==========================================
// 🌟 공연 정보 & 레이아웃 순서 제어 (기존 fetch 유지)
// ==========================================
if (concertId) {
    fetch(`/seat/api/concert/${concertId}`)
        .then(res => res.json())
        .then(concert => {
            console.log("✈️ 백엔드에서 받은 원본 공연 데이터:", concert);

            // HTML 화면 바인딩
            const nameEl = document.getElementById("concert-name");
            const posterEl = document.getElementById("concert-poster");
            const runtimeEl = document.getElementById("concert-runtime");
            const dateEl = document.getElementById("concert-date");

            if (nameEl) nameEl.innerText = concert.concertName;
            if (posterEl) posterEl.src = concert.concertPosterUrl;
            if (dateEl) dateEl.innerText = concert.concertDate;
            if (runtimeEl) runtimeEl.innerText = concert.concertTime || concert.concertRuntime;

            // 가격 정보 파싱 로직
            const priceInfo = concert.concertPriceInfo;
            concertPriceMap = {};
            isSinglePrice = false;

            if (priceInfo && priceInfo.trim() !== "") {
                const cleanPriceInfo = priceInfo.trim();
                const pairs = cleanPriceInfo.includes(",") ? cleanPriceInfo.split(",") : [cleanPriceInfo];

                pairs.forEach(pair => {
                    const trimmedPair = pair.trim();
                    const priceMatch = trimmedPair.match(/[\d,]+원?$/);

                    if (priceMatch) {
                        const priceStr = priceMatch[0];
                        let cleanPrice = parseInt(priceStr.replace(/,/g, "").replace("원", ""), 10);

                        if (!isNaN(cleanPrice) && cleanPrice > 0 && cleanPrice < 1000) {
                            cleanPrice = cleanPrice * 1000;
                        }

                        let gradeName = trimmedPair.replace(priceStr, "").trim();
                        gradeName = gradeName.replace(/석석/g, "석");
                        if (gradeName.endsWith("석")) {
                            gradeName = gradeName.slice(0, -1);
                        }

                        if (gradeName && !isNaN(cleanPrice)) {
                            concertPriceMap[gradeName] = cleanPrice;
                        }
                    }
                });
            }

            return fetch(`/seat/layout/${concertId}`);
        })
        .then(res => res.text())
        .then(type => {
            const cleanType = type.trim();
            if (cleanType === "SEAT" || cleanType === "SEAT_A") {
                seatLayout = seatLayouts.map1;
                renderSeat();
            } else if (cleanType === "SEAT_B" || cleanType === "STANDING") {
                showQuantitySelectionForm();
            } else {
                seatContainer.innerHTML = "<h3>알 수 없는 레이아웃 타입입니다.</h3>";
            }
        })
        .catch(err => {
            console.error("🚨 데이터 로드 중 치명적 에러 발생:", err);
        });
}
// ==========================
// 공연별 고정 좌석 배치 데이터
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

function renderSeat() {
    seatContainer.innerHTML = "";

    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";
    stageDiv.style.width = "70%"; stageDiv.style.maxWidth = "500px"; stageDiv.style.height = "40px";
    stageDiv.style.background = "#1e293b"; stageDiv.style.color = "#ffffff"; stageDiv.style.fontSize = "16px";
    stageDiv.style.fontWeight = "bold"; stageDiv.style.display = "flex"; stageDiv.style.alignItems = "center";
    stageDiv.style.justifyContent = "center"; stageDiv.style.margin = "0 auto 40px auto"; stageDiv.style.borderRadius = "4px";
    seatContainer.appendChild(stageDiv);

    const priceList = Object.values(concertPriceMap).sort((a, b) => b - a);
    const defaultPrice = priceList[0] || 0;

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

            const seatId = `R${rowIndex + 1}C${seatColIndex + 1}`;
            seatDiv.dataset.seatId = seatId;
            seatDiv.title = seatId;
            seatDiv.style.cursor = "pointer";
            seatDiv.dataset.selected = "false";
            seatDiv.dataset.status = "available"; // 초기 상태 설정

            let seatPrice = defaultPrice;
            if (cell === 'R' || vipRows.includes(rowIndex)) {
                seatDiv.dataset.seatClass = "VIP";
                seatPrice = priceList[0] || defaultPrice;
            } else {
                seatDiv.dataset.seatClass = "GENERAL";
                seatPrice = priceList[1] || priceList[0] || defaultPrice;
            }

            seatDiv.dataset.price = seatPrice;

            // 등급별 기본 색상 부여
            if (seatDiv.dataset.seatClass === "VIP") {
                seatDiv.style.background = "#facc15"; seatDiv.style.border = "1px solid #ca8a04";
            } else {
                seatDiv.style.background = "#3b82f6"; seatDiv.style.border = "1px solid #2563eb";
            }

            // 🖱️ [수정] 좌석 클릭 이벤트 핸들러 내부 소켓 연동
            seatDiv.addEventListener("click", () => {
                // 🚨 다른 유저가 선점 중인 좌석이면 클릭 자체를 원천 차단
                if (seatDiv.dataset.status === "locked") {
                    alert("다른 유저가 선택 중인 좌석입니다.");
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
                    seatDiv.style.background = "#1d4ed8"; seatDiv.style.border = "1px solid #1e40af"; // 내가 선택한 색상(진한파랑)

                    // 📤 [소켓 전송] "나 이 좌석 찜했어!" 라고 서버에 브로드캐스트 요청
                    socket.emit('seat_select', {
                        concertId: concertId,
                        seatId: seatId
                    });
                } else {
                    seatDiv.dataset.selected = "false";
                    if (seatDiv.dataset.seatClass === "VIP") {
                        seatDiv.style.background = "#facc15"; seatDiv.style.border = "1px solid #ca8a04";
                    } else {
                        seatDiv.style.background = "#3b82f6"; seatDiv.style.border = "1px solid #2563eb";
                    }

                    // 📤 [소켓 전송] "나 이 좌석 선택 취소할게!" 라고 서버에 브로드캐스트 요청
                    socket.emit('seat_cancel', {
                        concertId: concertId,
                        seatId: seatId
                    });
                }

                const activeSelectedSeats = seatContainer.querySelectorAll('[data-selected="true"]');
                updateSelectedSeatsUI(activeSelectedSeats);
                if (typeof calculateAndDisplayTotalPrice === "function") {
                    calculateAndDisplayTotalPrice(activeSelectedSeats);
                }
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
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#d1d5db;border:1px solid #9ca3af;border-radius:3px;"></div><span>선택불가(타인선점)</span></div>
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

// ===================================================
// 🌟 티켓 장수 선택형 UI 렌더링 영역 (스탠딩 & SEAT_B 공용)
// ===================================================
function showQuantitySelectionForm() {
    const rightSidebar = document.querySelector(".right-sidebar");
    if (rightSidebar) rightSidebar.style.display = "none";

    const seatPage = document.querySelector(".seat-page");
    if (seatPage) {
        seatPage.style.width = "100%";
        seatPage.style.flex = "1";
    }

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
        <p style="margin-bottom: 35px; text-align: center; color: #64748b; font-size: 14px;">원하시는 티켓의 수량을 선택해 주세요. (인당 최대 4장)</p>
    `;

    let finalPriceMap = {};

    if (typeof concertPriceMap !== "undefined" && Object.keys(concertPriceMap).length > 0) {
        Object.keys(concertPriceMap).forEach(key => {
            let currentPrice = concertPriceMap[key];

            let cleanKey = key.replace(/석석/g, "석");
            if (cleanKey.endsWith("석")) {
                cleanKey = cleanKey.slice(0, -1);
            }

            finalPriceMap[cleanKey] = currentPrice;
        });
    }

    if (Object.keys(finalPriceMap).length > 0) {
        Object.keys(finalPriceMap).forEach(gradeName => {
            const price = finalPriceMap[gradeName];
            createQuantityRow(formWrapper, `${gradeName}석`, price, gradeName);
        });
    } else if (isSinglePrice && defaultSinglePrice > 0) {
        createQuantityRow(formWrapper, "전석 일반석", defaultSinglePrice, "GENERAL");
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

    // 🌟 예매하기 버튼 색상을 #3b82f6 으로 변경 완료
    submitBtn.style.background = "#3b82f6";

    submitBtn.style.color = "#fff"; submitBtn.style.border = "none";
    submitBtn.style.borderRadius = "6px"; submitBtn.style.fontSize = "16px"; submitBtn.style.fontWeight = "bold";
    submitBtn.style.cursor = "pointer";

    submitBtn.addEventListener("click", () => { submitBooking(); });
    formWrapper.appendChild(submitBtn);

    seatContainer.appendChild(formWrapper);
}

function createQuantityRow(container, label, price, gradeCode) {
    const row = document.createElement("div");
    row.style.display = "flex";
    row.style.justifyContent = "space-between";
    row.style.alignItems = "center";
    row.style.marginBottom = "20px";
    row.style.paddingBottom = "16px";
    row.style.borderBottom = "1px solid #f1f5f9";
    row.style.gap = "16px";

    row.innerHTML = `
        <div style="flex: 1; min-width: 0;">
            <div style="font-weight: bold; color: #334155; font-size: 16px;">
                ${label}
            </div>
            <div style="font-size: 14px; color: #64748b; margin-top: 4px;">
                ${price.toLocaleString()}원 
            </div>
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

    selects.forEach(select => {
        totalSelectedTickets += parseInt(select.value, 10);
    });

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
    if (standingPriceEl) {
        standingPriceEl.innerText = totalPrice.toLocaleString() + "원";
    }
}

// ===================================================
// 🌟 지정석(바둑판) 선택 좌석 실시간 금액 계산 함수 (값 반환형)
// ===================================================

// ===================================================
// 🌟 최종 선택 데이터 수집 및 결제/예매 요청 전송 (금액 주입완료)
// ===================================================
function submitBooking() {
    let bookingData = {
        concertId: concertId,
        ticketType: "",
        totalPrice: 0 // 🌟 DTO 매핑용 totalPrice 추가
    };

    const activeSelectedSeats = seatContainer.querySelectorAll('[data-selected="true"]');
    const qtySelects = seatContainer.querySelectorAll(".ticket-qty-select");

    if (activeSelectedSeats.length > 0) {
        bookingData.ticketType = "SEAT";
        bookingData.selectedSeats = Array.from(activeSelectedSeats).map(el => el.dataset.seatId);

        // return 받은 좌석 총 가격을 주입합니다.
        bookingData.totalPrice = calculateAndDisplayTotalPrice(activeSelectedSeats);

    } else if (qtySelects.length > 0) {
        bookingData.ticketType = "STANDING";
        bookingData.quantities = {};

        let totalQty = 0;
        let calculatedPrice = 0;

        qtySelects.forEach(select => {
            const qty = parseInt(select.value, 10);
            const price = parseInt(select.dataset.price, 10) || 0;
            if (qty > 0) {
                bookingData.quantities[select.dataset.grade] = qty;
                totalQty += qty;
                calculatedPrice += (qty * price);
            }
        });

        if (totalQty === 0) {
            alert("티켓 수량을 1장 이상 선택해 주세요.");
            return;
        }

        // 수량 폼 합산 가격을 주입합니다.
        bookingData.totalPrice = calculatedPrice;
    } else {
        alert("선택된 좌석이나 티켓 수량이 없습니다.");
        return;
    }

    console.log("✈ nighttime 백엔드로 전송할 최종 예매 데이터:", bookingData);

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    fetch("/seat/api/booking/prepare", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(bookingData)
    })
        .then(res => {
            if (!res.ok) throw new Error("예매 정보 등록에 실패했습니다.");
            return res.json();
        })
        .then(result => {
            alert("데이터 전송 성공! 결제 페이지 연동부로 진입합니다.");
        })
        .catch(err => {
            console.error("예매 전송 에러:", err);
            alert("예매 처리 중 오류가 발생했습니다.");
        });
}