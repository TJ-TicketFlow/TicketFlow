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
if (concertId) {
    fetch(`/seat/api/concert/${concertId}`)
        .then(res => {
            if (!res.ok) throw new Error("공연 정보를 가져오지 못했습니다.");
            return res.json();
        })
        .then(concert => {
            console.log("백엔드에서 받은 공연 데이터:", concert);

            const nameEl = document.getElementById("concert-name");
            const posterEl = document.getElementById("concert-poster");
            const runtimeEl = document.getElementById("concert-runtime");
            const dateEl = document.getElementById("concert-date");

            if (nameEl) nameEl.innerText = concert.concertName;
            if (posterEl) posterEl.src = concert.concertPosterUrl;

            const urlParams = new URLSearchParams(window.location.search);
            const startTime = urlParams.get('sessionId');

            if (runtimeEl && startTime) runtimeEl.innerText = startTime;
            if (dateEl) dateEl.innerText = concert.concertDate;

            // 가격 계산 모듈에 백엔드 가격 문자열 등록
            if (typeof initPriceMap === "function") {
                initPriceMap(concert.concertPriceInfo);
            }
        })
        .catch(err => console.error("공연 정보 로드 에러:", err));
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
        document.getElementById("booking-submit-btn")?.click();
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