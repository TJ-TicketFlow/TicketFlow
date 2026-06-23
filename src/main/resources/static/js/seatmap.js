// ==========================
// 공연 ID 및 전역 설정
// ==========================
console.log("seatmap.js 실행됨");

const seatContainer = document.getElementById("seat-container");
const pathSegments = window.location.pathname.split('/');
const concertId = pathSegments[pathSegments.length - 1];

let seatLayout;
let vipRows = [0, 1, 2, 3]; // 차등가일 때 VIP로 지정할 행 번호 (0부터 시작)

// ==========================
// 🌟 공연 정보 동적 불러오기
// ==========================
// 🟢 seatmap.js 수정 방향 스케치

// 1. 먼저 공연 정보를 불러옵니다.
if (concertId) {
    fetch(`/seat/api/concert/${concertId}`)
        .then(res => res.json())
        .then(concert => {
            // [여기가 중요!] 단일가 판별 및 가격 세팅을 완벽히 끝냅니다.
            if (typeof initPriceMap === "function") {
                initPriceMap(concert.concertPriceInfo);
            }

            // 🌟 가격 세팅이 완전히 끝난 "이 시점"에 레이아웃을 불러오라고 강제합니다.
            return fetch(`/seat/layout/${concertId}`);
        })
        .then(res => res.text())
        .then(type => {
            const cleanType = type.trim();
            console.log("공연 레이아웃 타입:", cleanType);

            // 이제는 무조건 isSinglePrice가 세팅된 상태이므로 올바르게 그려집니다!
            if (cleanType === "SEAT" || cleanType === "SEAT_A") {
                seatLayout = seatLayouts.map1;
                renderSeat();
            } else if (cleanType === "SEAT_B") {
                seatLayout = seatLayouts.map2;
                renderSeat();
            } else if (cleanType === "STANDING") {
                showQuantitySelectionForm();
            }
        })
        .catch(err => console.error("데이터 로드 에러:", err));
}

// ==========================
// 공연별 고정 좌석 배치 데이터
// ==========================
const seatLayouts = {
    map1: [
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['R','R','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','R','R','R','R','R','A','A','A','N','A','A','A','A','A','A','A','A'],
        ['A','R','R','R','R','A','A','A','A','N','R','A','R','R','R','R','A','A'],
        ['N','N','R','A','A','A','A','A','A','N','A','A','A','A','R','A','N','N'],
        ['N','N','R','A','A','A','A','A','A','N','A','A','A','A','A','A','N','N'],
        ['N','N','R','R','R','R','A','A','A','N','A','R','R','R','R','R','A','N'],
        ['N','N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N']
    ],
    map2: [
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','R','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','R','R','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','R','R','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','R','R','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A','A']
    ]
};

// ==========================
// 레이아웃 조건 분기 및 로드
// ==========================
if (concertId) {
    fetch(`/seat/layout/${concertId}`)
        .then(res => res.text())
        .then(type => {
            const cleanType = type.trim();
            console.log("공연 레이아웃 타입:", cleanType);

            if (cleanType === "SEAT" || cleanType === "SEAT_A") {
                seatLayout = seatLayouts.map1;
                renderSeat();
            } else if (cleanType === "SEAT_B") {
                seatLayout = seatLayouts.map2;
                renderSeat();
            } else if (cleanType === "STANDING") {
                // 🌟 비지정석(스탠딩)일 때 수량 선택 폼 로드
                showQuantitySelectionForm();
            } else {
                seatContainer.innerHTML = "<h3>알 수 없는 레이아웃 타입입니다.</h3>";
            }
        })
        .catch(err => console.error("레이아웃 로드 에러:", err));
}

