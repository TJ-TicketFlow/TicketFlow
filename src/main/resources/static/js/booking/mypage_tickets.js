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

// ==========================================
// 🌟 1. 페이지 로딩 완료 시 날짜 및 탭 활성화 세팅
// ==========================================
document.addEventListener("DOMContentLoaded", function() {
    // 날짜 세팅
    document.getElementById('text-from').textContent = state.from.selected;
    document.getElementById('text-to').textContent = state.to.selected;

    // 현재 주소창(URL)에 있는 status(예매상태) 값을 읽어옵니다. 없으면 '전체'로 간주!
    const urlParams = new URLSearchParams(window.location.search);
    const currentStatus = urlParams.get('status') || '전체';

    // 탭 버튼들을 돌면서, 현재 상태와 이름이 일치하는 버튼에만 파란색(active)을 칠해줍니다.
    document.querySelectorAll('.filter-btn').forEach(btn => {
        const btnText = btn.textContent.trim();
        // HTML버튼명은 '취소/환불'이고 DB조건은 '취소'인 경우를 위해 예외 처리
        if (btnText === currentStatus || (currentStatus === '취소' && btnText === '취소/환불')) {
            btn.classList.add('active');
        } else {
            btn.classList.remove('active');
        }
    });
});


// ==========================================
// 🌟 2. 상태 필터 버튼 로직 (서버로 찐 데이터 요청하기!)
// ==========================================
function setFilter(btn, status) {
    // 현재 달력에 선택된 날짜 가져오기 (날짜 안 날아가게 꽉 쥐기!)
    const startStr = state.from.selected;
    const endStr = state.to.selected;

    // 💡 [핵심] CSS로 숨기는 게 아니라, 서버에 "상태+날짜+1페이지(page=0)" 조건을 달아서 페이지를 새로 이동시킵니다!
    // (스프링부트 Pageable은 보통 0이 1페이지입니다)
    window.location.href = `/mypage/tickets?status=${status}&startDate=${startStr}&endDate=${endStr}&page=0`;
}
// ==========================================
// 🌟 3. 조회 버튼 로직 (날짜 검색 시에도 탭 유지 및 1페이지 리셋)
// ==========================================
function searchByDate(){
    const startStr = state.from.selected;
    const endStr = state.to.selected;

    // 현재 선택된 탭(상태) 가져오기
    const urlParams = new URLSearchParams(window.location.search);
    const currentStatus = urlParams.get('status') || '전체';

    // 💡 [핵심] 날짜를 검색해도 기존 탭 상태를 유지한 채 1페이지(page=0)로 리셋합니다!
    window.location.href = `/mypage/tickets?status=${currentStatus}&startDate=${startStr}&endDate=${endStr}&page=0`;
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