const layout = [

    // 1
    ['A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A'],

    // 2
    ['A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],

    // 3
    ['A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],

    // 4
    ['A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],

    // 5
    ['A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],

    // 6
    ['A','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','A'],

    // 7
    ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','A','N','N'],

    // 8
    ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','A','N','N'],

    // 9
    ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],

    // 10
    ['N','N','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],

    // 11
    ['N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],

    // 12
    ['N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N'],

    // 13
    ['N','A','A','A','A','A','A','A','N','A','A','A','A','A','A','A','N']

];

const seatMap = document.getElementById("seatMap");

layout.forEach((row,rowIndex)=>{

    const rowDiv = document.createElement("div");
    rowDiv.classList.add("row");

    row.forEach((type,colIndex)=>{

        if(type==="N"){
            const empty = document.createElement("div");
            empty.className="empty";
            rowDiv.appendChild(empty);
            return;
        }

        const seat = document.createElement("div");
        seat.className="seat";

        seat.dataset.seatNo =
            `${rowIndex+1}-${colIndex+1}`;

        seat.title = seat.dataset.seatNo;

        // 여기 부분 교체
        seat.addEventListener("click", () => {

            const isSelected =
                seat.classList.contains("selected");

            const selectedCount =
                document.querySelectorAll(".seat.selected").length;

            // 선택 해제
            if (isSelected) {
                seat.classList.remove("selected");
                return;
            }

            // 최대 4석 제한
            if (selectedCount >= 4) {
                alert("최대 4석까지만 선택 가능합니다.");
                return;
            }

            // 선택
            seat.classList.add("selected");

            console.log(
                "좌석:",
                seat.dataset.seatNo
            );

        });

        rowDiv.appendChild(seat);

    });
    seatMap.appendChild(rowDiv);
});