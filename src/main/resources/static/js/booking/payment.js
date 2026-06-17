document.addEventListener("DOMContentLoaded", function() {

    // ==========================================
    // 💡 1. 기초 설정 (HTML 상자 가져오기)
    // ==========================================

    // 외부 JS 파일이면 타임리프 문법이 안 먹힐 수 있습니다.
    // HTML에 미리 숨겨두거나, 일단 테스트용으로 false로 지정합니다.
    const isMember = false; // 타임리프 연동 시 HTML 내부 스크립트로 이동 권장

    // ⭐️ 핵심: Number()로 바꾸지 말고, 'HTML 상자(Element)' 자체를 가져옵니다!
    const couponContainer = document.getElementById('couponContainer');
    const deliveryRadios = document.querySelectorAll('input[name="deliveryType"]');

    const deliveryFeeDisplay = document.getElementById('deliveryFeeDisplay');
    const totalPriceDisplay = document.getElementById('totalPriceDisplay');
    const finalPriceDisplay = document.getElementById('finalPriceDisplay');
    const membershipDiscountDisplay = document.getElementById('membershipDiscount');
    const couponDiscountDisplay = document.getElementById('couponDiscount');
    const totalDiscountDisplay = document.getElementById('totalDiscount');

    // 계산을 위해 원본 티켓값과 수수료는 상자 안에서 '숫자'만 꺼내옵니다.
    const ticketPriceElement = document.getElementById('TicketPrice');
    const feeElement = document.getElementById('fee');

    // 값이 없을 경우를 대비한 안전한 숫자 변환
    const TicketPrice = ticketPriceElement ? Number(ticketPriceElement.textContent.replace(/,/g,"")) : 20000;
    const fee = feeElement ? Number(feeElement.textContent.replace(/,/g,"")) : 2000;


    // ==========================================
    // 💡 2. 쿠폰 목록 가져오기
    // ==========================================
    function loadCoupons() {
        if (!couponContainer) return; // 상자가 없으면 에러 방지

        fetch('/booking/checkcoupons')
            .then(response => response.json())
            .then(coupons => {
                couponContainer.innerHTML = ''; // "불러오는 중..." 지우기

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

                // 새 라디오 버튼에 이벤트 달아주기
                const couponRadios = document.querySelectorAll('input[name="couponRate"]');
                couponRadios.forEach(radio => {
                    radio.addEventListener('change', calculateFinalPrice);
                });

                // 그리기 완료 후 계산기 한 번 실행
                calculateFinalPrice();
            })
            .catch(error => {
                console.error("쿠폰을 불러오는데 실패했습니다.", error);
                couponContainer.innerHTML = '<span style="color:red; font-size:13px;">쿠폰을 불러올 수 없습니다.</span>';
            });
    }


    // ==========================================
    // 💡 3. 결제 가격 계산기
    // ==========================================
    function calculateFinalPrice() {
        // ① 배송비 확인
        const checkedDelivery = document.querySelector('input[name="deliveryType"]:checked');
        let deliveryFee = 0;
        if (checkedDelivery && checkedDelivery.value === 'POST') {
            deliveryFee = 10000;
        }

        // ② 멤버십 할인 계산
        let membershipDiscountAmt = 0;
        if (isMember) {
            membershipDiscountAmt = TicketPrice * 0.03; // 3% 할인
        }

        // ③ 쿠폰 할인 계산
        let couponDiscountAmt = 0;
        const checkedCoupon = document.querySelector('input[name="couponRate"]:checked');
        if (checkedCoupon) {
            const discountRate = Number(checkedCoupon.value);
            couponDiscountAmt = TicketPrice * (discountRate / 100);
        }

        // ④ 최종 계산
        const newTotalPrice = TicketPrice + deliveryFee + fee;
        const totalDiscountAmt = couponDiscountAmt + membershipDiscountAmt;
        const newFinalPrice = newTotalPrice - totalDiscountAmt;

        // ⑤ 화면 업데이트 (쉼표 찍기)
        if (deliveryFeeDisplay) deliveryFeeDisplay.textContent = deliveryFee.toLocaleString();
        if (membershipDiscountDisplay) membershipDiscountDisplay.textContent = membershipDiscountAmt.toLocaleString();
        if (couponDiscountDisplay) couponDiscountDisplay.textContent = couponDiscountAmt.toLocaleString();
        if (totalDiscountDisplay) totalDiscountDisplay.textContent = totalDiscountAmt.toLocaleString();
        if (totalPriceDisplay) totalPriceDisplay.textContent = newTotalPrice.toLocaleString();
        if (finalPriceDisplay) finalPriceDisplay.textContent = newFinalPrice.toLocaleString();
    }

    // ==========================================
    // 💡 4. 감시자 설정 및 최초 실행
    // ==========================================
    deliveryRadios.forEach(radio => {
        radio.addEventListener('change', calculateFinalPrice);
    });

    loadCoupons(); // 쿠폰부터 먼저 불러옵니다.


    // ==========================================
    // 💡 5. 결제하기 버튼 클릭
    // ==========================================
    const payButton = document.getElementById('createPayment');

    if (payButton) {
        payButton.addEventListener('click', function() {
            // 정보 가져오기
            const buyerName = document.querySelector('input[name="buyerName"]').value;
            const buyerEmail = document.querySelector('input[name="buyerEmail"]').value;
            const buyerPhone = document.querySelector('input[name="buyerPhone"]').value; // 💡 누락되었던 폰번호 추가

            const receiverName = document.querySelector('input[name="receiverName"]').value;
            const receiverPhone = document.querySelector('input[name="receiverPhone"]').value;

            // 💡 <div>와 <span> 태그에서 텍스트로 읽어오도록 수정
            const payName = document.getElementById('payNameDisplay').textContent;
            const payAmount = Number(finalPriceDisplay.textContent.replace(/,/g, ""));

            const checkedCoupon = document.querySelector('input[name="couponRate"]:checked');
            const usedCouponId = (checkedCoupon && checkedCoupon.value !== "0") ? checkedCoupon.getAttribute('data-id') : null;

            // 유효성 검사
            if(buyerPhone === "" || receiverName === "") {
                alert("필수 입력 정보를 모두 채워주세요!");
                return;
            }

            // 토큰 가져오기 (오류 방지를 위한 안전장치 추가)
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
                userCouponId: usedCouponId
            };

            const headers = {
                'Content-Type': 'application/json'
            };
            if (csrfHeader && csrfToken) {
                headers[csrfHeader] = csrfToken;
            }

            fetch('/booking/create', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(requestData)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('서버 통신 실패 (상태 코드: ' + response.status + ')');
                    }
                    return response.text();
                })
                .then(checkoutUrl => {
                    window.location.href = checkoutUrl;
                })
                .catch(error => {
                    console.error('❌ 결제 준비 중 오류 발생:', error);
                    alert('결제창을 불러오는 데 실패했습니다.');
                });
        });
    }
});