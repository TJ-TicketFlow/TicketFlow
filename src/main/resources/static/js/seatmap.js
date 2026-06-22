// ==========================
// 공연 ID 가져오기
// ==========================
console.log("seatmap.js 실행됨");
const seatContainer = document.getElementById("seat-container");
const pathSegments = window.location.pathname.split('/');
const concertId = pathSegments[pathSegments.length - 1];
console.log("받은 concertId:", concertId);

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
        })
        .catch(err => console.error("공연 정보 로드 에러:", err));
}

const urlParams = new URLSearchParams(window.location.search);
const selectedDate = urlParams.get('date');
const selectedTime = urlParams.get('sessionId');

// ==========================
// 공연별 좌석 배치 데이터
// ==========================
let seatLayout;
let vipRows = []; // 필요시 특정 행 인덱스를 넣으시면 VIP 처리가 활성화됩니다.

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
// 레이아웃 조건 처리 및 로드
// ==========================
fetch(`/seat/layout/${concertId}`)
    .then(res => res.text())
    .then(type => {
        console.log("공연 타입:", type);
        const cleanType = type.trim();

        if (cleanType === "SEAT" || cleanType === "SEAT_A") {
            seatLayout = seatLayouts.map1;
        } else if (cleanType === "SEAT_B") {
            seatLayout = seatLayouts.map2;
        } else if (cleanType === "STANDING") {
            showStandingOrder();
            return;
        } else {
            console.error("매칭되는 레이아웃이 없습니다:", cleanType);
            seatContainer.innerHTML = "<h3>알 수 없는 공연 타입입니다.</h3>";
            return;
        }
        renderSeat();
    })
    .catch(err => console.error("레이아웃 로드 에러:", err));

// ==========================
// 좌석 렌더링 함수
// ==========================
function renderSeat() {
    seatContainer.innerHTML = "";

    // 무대(STAGE) 생성 및 디자인 스타일링
    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";
    stageDiv.style.width = "70%";
    stageDiv.style.maxWidth = "500px";
    stageDiv.style.height = "40px";
    stageDiv.style.background = "#1e293b";
    stageDiv.style.color = "#ffffff";
    stageDiv.style.fontSize = "16px";
    stageDiv.style.fontWeight = "bold";
    stageDiv.style.display = "flex";
    stageDiv.style.alignItems = "center";
    stageDiv.style.justifyContent = "center";
    stageDiv.style.margin = "0 auto 40px auto";
    stageDiv.style.borderRadius = "4px";
    stageDiv.style.boxShadow = "0 2px 4px rgba(0,0,0,0.1)";
    seatContainer.appendChild(stageDiv);

    seatLayout.forEach((row, rowIndex) => {
        const rowDiv = document.createElement("div");
        rowDiv.style.display = "flex";
        rowDiv.style.justifyContent = "center";
        rowDiv.style.gap = "4px";
        rowDiv.style.marginBottom = "4px";

        let seatColIndex = 0;

        row.forEach((cell) => {
            const seatDiv = document.createElement("div");
            seatDiv.style.width = "30px";
            seatDiv.style.height = "30px";
            seatDiv.style.borderRadius = "4px";
            seatDiv.style.display = "flex";
            seatDiv.style.alignItems = "center";
            seatDiv.style.justifyContent = "center";
            seatDiv.style.fontSize = "10px";

            if (cell === "N") {
                seatDiv.style.background = "transparent";
                rowDiv.appendChild(seatDiv);
                return;
            }

            const seatId = `R${rowIndex + 1}C${seatColIndex + 1}`;
            seatDiv.dataset.seatId = seatId;
            seatDiv.title = seatId;
            seatDiv.style.cursor = "pointer";

            // 🌟 [Data Store] 선택 상태(false) 및 기본 좌석 클래스(VIP/일반) 저장
            seatDiv.dataset.selected = "false";
            seatDiv.dataset.seatClass = vipRows.includes(rowIndex) ? "VIP" : "GENERAL";

            // 기본 배경색 설정
            if (seatDiv.dataset.seatClass === "VIP") {
                seatDiv.style.background = "#facc15";
                seatDiv.style.border = "1px solid #ca8a04";
            } else {
                seatDiv.style.background = "#4ade80";
                seatDiv.style.border = "1px solid #16a34a";
            }

            // 클릭 이벤트 리스너 변경 (Data Store 기반)
            seatDiv.addEventListener("click", () => {
                const isSelected = seatDiv.dataset.selected === "true";

                if (!isSelected) {
                    // 🌟 실시간으로 선택된 좌석 개수를 DOM에서 직접 쿼리하여 체크
                    const currentSelectedCount = seatContainer.querySelectorAll('[data-selected="true"]').length;
                    if (currentSelectedCount >= 4) {
                        alert("좌석은 최대 4개까지 선택 가능합니다.");
                        return;
                    }
                    // 상태 변경 및 UI 색상 갱신
                    seatDiv.dataset.selected = "true";
                    seatDiv.style.background = "#3b82f6";
                    seatDiv.style.border = "1px solid #2563eb";
                } else {
                    // 선택 해제 상태로 복구
                    seatDiv.dataset.selected = "false";
                    if (seatDiv.dataset.seatClass === "VIP") {
                        seatDiv.style.background = "#facc15";
                        seatDiv.style.border = "1px solid #ca8a04";
                    } else {
                        seatDiv.style.background = "#4ade80";
                        seatDiv.style.border = "1px solid #16a34a";
                    }
                }

                // 🌟 현재 상태 콘솔 로그 출력 테스트
                const allSelected = Array.from(seatContainer.querySelectorAll('[data-selected="true"]')).map(el => el.dataset.seatId);
                console.log("선택된 좌석(DOM 스냅샷):", allSelected);

                updateSelectedSeatsUI();
            });

            seatColIndex++;
            rowDiv.appendChild(seatDiv);
        });

        seatContainer.appendChild(rowDiv);
    });

    renderLegend();
}

