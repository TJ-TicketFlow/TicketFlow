// ==========================
// 공연 ID 가져오기
// ==========================


console.log("seatmap.js 실행됨");

const seatContainer = document.getElementById("seat-container");

const pathSegments = window.location.pathname.split('/');
const concertId = pathSegments[pathSegments.length - 1];

console.log("받은 concertId:", concertId);


// ==========================
// 🌟 [추가] 공연 정보 동적 불러오기
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
            const dateEl = document.getElementById("concert-date");       // 💡 추가

            // 안전장치: 태그가 제대로 존재할 때만 데이터를 쏙쏙 매핑합니다.
            if (nameEl) nameEl.innerText = concert.concertName;
            if (posterEl) posterEl.src = concert.concertPosterUrl;

            const urlParams = new URLSearchParams(window.location.search);
            const selectedDate = urlParams.get('date');      // 예: "2026-06-25" 추출
            const startTime = urlParams.get('sessionId'); // "19:00" 획득

            if (runtimeEl && startTime) {
                runtimeEl.innerText = startTime; // 19:00이 화면에 꽂힙니다.
            }
            if (dateEl) dateEl.innerText = concert.concertDate;
        })
        .catch(err => console.error("공연 정보 로드 에러:", err));
}



const urlParams = new URLSearchParams(window.location.search);

const selectedDate = urlParams.get('date');      // "2026-06-25" 획득!
const selectedTime = urlParams.get('sessionId'); // "19:00" 획득!

console.log("사용자가 선택한 날짜:", selectedDate);
console.log("사용자가 선택한 시간:", selectedTime);
// ==========================
// 공연별 좌석 배치 선택
// ==========================

let seatLayout;
let vipRows = [];
// let selectedSeats = []; 임시주석처리
const seatLayouts={
    map1 : [[
        'A','A','A','A','A','A','A','A','A',
        'N',
        'A','A','A','A','A','A','A','N'
    ],


        // 2행
        [
            'A','A','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','A'
        ],


        // 3행
        [
            'R','R','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','A'
        ],


        // 4행
        [
            'A','A','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','A'
        ],


        // 5행
        [
            'A','R','R','R','R','R','A','A','A',
            'N',
            'A','A','A','A','A','A','A','A'
        ],


        // 6행
        [
            'A','R','R','R','R','A','A','A','A',
            'N',
            'R','A','R','R','R','R','A','A'
        ],


        // 7행
        [
            'N','N','R','A','A','A','A','A','A',
            'N',
            'A','A','A','A','R','A','N','N'
        ],


        // 8행
        [
            'N','N','R','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','N','N'
        ],


        // 9행
        [
            'N','N','R','R','R','R','A','A','A',
            'N',
            'A','R','R','R','R','R','A','N'
        ],


        // 10행
        [
            'N','N','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','N'
        ],


        // 11행
        [
            'N','A','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','N'
        ],


        // 12행
        [
            'N','A','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','N'
        ],


        // 13행
        [
            'N','A','A','A','A','A','A','A','A',
            'N',
            'A','A','A','A','A','A','A','N'
        ]],


   map2: [

       // 1열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 2열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 3열
       [
           'A','A','R','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 4열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','R','R','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 5열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 6열
       [
           'A','A','A','A','A',
           'A','A','R','R','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 7열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 8열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','R','R','A','A',
           'A','A','A','A','A'
       ],


       // 9열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 10열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 11열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 12열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 13열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ],


       // 14열
       [
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A',
           'A','A','A','A','A'
       ]

   ]};







// ==========================
// 좌석 렌더링
// ==========================

// 선택된 좌석 목록
const selectedSeats = [];

fetch(`/seat/layout/${concertId}`)
    .then(res => res.text())

    .then(type => {

        console.log("공연 타입:", type);

        const cleanType = type.trim();

        if (cleanType === "SEAT" || cleanType === "SEAT_A") {
            seatLayout = seatLayouts.map1;
        }
        else if (cleanType === "SEAT_B") {
            seatLayout = seatLayouts.map2;
        }
        else if (cleanType === "STANDING") {
            showStandingOrder();
            return;
        }
        else {
            console.error("매칭되는 레이아웃이 없습니다:", cleanType);
            seatContainer.innerHTML = "<h3>알 수 없는 공연 타입입니다.</h3>";
            return;
        }

        renderSeat(); // 이제 seatLayout이 정상 할당되어 에러가 안 납니다!
    })
    .catch(err => console.error("레이아웃 로드 에러:", err));



// ==========================
// 좌석 렌더링 함수 (🔥 구조 변경)
// ==========================

function renderSeat() {

    seatContainer.innerHTML = ""; // 초기화

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

            // VIP / 일반
            if (vipRows.includes(rowIndex)) {
                seatDiv.style.background = "#facc15";
                seatDiv.style.border = "1px solid #ca8a04";
            } else {
                seatDiv.style.background = "#4ade80";
                seatDiv.style.border = "1px solid #16a34a";
            }

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
            });

            seatColIndex++;
            rowDiv.appendChild(seatDiv);
        });

        seatContainer.appendChild(rowDiv);
    });

    renderLegend();
}updateSelectedSeatsUI();

// ==========================
// standing 처리 (임시)
// ==========================

function showStandingOrder() {
    seatContainer.innerHTML = "<h3>스탠딩 공연입니다. 번호 순서로 입장합니다.</h3>";
}

// ==========================
// 범례
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
function updateSelectedSeatsUI() {
    const displayContainer = document.getElementById("selected-seats-display");
    if (!displayContainer) return;

    // 초기화
    displayContainer.innerHTML = "";

    if (selectedSeats.length === 0) {
        displayContainer.innerHTML = '<span class="no-seat-msg">좌석을 선택해 주세요. (최대 4개)</span>';
        return;
    }

    // 선택된 좌석 배열(selectedSeats)을 돌면서 예쁜 칩 형태로 화면에 추가
    selectedSeats.forEach(seatId => {
        const chip = document.createElement("span");
        chip.className = "seat-chip";
        chip.innerText = seatId; // 예: "R1C5" 형식 출력 (행/열을 가독성 있게 바꾸셔도 좋습니다)
        displayContainer.appendChild(chip);
    });
}