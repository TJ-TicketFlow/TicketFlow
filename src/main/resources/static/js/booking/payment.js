// 결제창 내의 [이전 단계로] 버튼에 달아줄 함수
// 결제창으로 넘어가는 중인지 확인하는 스위치
let preserveSeat = false;

const reservationMeta = document.querySelector("meta[name='reservation_key']");
const realReservationKey = reservationMeta ? Number(reservationMeta.getAttribute("content")) : 0;

function sendReleaseRequest() {
    if (realReservationKey === 0) return Promise.resolve(); // 예약 번호가 없으면 실행 안 함

    // 1. 보안 토큰(CSRF)을 챙깁니다.
    const csrfMeta = document.querySelector("meta[name='_csrf']");
    const csrfToken = csrfMeta ? csrfMeta.getAttribute("content") : '';

    // 2. 백엔드가 @RequestBody로 받을 수 있도록 JSON 데이터를 캡슐(Blob)로 예쁘게 포장합니다.
    const data = JSON.stringify({ reservationKey: realReservationKey });
    const blob = new Blob([data], { type: 'application/json' });

    // 3. 🌟 [핵심 우회 기법] 헤더를 못 쓰는 비콘을 위해 주소 끝에 ?_csrf=토큰 값을 몰래 달아줍니다.
    // 스프링 시큐리티는 헤더에 토큰이 없으면 주소창에서 토큰을 찾아내므로 완벽하게 통과됩니다!
    const url = '/booking/release-seat?_csrf=' + csrfToken;

    // 4. 브라우저가 죽는 순간 발동하는 가장 강력한 최후의 통신 수단 (sendBeacon)
    if (navigator.sendBeacon) {
        navigator.sendBeacon(url, blob);
    } else {
        // 비콘을 지원하지 않는 아주 오래된 구형 브라우저를 위한 보험 (기존 fetch 로직)
        const csrfHeaderMeta = document.querySelector("meta[name='_csrf_header']");
        const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute("content") : 'X-CSRF-TOKEN';
        const headers = { 'Content-Type': 'application/json' };
        if (csrfHeader && csrfToken) { headers[csrfHeader] = csrfToken; }

        fetch('/booking/release-seat', {
            method: 'POST',
            headers: headers,
            body: data,
            keepalive: true
        }).catch(err => console.error("좌석 해제 요청 실패", err));
    }

    return Promise.resolve();
}

// [수정] 서버가 좌석을 실제로 풀어줄 때까지 기다렸다가 뒤로갑니다.
// 응답이 오기 전에 history.back()이 먼저 실행되면, 좌석선택 페이지가
// 새로고침되면서 아직 안 풀린(잠긴) 좌석 상태를 그대로 받아와 버리는 문제가 있었습니다.
// 혹시 응답이 늦어져도 최대 1.5초만 기다리고 강제로 진행합니다(무한 대기 방지).
function goBackToSeats() { // 🌟 이름 끝에 's'를 지워서 HTML 버튼과 맞췄습니다!

    // 1. 서버에 "좌석 풀어줘!" 요청을 보냅니다.
    sendReleaseRequest().finally(() => {

        // 2. DB가 안전하게 저장을 끝낼 수 있도록 딱 0.5초(500ms)만 기다려줍니다.
        setTimeout(() => {
            // 창 닫힘 이벤트가 또 실행되지 않도록 스위치를 켭니다.
            preserveSeat = true;

            // 브라우저 캐시 버그 방지를 위해 이전 페이지로 '새로고침'하며 돌아갑니다!
            if (document.referrer) {
                window.location.href = document.referrer;
            } else {
                history.back();
            }
        }, 500);

    });
}

// ==========================================
// 1. 카카오 우편번호 검색 팝업 기능
// ==========================================
function openAddressSearch() {
    new daum.Postcode({
        oncomplete: function(data) {
            let fullAddress = data.roadAddress;
            if (data.userSelectedType === 'J') {
                fullAddress = data.jibunAddress;
            }

            const zipCodeInput = document.querySelector('input[name="zipCode"]');
            const addressBaseInput = document.querySelector('input[name="addressBase"]');
            const addressDetailInput = document.querySelector('input[name="addressDetail"]');

            if (zipCodeInput) zipCodeInput.value = data.zonecode;
            if (addressBaseInput) addressBaseInput.value = fullAddress;

            if (addressDetailInput) addressDetailInput.focus();
        }
    }).open();
}