// ==========================
// 지정석 바둑판 렌더링 함수
// ==========================
function renderSeat() {
    seatContainer.innerHTML = "";

    // 무대 그리기
    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";
    stageDiv.style.width = "70%"; stageDiv.style.maxWidth = "500px"; stageDiv.style.height = "40px";
    stageDiv.style.background = "#1e293b"; stageDiv.style.color = "#ffffff"; stageDiv.style.fontSize = "16px";
    stageDiv.style.fontWeight = "bold"; stageDiv.style.display = "flex"; stageDiv.style.alignItems = "center";
    stageDiv.style.justifyContent = "center"; stageDiv.style.margin = "0 auto 40px auto"; stageDiv.style.borderRadius = "4px";
    seatContainer.appendChild(stageDiv);

    // 좌석 배열 배치
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

            // 🌟 [보완] 단일가(isSinglePrice)일 땐 vipRows 무시하고 무조건 GENERAL 처리
            if (typeof isSinglePrice !== "undefined" && isSinglePrice) {
                seatDiv.dataset.seatClass = "GENERAL";
            } else {
                seatDiv.dataset.seatClass = vipRows.includes(rowIndex) ? "VIP" : "GENERAL";
            }

            // 색상 부여
            if (seatDiv.dataset.seatClass === "VIP") {
                seatDiv.style.background = "#facc15"; seatDiv.style.border = "1px solid #ca8a04";
            } else {
                seatDiv.style.background = "#4ade80"; seatDiv.style.border = "1px solid #16a34a";
            }

            // 클릭 이벤트
            seatDiv.addEventListener("click", () => {
                const isSelected = seatDiv.dataset.selected === "true";

                if (!isSelected) {
                    const currentCount = seatContainer.querySelectorAll('[data-selected="true"]').length;
                    if (currentCount >= 4) {
                        alert("좌석은 최대 4개까지 선택 가능합니다.");
                        return;
                    }
                    seatDiv.dataset.selected = "true";
                    seatDiv.style.background = "#3b82f6"; seatDiv.style.border = "1px solid #2563eb";
                } else {
                    seatDiv.dataset.selected = "false";
                    if (seatDiv.dataset.seatClass === "VIP") {
                        seatDiv.style.background = "#facc15"; seatDiv.style.border = "1px solid #ca8a04";
                    } else {
                        seatDiv.style.background = "#4ade80"; seatDiv.style.border = "1px solid #16a34a";
                    }
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
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#4ade80;border:1px solid #16a34a;border-radius:3px;"></div><span>선택 가능</span></div>
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#3b82f6;border:1px solid #2563eb;border-radius:3px;"></div><span>선택됨</span></div>
        <div style="display:flex;align-items:center;gap:6px;"><div style="width:20px;height:20px;background:#d1d5db;border:1px solid #9ca3af;border-radius:3px;"></div><span>예매완료</span></div>
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
// 🌟 [신설 및 통합] 티켓 장수 선택형 UI 렌더링 영역
// ===================================================
// ===================================================
// 🌟 [레이아웃 완전 분리] 티켓 장수 선택형 UI 렌더링 영역
// ===================================================
function showQuantitySelectionForm() {
    // 1️⃣ 오른쪽 사이드바(지정석용) 찾아내서 통째로 숨기기
    const rightSidebar = document.querySelector(".right-sidebar");
    if (rightSidebar) {
        rightSidebar.style.display = "none";
    }

    // 2️⃣ 중앙 좌석 페이지 영역을 화면 전체(100%) 넓이로 확장
    const seatPage = document.querySelector(".seat-page");
    if (seatPage) {
        seatPage.style.width = "100%";
        seatPage.style.flex = "1";
    }

    // 3️⃣ 기존 배치도 영역 청소 후 수량 선택 카드 배치
    seatContainer.innerHTML = "";

    const formWrapper = document.createElement("div");
    formWrapper.style.padding = "40px";
    formWrapper.style.background = "#ffffff";
    formWrapper.style.borderRadius = "12px";
    formWrapper.style.maxWidth = "500px";
    formWrapper.style.margin = "60px auto";
    formWrapper.style.boxShadow = "0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)";

    formWrapper.innerHTML = `
        <h2 style="margin-bottom: 8px; text-align: center; color: #1e293b; font-size: 22px;">티켓 수량 선택</h2>
        <p style="margin-bottom: 32px; text-align: center; color: #64748b; font-size: 14px;">원하시는 티켓의 수량을 선택해 주세요. (인당 최대 4장)</p>
    `;

    // 4️⃣ 단일가/차등가에 따라 입력 행 생성
    if (typeof isSinglePrice !== "undefined" && isSinglePrice) {
        createQuantityRow(formWrapper, "일반석", defaultSinglePrice, "GENERAL");
    } else if (typeof concertPriceMap !== "undefined") {
        Object.keys(concertPriceMap).forEach(grade => {
            createQuantityRow(formWrapper, `${grade}석`, concertPriceMap[grade], grade);
        });
    }

    // 5️⃣ 하단에 결제 금액 및 예매하기 버튼을 이 카드 안으로 이사시키기
    const totalBox = document.createElement("div");
    totalBox.style.marginTop = "30px";
    totalBox.style.paddingTop = "20px";
    totalBox.style.borderTop = "2px dashed #e2e8f0";
    totalBox.style.display = "flex";
    totalBox.style.justifyContent = "space-between";
    totalBox.style.alignItems = "center";
    totalBox.innerHTML = `
        <span style="font-weight: bold; color: #475569; font-size: 16px;">총 결제 금액</span>
        <span id="standing-total-price" style="font-weight: bold; color: #3b82f6; font-size: 24px;">0원</span>
    `;
    formWrapper.appendChild(totalBox);

    const submitBtn = document.createElement("button");
    submitBtn.innerText = "예매하기";
    submitBtn.style.width = "100%";
    submitBtn.style.marginTop = "24px";
    submitBtn.style.padding = "14px";
    submitBtn.style.background = "#10b981"; // 초록색 버튼
    submitBtn.style.color = "#fff";
    submitBtn.style.border = "none";
    submitBtn.style.borderRadius = "6px";
    submitBtn.style.fontSize = "16px";
    submitBtn.style.fontWeight = "bold";
    submitBtn.style.cursor = "pointer";

    submitBtn.addEventListener("click", () => {
        // 기존의 booking-submit-btn과 똑같은 제출 로직을 수행하도록 바인딩
        submitBooking();
    });

    formWrapper.appendChild(submitBtn);
    seatContainer.appendChild(formWrapper);
}

function createQuantityRow(container, label, price, gradeCode) {
    const row = document.createElement("div");
    row.style.display = "flex"; row.style.justifyContent = "space-between";
    row.style.alignItems = "center"; row.style.marginBottom = "20px"; row.style.paddingBottom = "16px";
    row.style.borderBottom = "1px solid #f1f5f9";

    row.innerHTML = `
        <div>
            <div style="font-weight: bold; color: #334155; font-size: 16px;">${label}</div>
            <div style="font-size: 14px; color: #64748b; margin-top: 4px;">${price.toLocaleString()}원</div>
        </div>
        <select class="ticket-qty-select" data-grade="${gradeCode}" data-price="${price}" style="padding: 8px 16px; border-radius: 6px; border: 1px solid #cbd5e1; background: #fff; font-size: 15px; font-weight: 500;">
            <option value="0">0장</option> <option value="1">1장</option>
            <option value="2">2장</option> <option value="3">3장</option> <option value="4">4장</option>
        </select>
    `;

    const selectEl = row.querySelector(".ticket-qty-select");
    selectEl.addEventListener("change", (e) => {
        handleQuantityChange(e.target);
    });

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

    // 🌟 카드 내부에 새로 만든 금액창에 실시간 반영
    const standingPriceEl = document.getElementById("standing-total-price");
    if (standingPriceEl) {
        standingPriceEl.innerText = totalPrice.toLocaleString() + "원";
    }
}


// ===================================================
// 🌟 [신설] 최종 선택 데이터 수집 및 결제/예매 요청 전송
// ===================================================
function submitBooking() {
    let bookingData = {
        concertId: concertId,
        ticketType: ""
    };

    // 1. 현재 어떤 레이아웃(모드)인지 판별하여 데이터 수집
    const activeSelectedSeats = seatContainer.querySelectorAll('[data-selected="true"]');
    const qtySelects = seatContainer.querySelectorAll(".ticket-qty-select");

    if (activeSelectedSeats.length > 0) {
        // A. 지정석 모드 데이터 수집
        bookingData.ticketType = "SEAT";
        bookingData.selectedSeats = Array.from(activeSelectedSeats).map(el => el.dataset.seatId);
    } else if (qtySelects.length > 0) {
        // B. 수량 선택형(스탠딩) 모드 데이터 수집
        bookingData.ticketType = "STANDING";
        bookingData.quantities = {};

        let totalQty = 0;
        qtySelects.forEach(select => {
            const qty = parseInt(select.value, 10);
            if (qty > 0) {
                bookingData.quantities[select.dataset.grade] = qty;
                totalQty += qty;
            }
        });

        if (totalQty === 0) {
            alert("티켓 수량을 1장 이상 선택해 주세요.");
            return;
        }
    } else {
        alert("선택된 좌석이나 티켓 수량이 없습니다.");
        return;
    }

    // 콘솔창에서 가공된 데이터 눈으로 확인하기
    console.log("✈️ 백엔드로 전송할 최종 예매 데이터:", bookingData);

    // 2. 백엔드 컨트롤러로 데이터 전송 (Fetch API 예시)
    // 실제 결제 페이지로 이동하거나 백엔드 세션에 임시 저장을 요청합니다.

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');


    fetch("/seat/api/booking/prepare", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            // 🌟 CSRF 토큰 열쇠 추가
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(bookingData)
    })
        .then(res => {
            if (!res.ok) throw new Error("예매 정보 등록에 실패했습니다.");
            return res.json(); // 보통 예매 임시 ID 등을 반환받음
        })
        .then(result => {
            // 성공 시 결제 페이지(예: /payment)로 페이지 이동
            // window.location.href = `/payment?bookingId=${result.bookingId}`;
            alert("데이터 전송 성공! 결제 페이지 연동부로 진입합니다.");
        })
        .catch(err => {
            console.error("예매 전송 에러:", err);
            alert("예매 처리 중 오류가 발생했습니다.");
        });
}