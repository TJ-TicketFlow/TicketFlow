// ==========================================
// 💡 1. 카카오 우편번호 검색 팝업 기능 (바깥에 배치 완료!)
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
// 💡 2. 화면이 다 켜진 후 실행될 구역
// ==========================================
document.addEventListener("DOMContentLoaded", function() {

    // [타이머 설정]
    let timeout = 30;
    let timeLeft = timeout * 60;
    const timerDisplay = document.getElementById('countdownTimer');

    // 1초마다 똑딱거리는 초시계
    const timerInterval = setInterval(function() {
        const minutes = Math.floor(timeLeft / 60);
        const seconds = timeLeft % 60;

        const minutesString = String(minutes).padStart(2, '0');
        const secondsString = String(seconds).padStart(2, '0');

        if (timerDisplay) {
            timerDisplay.textContent = minutesString + ":" + secondsString;
        }

        if (timeLeft <= 0) {
            clearInterval(timerInterval);
            alert("결제 대기 시간("+ timeout + "분)이 초과되었습니다. 메인 화면으로 돌아갑니다.");
            window.location.href = '/';
        }
        timeLeft--;
    }, 1000);

    // [기초 HTML 상자들 가져오기]
    const isMember = false;
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

    const TicketPrice = ticketPriceElement ? Number(ticketPriceElement.textContent.replace(/,/g,"")) : 20000;
    const fee = feeElement ? Number(feeElement.textContent.replace(/,/g,"")) : 2000;


    // ==========================================
    // 💡 기능 함수들 (쿠폰 불러오기, 계산기, 숨김마법사)
    // ==========================================

    // 쿠폰 목록 가져오기
    function loadCoupons() {
        if (!couponContainer) return;

        fetch('/booking/checkcoupons')
            .then(response => response.json())
            .then(coupons => {
                couponContainer.innerHTML = '';

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

                const couponRadios = document.querySelectorAll('input[name="couponRate"]');
                couponRadios.forEach(radio => {
                    radio.addEventListener('change', calculateFinalPrice);
                });

                calculateFinalPrice();
            })
            .catch(error => {
                console.error("쿠폰을 불러오는데 실패했습니다.", error);
                couponContainer.innerHTML = '<span style="color:red; font-size:13px;">쿠폰을 불러올 수 없습니다.</span>';
            });
    }

    // 결제 가격 계산기
    function calculateFinalPrice() {
        const checkedDelivery = document.querySelector('input[name="deliveryType"]:checked');
        let deliveryFee = 0;
        if (checkedDelivery && checkedDelivery.value === 'POST') {
            deliveryFee = 10000;
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
    // 💡 감시자(Event Listener) 설정 및 최초 실행
    // ==========================================
    deliveryRadios.forEach(radio => {
        radio.addEventListener('change', calculateFinalPrice);
        radio.addEventListener('change', toggleDeliveryInfo);
    });

    // 최초 1회 실행하여 초기 화면 세팅
    toggleDeliveryInfo();
    loadCoupons();


    // ==========================================
    // 💡 결제하기 버튼 클릭 이벤트
    // ==========================================
    const payButton = document.getElementById('createPayment');

    if (payButton) {
        payButton.addEventListener('click', function() {
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

            const checkedDelivery = document.querySelector('input[name="deliveryType"]:checked');
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
                reservationKey: 1,
                payName: payName,
                payAmount: payAmount,
                buyerName: buyerName,
                buyerEmail: buyerEmail,
                payDelName: receiverName,
                payDelCall: receiverPhone,
                userCouponId: usedCouponId,
                payDelPostcode: zipCode,
                payDelAddr: addressBase + " " + addressDetail
            };

            const headers = { 'Content-Type': 'application/json' };
            if (csrfHeader && csrfToken) { headers[csrfHeader] = csrfToken; }

            // 서버 전송
            fetch('/booking/create', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(requestData)
            })
                .then(response => {
                    if (!response.ok) { throw new Error('서버 통신 실패'); }
                    return response.text();
                })
                .then(checkoutUrl => {
                    window.location.href = checkoutUrl;
                })
                .catch(error => {
                    console.error('❌ 결제 준비 중 오류 발생:', error);
                    if (loadingOverlay) loadingOverlay.style.display = 'none';
                    payButton.disabled = false;
                    payButton.textContent = '결제하기';
                    alert('결제창을 불러오는 데 실패했습니다.');
                });
        });
    }
});