// ==========================
// standing 처리 (임시)
// ==========================
function showStandingOrder() {
    seatContainer.innerHTML = "<h3>스탠딩 공연입니다. 번호 순서로 입장합니다.</h3>";
}

// ==========================
// 범례 렌더링
// ==========================
function renderLegend() {
    const legend = document.createElement("div");
    legend.style.display = "flex";
    legend.style.gap = "20px";
    legend.style.justifyContent = "center";
    legend.style.marginTop = "16px";
    legend.innerHTML = `
        <div style="display:flex;align-items:center;gap:6px;">
            <div style="width:20px;height:20px;background:#4ade80;border:1px solid #16a34a;border-radius:3px;"></div>
            <span>선택 가능</span>
        </div>
        <div style="display:flex;align-items:center;gap:6px;">
            <div style="width:20px;height:20px;background:#3b82f6;border:1px solid #2563eb;border-radius:3px;"></div>
            <span>선택됨</span>
        </div>
        <div style="display:flex;align-items:center;gap:6px;">
            <div style="width:20px;height:20px;background:#d1d5db;border:1px solid #9ca3af;border-radius:3px;"></div>
            <span>예매완료</span>
        </div>
    `;
    seatContainer.appendChild(legend);
}

// ==========================
// 🌟 [Data Store 변경] 오른쪽 팝업 화면 갱신 함수
// ==========================
function updateSelectedSeatsUI() {
    const displayContainer = document.getElementById("selected-seats-display");
    if (!displayContainer) return;

    displayContainer.innerHTML = "";

    // 🌟 DOM 저장소에서 'data-selected="true"' 인 엘리먼트들을 긁어옴
    const selectedElements = seatContainer.querySelectorAll('[data-selected="true"]');

    if (selectedElements.length === 0) {
        displayContainer.innerHTML = '<span class="no-seat-msg">좌석을 선택해 주세요. (최대 4개)</span>';
        return;
    }

    selectedElements.forEach(seatEl => {
        const chip = document.createElement("span");
        chip.className = "seat-chip";
        chip.innerText = seatEl.dataset.seatId; // 엘리먼트에 저장된 ID 추출
        displayContainer.appendChild(chip);
    });
}