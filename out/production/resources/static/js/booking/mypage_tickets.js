// HTML에서 넘겨받은 serverStartDate, serverEndDate 변수를 그대로 사용합니다.

const [sYear, sMonth, sDay] = serverStartDate.split('-').map(Number);
const [eYear, eMonth, eDay] = serverEndDate.split('-').map(Number);

const state = {
    from: { year: sYear, month: sMonth - 1, selected: serverStartDate },
    to: { year: eYear, month: eMonth - 1, selected: serverEndDate }
};

const today = new Date();
function pad(n){return String(n).padStart(2,'0');}
function formatDate(y,m,d){return`${y}-${pad(m+1)}-${pad(d)}`;}

// 페이지 로딩 완료 시 날짜 글씨 세팅
document.addEventListener("DOMContentLoaded", function() {
    document.getElementById('text-from').textContent = state.from.selected;
    document.getElementById('text-to').textContent = state.to.selected;
});

// 달력 그리기
function renderCalendar(which){
    const s=state[which], popup=document.getElementById('cal-'+which);
    const firstDay=new Date(s.year,s.month,1).getDay(), daysInMonth=new Date(s.year,s.month+1,0).getDate(), daysInPrev=new Date(s.year,s.month,0).getDate();
    const monthNames=['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'];
    let cells='';
    for(let i=firstDay-1;i>=0;i--) cells+=`<div class="cal-cell"><button disabled class="other-month">${daysInPrev-i}</button></div>`;
    for(let d=1;d<=daysInMonth;d++){
        const dateStr=formatDate(s.year,s.month,d);
        const isToday=(s.year===today.getFullYear()&&s.month===today.getMonth()&&d===today.getDate());
        const cls=dateStr===s.selected?'selected':(isToday?'today':'');
        cells+=`<div class="cal-cell"><button class="${cls}" onclick="event.stopPropagation();selectDate('${which}','${dateStr}')">${d}</button></div>`;
    }
    const remain=(firstDay+daysInMonth)%7===0?0:7-((firstDay+daysInMonth)%7);
    for(let d=1;d<=remain;d++) cells+=`<div class="cal-cell"><button disabled class="other-month">${d}</button></div>`;
    popup.innerHTML=`<div class="cal-header"><div class="cal-month-label">${s.year}년 ${monthNames[s.month]}</div><div class="cal-nav"><button onclick="event.stopPropagation();changeMonth('${which}',-1)"><svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="m15 18-6-6 6-6"/></svg></button><button onclick="event.stopPropagation();changeMonth('${which}',1)"><svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="m9 18 6-6-6-6"/></svg></button></div></div><div class="cal-grid"><div class="cal-day-label sun">일</div><div class="cal-day-label">월</div><div class="cal-day-label">화</div><div class="cal-day-label">수</div><div class="cal-day-label">목</div><div class="cal-day-label">금</div><div class="cal-day-label sat">토</div>${cells}</div><div class="cal-footer"><button class="cal-cancel" onclick="event.stopPropagation();closeCalendar('${which}')">Cancel</button><button class="cal-ok" onclick="event.stopPropagation();closeCalendar('${which}')">OK</button></div>`;
}

function toggleCalendar(e,which){
    e.stopPropagation();
    const popup=document.getElementById('cal-'+which), input=document.getElementById('input-'+which), isOpen=popup.classList.contains('show');
    document.querySelectorAll('.calendar-popup').forEach(p=>p.classList.remove('show'));
    document.querySelectorAll('.custom-date-input').forEach(i=>i.classList.remove('open'));
    if(!isOpen){renderCalendar(which);popup.classList.add('show');input.classList.add('open');}
}

function closeCalendar(which){
    document.getElementById('cal-'+which).classList.remove('show');
    document.getElementById('input-'+which).classList.remove('open');
}

function changeMonth(which,delta){
    const s=state[which];s.month+=delta;
    if(s.month<0){s.month=11;s.year--;}
    if(s.month>11){s.month=0;s.year++;}
    renderCalendar(which);
}

function selectDate(which,dateStr){
    state[which].selected=dateStr;
    document.getElementById('text-'+which).textContent=dateStr;
    const[y,m]=dateStr.split('-').map(Number);
    state[which].year=y;state[which].month=m-1;
    renderCalendar(which);
}

// 상태 필터 버튼 로직
// 💡 상태 필터 버튼 로직 (내역이 없을 때 빈 메시지 띄우기 기능 추가!)
function setFilter(btn, status) {
    // 1. 눌린 버튼을 파란색으로 활성화합니다.
    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    // 2. 테이블 데이터를 확인합니다.
    const tbody = document.getElementById('ticket-tbody');
    const rows = tbody.querySelectorAll('tr'); // 테이블 안의 모든 줄을 가져옵니다.
    let visibleCount = 0; // 화면에 보이는 '진짜 데이터'의 개수를 세는 변수입니다.

    rows.forEach(row => {
        // 이미 만들어진 '빈 메시지' 줄(td가 1개인 경우)은 데이터를 셀 때 방해되므로 잠시 숨겨둡니다.
        if (row.id === 'js-empty-row' || row.cells.length === 1) {
            row.style.display = 'none';
            return;
        }

        // 진짜 데이터 줄에서 상태(예매완료, 취소 등) 글자를 가져옵니다.
        const s = row.querySelector('.ticket-status')?.textContent.trim() || '';

        // 필터 조건에 맞는지 확인합니다.
        const isMatch = (status === '전체' || (status === '취소/환불' && s.includes('취소')) || s === status);

        if (isMatch) {
            row.style.display = ''; // 조건에 맞으면 보여줍니다.
            visibleCount++;         // 화면에 보이는 개수를 1개 늘려줍니다!
        } else {
            row.style.display = 'none'; // 조건에 안 맞으면 숨깁니다.
        }
    });

    // 3. 만약 화면에 보이는 데이터가 0개라면? "내역이 없습니다" 메시지 띄우기
    if (visibleCount === 0) {
        let emptyRow = document.getElementById('js-empty-row');

        // 빈 줄이 없다면 자바스크립트로 새로 하나 만들어서 테이블 밑에 찰싹 붙여줍니다.
        if (!emptyRow) {
            emptyRow = document.createElement('tr');
            emptyRow.id = 'js-empty-row';
            emptyRow.innerHTML = '<td colspan="5" style="text-align: center; padding: 30px; color: #666;">해당 상태의 예매 내역이 없습니다.</td>';
            tbody.appendChild(emptyRow);
        }
        emptyRow.style.display = ''; // 만들어진 빈 줄을 화면에 보여줍니다.
    }
}

// 조회 버튼 로직 (서버로 날짜 전송)
function searchByDate(){
    const startStr = state.from.selected;
    const endStr = state.to.selected;
    window.location.href = `/mypage/tickets?startDate=${startStr}&endDate=${endStr}`;
}

// 초기화 버튼 로직
function resetDate(){
    window.location.href = `/mypage/tickets`;
}

// 화면 빈 곳 클릭 시 달력 닫기
document.addEventListener('click',function(){
    document.querySelectorAll('.calendar-popup').forEach(p=>p.classList.remove('show'));
    document.querySelectorAll('.custom-date-input').forEach(i=>i.classList.remove('open'));
});