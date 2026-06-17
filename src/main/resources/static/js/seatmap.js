// ==========================
// 공연 ID 가져오기
// ==========================

console.log("seatmap.js 실행됨");

const seatContainer = document.getElementById("seat-container");

console.log("받은 concertId:", concertId);

// ==========================
// 공연별 좌석 배치 선택
// ==========================

let seatLayout;

if (concertId === "PF277688") {

    seatLayout = [
        ["N","N","N","N","N","N","N","N","A","A","A","A","A","A","N","N"],
        ["A","A","A","N","N","A","A","N","A","A","A","N","A","A"],
        ["A","A","A","A","A","A","A","A","A","A","A","A","A","A"],
        ["A","A","A","A","A","A","A","A","A","A","A","A","A","A"],
        ["A","A","A","A","A","A","A","A","A","A","A","A","A","A"],
        ["A","A","A","A","A","A","A","A","A","A","A","A","A","A"],
        ["A","A","A","A","A","A","A","A","A","A","A","A","A","A"]
    ];

} else if (concertId === "2") {

    seatLayout = [
        ['A','A','A','A','A','A','A','A','N','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','N','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','N','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','N','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','N','A','A','A','A'],
        ['A','A','A','A','A','A','A','A','N','A','A','A','A'],
        ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','N','N'],
        ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','N','N'],
        ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','A','N'],
        ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','N'],
        ['N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','N']
    ];

} else {
    alert("좌석 배치 정보가 없습니다.");
    seatLayout = [];
}

// ==========================
// 좌석 렌더링
// ==========================

// 선택된 좌석 목록
const selectedSeats = [];

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
        seatDiv.style.cursor = cell === "A" ? "pointer" : "default";

        if (cell === "N") {
            // 빈 공간
            seatDiv.style.background = "transparent";
        } else {
            // 선택 가능한 좌석
            const seatId = `R${rowIndex + 1}C${seatColIndex + 1}`;
            seatDiv.dataset.seatId = seatId;
            seatDiv.style.background = "#4ade80";
            seatDiv.style.border = "1px solid #16a34a";
            seatDiv.title = seatId;

            seatDiv.addEventListener("click", () => {
                const idx = selectedSeats.indexOf(seatId);
                if (idx === -1) {
                    // 선택
                    selectedSeats.push(seatId);
                    seatDiv.style.background = "#f59e0b";
                    seatDiv.style.border = "1px solid #d97706";
                } else {
                    // 선택 해제
                    selectedSeats.splice(idx, 1);
                    seatDiv.style.background = "#4ade80";
                    seatDiv.style.border = "1px solid #16a34a";
                }
                console.log("선택된 좌석:", selectedSeats);
            });

            seatColIndex++;
        }

        rowDiv.appendChild(seatDiv);
    });

    seatContainer.appendChild(rowDiv);
});

// ==========================
// 범례
// ==========================

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
        <div style="width:20px;height:20px;background:#f59e0b;border:1px solid #d97706;border-radius:3px;"></div>
        <span>선택됨</span>
    </div>
    <div style="display:flex;align-items:center;gap:6px;">
        <div style="width:20px;height:20px;background:#d1d5db;border:1px solid #9ca3af;border-radius:3px;"></div>
        <span>예매완료</span>
    </div>
`;
seatContainer.appendChild(legend);