// ==========================================
// 2. 화면이 다 켜진 후 실행될 구역
// ==========================================
document.addEventListener("DOMContentLoaded", function() {

    const remainingMeta = document.querySelector("meta[name='remaining_seconds']");
    let timeLeft = remainingMeta ? Number(remainingMeta.getAttribute("content")) : 30 * 60;

    const timerDisplay = document.getElementById('countdownTimer');

    // 1. 시간을 예쁘게 포장해서 화면에 그리는 작업을 '함수'로 따로 빼냅니다.
    function renderTimer() {
        // 혹시 시간이 마이너스면 0으로 고정
        if (timeLeft <= 0) timeLeft = 0;

        const minutes = Math.floor(timeLeft / 60);
        const seconds = timeLeft % 60;
        const minutesString = String(minutes).padStart(2, '0');
        const secondsString = String(seconds).padStart(2, '0');

        if (timerDisplay) {
            timerDisplay.textContent = minutesString + ":" + secondsString;
        }
    }

    // 2. setInterval이 1초 멍 때리기 전에, 화면이 켜지자마자 즉시 1번 그려버립니다!
    renderTimer();

    // 3. 그 직후부터 1초마다 남은 시간을 줄이며 똑딱거리게 만듭니다.
    const timerInterval = setInterval(function() {
        timeLeft--; // 1초를 먼저 빼고

        // 시간이 다 되었을 때
        if (timeLeft <= 0) {
            clearInterval(timerInterval);
            renderTimer(); // 화면에 00:00을 확실히 찍어줍니다.

            // 0.1초 정도 여유를 주어 00:00이 화면에 보인 직후 알림창을 띄웁니다.
            setTimeout(() => {
                if (window.LemonSqueezy) {
                    window.LemonSqueezy.Url.Close();
                }
                alert("결제 대기 시간이 초과되었습니다. 메인 화면으로 돌아갑니다.");
                preserveSeat = true;
                sendReleaseRequest();
                window.location.href = '/';
            }, 100);
            return;
        }

        // 남은 시간 화면에 다시 그리기
        renderTimer();
    }, 1000);

    // [기초 HTML 상자들 가져오기]
    const memberMeta = document.querySelector("meta[name='is_member']");
    const isMember = memberMeta ? (memberMeta.getAttribute("content") === 'true') : false;
    const couponContainer = document.getElementById('couponContainer');
    const deliveryRadios = document.querySelectorAll('input[name="deliveryType"]'); // ✨ 이름표는 여기서 딱 한 번만 선언!
    const deliveryInfoSection = document.getElementById('deliveryInfoSection'); // ✨ 배송지 구역 상자

    const deliveryFeeDisplay = document.getElementById('deliveryFeeDisplay');
    const totalPriceDisplay = document.getElementById('totalPriceDisplay');
    const finalPriceDisplay = document.getElementById('finalPriceDisplay');
    const membershipDiscountDisplay = document.getElementById('membershipDiscount');
    const couponDiscountDisplay = document.getElementById('couponDiscount');
    const totalDiscountDisplay = document.getElementById('totalDiscount');

    const ticketPriceElement = document.getElementById('TicketPrice');
    const feeElement = document.getElementById('fee');

    const TicketPrice = ticketPriceElement ? Number(ticketPriceElement.textContent.replace(/,/g,"")) : 0;
    const fee = feeElement ? Number(feeElement.textContent.replace(/,/g,"")) : 2000;

    if (TicketPrice === 0) {
        console.error("티켓 가격 정보를 정상적으로 불러오지 못했습니다.");
        // 필요하다면 여기서 alert("잘못된 접근입니다.") 후 페이지를 뒤로 보낼 수도 있습니다.
    }

    // ==========================================
    // 기능 함수들 (쿠폰 불러오기, 계산기, 숨김마법사)
    // ==========================================

    // 쿠폰 목록 가져오기
    function loadCoupons() {
        if (!couponContainer) return;

        fetch('/booking/checkcoupons')
            .then(response => response.json())
            .then(coupons => {
                couponContainer.innerHTML = ''; // "불러오는 중..." 지우기

                // 만약 서버에서 가져온 쿠폰이 하나도 없다면?
                if (!coupons || coupons.length === 0) {
                    couponContainer.innerHTML = '<span style="font-size:13px; color:#666; margin-left: 5px;">보유하신 쿠폰이 없습니다.</span>';
                    calculateFinalPrice(); // 쿠폰 0원 기준으로 가격 계산 한 번 돌려주기
                    return; // 여기서 함수를 끝냅니다!
                }

                // 쿠폰이 존재할 경우에만 아래 로직이 실행됩니다.
                let htmlString = `
                    <div class="radio-box">
                        <label><input type="radio" name="couponRate" value="0" checked> 쿠폰 적용 안 함</label>
                    </div>
                `;

                coupons.forEach(coupon => {
                    htmlString += `
                        <div class="radio-box">
                            <label>
                                <input type="radio" name="couponRate" value="${coupon.couponDiscountRate}" data-id="${coupon.userCouponId}"> 
                                ${coupon.name}
                            </label>
                        </div>
                    `;
                });

                couponContainer.innerHTML = htmlString;

                // 새 라디오 버튼에 계산기 이벤트 달아주기
                const couponRadios = document.querySelectorAll('input[name="couponRate"]');
                couponRadios.forEach(radio => {
                    radio.addEventListener('change', calculateFinalPrice);
                });

                // 그리기 완료 후 계산기 한 번 실행
                calculateFinalPrice();
            })
            .catch(error => {
                console.error("쿠폰을 불러오는데 실패했습니다.", error);
                couponContainer.innerHTML = '<span style="color:red; font-size:13px; margin-left: 5px;">쿠폰 정보를 확인할 수 없습니다.</span>';
            });
    }

    // 결제 가격 계산기
    function calculateFinalPrice() {
        const checkedDelivery = document.querySelector('input[name="deliveryType"]:checked');
        let deliveryFee = 0;
        if (checkedDelivery && checkedDelivery.value === 'POST') {
            deliveryFee = 3000;
        }

        let membershipDiscountAmt = 0;
        if (isMember) {
            membershipDiscountAmt = TicketPrice * 0.03;
        }

        let couponDiscountAmt = 0;
        const checkedCoupon = document.querySelector('input[name="couponRate"]:checked');
        if (checkedCoupon) {
            const discountRate = Number(checkedCoupon.value);
            couponDiscountAmt = TicketPrice * (discountRate / 100);
        }

        const newTotalPrice = TicketPrice + deliveryFee + fee;
        const totalDiscountAmt = couponDiscountAmt + membershipDiscountAmt;
        const newFinalPrice = newTotalPrice - totalDiscountAmt;

        if (deliveryFeeDisplay) deliveryFeeDisplay.textContent = deliveryFee.toLocaleString();
        if (membershipDiscountDisplay) membershipDiscountDisplay.textContent = membershipDiscountAmt.toLocaleString();
        if (couponDiscountDisplay) couponDiscountDisplay.textContent = couponDiscountAmt.toLocaleString();
        if (totalDiscountDisplay) totalDiscountDisplay.textContent = totalDiscountAmt.toLocaleString();
        if (totalPriceDisplay) totalPriceDisplay.textContent = newTotalPrice.toLocaleString();
        if (finalPriceDisplay) finalPriceDisplay.textContent = newFinalPrice.toLocaleString();
    }

    // 배송지 정보 표시/숨김 마법사
    function toggleDeliveryInfo() {
        if (!deliveryInfoSection) return;

        const checkedDelivery = document.querySelector('input[name="deliveryType"]:checked');

        if (checkedDelivery && checkedDelivery.value === 'MOBILE') {
            deliveryInfoSection.style.display = 'none'; // 모바일이면 숨김
        } else {
            deliveryInfoSection.style.display = 'block'; // 택배면 보여줌
        }
    }


    // ==========================================
    // 감시자(Event Listener) 설정 및 최초 실행
    // ==========================================
    deliveryRadios.forEach(radio => {
        radio.addEventListener('change', calculateFinalPrice);
        radio.addEventListener('change', toggleDeliveryInfo);
    });

    // 최초 1회 실행하여 초기 화면 세팅
    toggleDeliveryInfo();
    loadCoupons();

    // ==========================================
    // 1. 네이버 캡차 불러오기 함수 (새로 추가)
    // ==========================================
    function loadCaptcha() {
        fetch('/booking/captcha-key')
            .then(response => response.json())
            .then(data => {
                // 서버가 준 열쇠와 이미지 주소를 HTML에 세팅합니다.
                document.getElementById('captchaKey').value = data.key;
                document.getElementById('captchaImage').src = data.imageUrl;
                document.getElementById('captchaInput').value = ''; // 입력창 비우기
            })
            .catch(error => console.error("캡차 로딩 실패:", error));
    }

    // 화면 켜지자마자 캡차 한번 불러오기 (loadCoupons(); 밑에 추가)
    loadCaptcha();

    const captchaInputEl = document.getElementById('captchaInput');
    const captchaWarning = document.getElementById('captchaWarning');

    if (captchaInputEl) {
        captchaInputEl.addEventListener('input', function(e) {
            const currentVal = e.target.value;

            // 1. 입력된 글자 중에 한글(자음, 모음, 완성형)이 단 1개라도 섞여 있는지 검사합니다.
            const hasKorean = /[ㄱ-ㅎ|ㅏ-ㅣ|가-힣]/.test(currentVal);

            if (hasKorean) {
                // 한글이 감지되면 숨겨뒀던 빨간 경고창을 짠! 하고 보여줍니다.
                if (captchaWarning) captchaWarning.style.display = 'block';
            } else {
                // 영어로 다시 잘 치고 있으면 경고창을 자연스럽게 숨깁니다.
                if (captchaWarning) captchaWarning.style.display = 'none';
            }

            // 2. 입력된 한글 및 특수문자를 실시간으로 싹 지우고 영문 대소문자와 숫자만 입력창에 남깁니다.
            e.target.value = currentVal.replace(/[^A-Za-z0-9]/g, "");
        });
    }
    // 새로고침 버튼 누르면 다시 불러오기
    const refreshBtn = document.getElementById('refreshCaptcha');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadCaptcha);
    }

    // ==========================================
    // 결제하기 버튼 클릭 이벤트
    // ==========================================
    const payButton = document.getElementById('createPayment');

    if (payButton) {
        payButton.addEventListener('click', function() {
            const captchaKey = document.getElementById('captchaKey').value;
            const captchaInput = document.getElementById('captchaInput').value;
            const buyerName = document.querySelector('input[name="buyerName"]').value;
            const buyerEmail = document.querySelector('input[name="buyerEmail"]').value;
            const buyerPhone = document.querySelector('input[name="buyerPhone"]').value;
            const receiverName = document.querySelector('input[name="receiverName"]').value;
            const receiverPhone = document.querySelector('input[name="receiverPhone"]').value;

            const zipCode = document.querySelector('input[name="zipCode"]').value;
            const addressBase = document.querySelector('input[name="addressBase"]').value;
            const addressDetail = document.querySelector('input[name="addressDetail"]').value;

            const payName = document.getElementById('payNameDisplay').textContent;
            const payAmount = Number(finalPriceDisplay.textContent.replace(/,/g, ""));

            const checkedCoupon = document.querySelector('input[name="couponRate"]:checked');
            const usedCouponId = (checkedCoupon && checkedCoupon.value !== "0") ? checkedCoupon.getAttribute('data-id') : null;

            // 유효성 검사
            if(buyerPhone === "") {
                alert("구매자의 휴대폰 번호를 입력해주세요!");
                return;
            }

            // 방어 로직! 입력 안 했으면 막습니다.
            if (captchaInput.trim() === "") {
                alert("자동주문 방지 글자를 입력해주세요!");
                return;
            }

            const checkedDelivery = document.querySelector('input[name="deliveryType"]:checked');

            // 여기에 정리 로직 추가!
            let finalReceiverName = receiverName;
            let finalReceiverPhone = receiverPhone;
            let finalZipCode = zipCode;
            let finalAddr = addressBase + " " + addressDetail;

            // 만약 모바일 티켓이라면? 데이터를 강제로 비워버립니다.
            if (checkedDelivery && checkedDelivery.value === 'MOBILE') {
                finalReceiverName = null;
                finalReceiverPhone = null;
                finalZipCode = null;
                finalAddr = null;
            }

            if (checkedDelivery && checkedDelivery.value === 'POST') {
                if(receiverName === "" || receiverPhone === "" || addressDetail === "") {
                    alert("택배 수령을 위한 배송지 정보(이름, 연락처, 상세주소)를 모두 채워주세요!");
                    return;
                }
            }

            // [로딩 애니메이션 켜기]
            const loadingOverlay = document.getElementById('loadingOverlay');
            if (loadingOverlay) loadingOverlay.style.display = 'flex';
            payButton.disabled = true;
            payButton.textContent = '결제 준비 중...';

            // CSRF 토큰 설정
            const csrfMeta = document.querySelector("meta[name='_csrf']");
            const csrfHeaderMeta = document.querySelector("meta[name='_csrf_header']");
            const csrfToken = csrfMeta ? csrfMeta.getAttribute("content") : '';
            const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute("content") : 'X-CSRF-TOKEN';

            const requestData = {
                reservationKey: realReservationKey,
                payName: payName,
                payAmount: payAmount,
                buyerName: buyerName,
                buyerEmail: buyerEmail,
                payDelName: finalReceiverName,
                payDelCall: finalReceiverPhone,
                userCouponId: usedCouponId,
                payDelPostcode: finalZipCode,
                payDelAddr: finalAddr,
                captchaKey: captchaKey,
                captchaValue: captchaInput
            };

            const headers = { 'Content-Type': 'application/json' };
            if (csrfHeader && csrfToken) { headers[csrfHeader] = csrfToken; }

            // 서버 전송
            fetch('/booking/create', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(requestData)
            })
                .then(async response => {
                    // [핵심 수정] 서버가 400(캡차 틀림) 에러를 보내면 글자를 끄집어냅니다!
                    if (!response.ok) {
                        const errorText = await response.text();
                        throw new Error(errorText || '서버 통신 실패');
                    }
                    return response.text();
                })
                .then(checkoutUrl => {
                    preserveSeat = true;
                    if (window.LemonSqueezy) {
                        // 스크립트가 잘 있으면 팝업창(모달)으로 예쁘게 띄우기!
                        window.LemonSqueezy.Url.Open(checkoutUrl);
                    } else {
                        // 만약 통신 오류나 광고차단기로 스크립트가 막혔다면?
                        // 에러 띄우지 말고 예전처럼 안전하게 다음 페이지로 보내주기 (보험)
                        window.location.href = checkoutUrl;
                    }
                })
                .catch(error => {
                    console.error('결제 준비 중 오류 발생:', error);

                    // 로딩 끄고 버튼 원래대로 복구
                    const loadingOverlay = document.getElementById('loadingOverlay');
                    if (loadingOverlay) loadingOverlay.style.display = 'none';
                    payButton.disabled = false;
                    payButton.textContent = '결제하기';

                    // 백엔드에서 받아온 에러 메시지를 화면에 그대로 띄워줍니다! (예: "자동주문 방지 글자가 틀렸습니다.")
                    alert(error.message);

                    // 캡차가 틀렸다는 내용이면, 유저가 바로 다시 칠 수 있게 이미지를 새로고침해 줍니다!
                    if (error.message.includes('캡차') || error.message.includes('자동주문') || error.message.includes('틀렸습니다')) {
                        document.getElementById('captchaInput').value = ''; // 입력창 비워주기
                        loadCaptcha(); // 이미지 새로 갱신
                    }
                });
        });
    }

    // ==========================================
    // [새로 추가된 구역] 창 닫기 감지 및 좀비 좌석 해제
    // ==========================================

    // 1. 사용자가 탭을 닫거나, 뒤로가기를 누르거나, 새로고침을 할 때 발동
    const exitEvents = ['visibilitychange', 'pagehide', 'unload'];

    exitEvents.forEach(eventType => {
        window.addEventListener(eventType, function(event) {

            // visibilitychange 이벤트일 때는 화면이 완전히 '숨김(hidden)' 상태일 때만 작동
            if (eventType === 'visibilitychange' && document.visibilityState !== 'hidden') {
                return;
            }

            // 결제 버튼이나 이전 단계 버튼을 누른 게 아니라면 (도망친 거라면!)
            if (!preserveSeat) {
                // 🌟 핵심: 3개의 이벤트가 동시에 터져서 서버에 3번 요청가는 걸 막기 위해 스위치를 바로 꺼버립니다.
                preserveSeat = true;

                // 브라우저가 죽는 순간에도 서버로 "이 자리 풀어줘!" 유언을 남김 (keepalive)
                sendReleaseRequest();
            }
        });
    });

    // ==========================================
    // 🌟 2. 브라우저 기본 '뒤로가기(←)' 버튼 완벽 방어 (History 낚아채기)
    // ==========================================

    // 결제창에 들어오자마자, 브라우저 뒤로가기 기록에 가짜 페이지를 하나 쓱 끼워 넣습니다.
    history.pushState(null, null, location.href);

    // 유저가 브라우저의 실제 뒤로가기(←) 버튼을 누르면, 이전 페이지로 가는 대신 이 이벤트가 발동합니다!
    window.addEventListener('popstate', function(event) {

        // 우리가 만든 '이전단계' 버튼과 100% 똑같이 0.5초의 시간을 벌어줍니다.
        sendReleaseRequest().finally(() => {
            setTimeout(() => {
                preserveSeat = true; // 스위치 켜기
                history.back();      // 0.5초 뒤에 '진짜' 이전 페이지로 보내주기
            }, 500);
        });

    });
});

// 유저가 창을 닫거나 새로고침하려고 할 때 브라우저 자체 경고창 띄우기
window.addEventListener('beforeunload', function (event) {
    if (!preserveSeat) {
        // 현대 브라우저에서는 보안상 커스텀 문자열은 무시되고, 브라우저 표준 경고창이 뜹니다.
        event.preventDefault();
        event.returnValue = '';
    }
});