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

            // HTML 태그들을 안전하게 변수에 먼저 담습니다.
            const nameEl = document.getElementById("concert-name");
            const posterEl = document.getElementById("concert-poster");
            const runtimeEl = document.getElementById("concert-runtime");
            const dateEl = document.getElementById("concert-date");

            // 안전장치: 태그가 제대로 존재할 때만 데이터를 쏙쏙 매핑합니다.
            if (nameEl) nameEl.innerText = concert.concertName;
            if (posterEl) posterEl.src = concert.concertPosterUrl;

            const urlParams = new URLSearchParams(window.location.search);
            const selectedDate = urlParams.get('date');
            const startTime = urlParams.get('sessionId');

            if (runtimeEl && startTime) {
                runtimeEl.innerText = startTime;
            }
            if (dateEl) dateEl.innerText = concert.concertDate;
        })
        .catch(err => console.error("공연 정보 로드 에러:", err));
}

const urlParams = new URLSearchParams(window.location.search);
const selectedDate = urlParams.get('date');
const selectedTime = urlParams.get('sessionId');

console.log("사용자가 선택한 날짜:", selectedDate);
console.log("사용자가 선택한 시간:", selectedTime);


// ==========================
// 공연별 좌석 배치 데이터
// ==========================
let seatLayout;
let vipRows = [];
const selectedSeats = []; // 선택된 좌석 목록 배열

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
    seatContainer.innerHTML = ""; // 초기화


    // ==========================================
    // 🌟 [추가] 좌석 배치도 최상단에 무대(STAGE) 추가
    // ==========================================
    const stageDiv = document.createElement("div");
    stageDiv.innerText = "STAGE";

    // 무대 디자인 스타일링
    stageDiv.style.width = "70%";            /* 좌석 전체 너비의 70% 정도 차지 */
    stageDiv.style.maxWidth = "500px";       /* 너무 거대해지는 것 방지 */
    stageDiv.style.height = "40px";          /* 무대 높이(두께) */
    stageDiv.style.background = "#1e293b";   /* 어두운 네이비색 무대 배경 */
    stageDiv.style.color = "#ffffff";        /* 글자색 (흰색) */
    stageDiv.style.fontSize = "16px";
    stageDiv.style.fontWeight = "bold";
    stageDiv.style.display = "flex";
    stageDiv.style.alignItems = "center";
    stageDiv.style.justifyContent = "center";
    stageDiv.style.margin = "0 auto 40px auto"; /* 가운데 정렬 및 아래 좌석들과 40px 간격 둠 */
    stageDiv.style.borderRadius = "4px";
    stageDiv.style.boxShadow = "0 2px 4px rgba(0,0,0,0.1)";

    // 좌석 상자에 무대 먼저 얹기
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

            // VIP / 일반 색상 설정
            if (vipRows.includes(rowIndex)) {
                seatDiv.style.background = "#facc15";
                seatDiv.style.border = "1px solid #ca8a04";
            } else {
                seatDiv.style.background = "#4ade80";
                seatDiv.style.border = "1px solid #16a34a";
            }

            // 클릭 이벤트 리스너 추가
            seatDiv.addEventListener("click", () => {
                const idx = selectedSeats.indexOf(seatId);

                if (idx === -1) {
                    if (selectedSeats.length >= 4) {
                        alert("좌석은 최대 4개까지 선택 가능합니다.");
                        return;
                    }
                    selectedSeats.push(seatId);
                    seatDiv.style.background = "#3b82f6";
                    seatDiv.style.border = "1px solid #2563eb";
                } else {
                    selectedSeats.splice(idx, 1);
                    if (vipRows.includes(rowIndex)) {
                        seatDiv.style.background = "#facc15";
                        seatDiv.style.border = "1px solid #ca8a04";
                    } else {
                        seatDiv.style.background = "#4ade80";
                        seatDiv.style.border = "1px solid #16a34a";
                    }
                }

                console.log("선택된 좌석:", selectedSeats);
                updateSelectedSeatsUI(); // ⭕ 정상 호출
            }); // click 이벤트 종료

            seatColIndex++;
            rowDiv.appendChild(seatDiv);
        }); // row.forEach 종료

        seatContainer.appendChild(rowDiv);
    }); // seatLayout.forEach 종료

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
// 🌟 [안전하게 바깥으로 탈출!] 오른쪽 팝업 화면 갱신 함수
// ==========================
function updateSelectedSeatsUI() {
    const displayContainer = document.getElementById("selected-seats-display");
    if (!displayContainer) return;

    displayContainer.innerHTML = "";

    if (selectedSeats.length === 0) {
        displayContainer.innerHTML = '<span class="no-seat-msg">좌석을 선택해 주세요. (최대 4개)</span>';
        return;
    }

    selectedSeats.forEach(seatId => {
        const chip = document.createElement("span");
        chip.className = "seat-chip";
        chip.innerText = seatId;
        displayContainer.appendChild(chip);
    });
}