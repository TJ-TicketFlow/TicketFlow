// ===================================================
// src/main/resources/static/js/seat-price.js
// ===================================================

let concertPriceMap = {};
let isSinglePrice = false;
let defaultSinglePrice = 0;

/**
 * 1. 백엔드 가격 정보 문자열 파싱 및 오염 방어
 */
function initPriceMap(priceInfoStr) {
    concertPriceMap = {};
    isSinglePrice = false;
    defaultSinglePrice = 0;

    if (!priceInfoStr) {
        console.warn("⚠️ 백엔드에서 전달된 가격 정보 문자열이 비어있습니다!");
        return;
    }

    console.log("▶ 가격 계산 모듈이 처리할 데이터:", priceInfoStr);

    let items = priceInfoStr.split(/,(?=\s*[^0-9]*[A-Za-z가-힣])/);
    if (items.length === 1 && priceInfoStr.includes(',')) {
        items = priceInfoStr.split(/,(?=\s*[A-Za-z가-힣]+)/);
    }

    if (items.length === 1) {
        // [단일가 처리]
        const numericStr = priceInfoStr.replace(/[^0-9]/g, '');
        if (numericStr) {
            let price = parseInt(numericStr, 10);

            // 🚨 백엔드 만원 단위 오염 방어 (120원 등 방지)
            if (price > 0 && price < 1000) {
                price = price * 1000;
            }

            isSinglePrice = true;
            defaultSinglePrice = price;
            console.log(`가격 계산 모듈: [단일가] 전 좌석 ${defaultSinglePrice}원 적용`);
            return;
        }
    } else {
        // [차등가 처리]
        items.forEach(item => {
            if (!item.trim()) return;

            const numericStr = item.replace(/[^0-9]/g, '');
            let price = parseInt(numericStr, 10);

            // 🚨 백엔드 만원 단위 오염 방어
            if (!isNaN(price) && price > 0 && price < 1000) {
                price = price * 1000;
            }

            const textOnly = item.replace(/[0-9,원\s]/g, '').toUpperCase();

            if (textOnly && !isNaN(price)) {
                if (textOnly.includes("VIP")) {
                    concertPriceMap["VIP"] = price;
                } else if (textOnly.includes("GA") || textOnly.includes("스탠딩") || textOnly.includes("일반") || textOnly.includes("GENERAL")) {
                    concertPriceMap["GENERAL"] = price;
                } else {
                    concertPriceMap[textOnly] = price;
                }
            }
        });

        console.log("가격 계산 모듈: [차등가 파싱 성공] 등급별 가격표:", concertPriceMap);
    }
}

/**
 * 2. [순수 계산 함수] 현재 선택된 좌석들의 총 금액만 계산하여 return
 * (UI 조작 없이 오직 숫자 연산만 수행하여 독립성 확보)
 */
function calculateSelectedSeatsPrice(selectedElements) {
    let totalPrice = 0;

    if (!selectedElements || selectedElements.length === 0) {
        return totalPrice;
    }

    selectedElements.forEach(seatEl => {
        // 만약 지정석 배치도에서 엘리먼트에 직접 숫자가 박혀있다면 최우선 적용
        if (seatEl.dataset.price) {
            totalPrice += parseInt(seatEl.dataset.price, 10) || 0;
            return;
        }

        // 단일가 플래그가 켜져 있으면 기본가 적용
        if (isSinglePrice) {
            totalPrice += defaultSinglePrice;
        }
        // 등급별 차등가 연산
        else {
            const seatClass = seatEl.dataset.seatClass ? seatEl.dataset.seatClass.toUpperCase() : "";
            if (concertPriceMap[seatClass]) {
                totalPrice += concertPriceMap[seatClass];
            } else {
                // 방어 코드: 키가 완전히 일치하지 않을 때를 위한 유연한 매칭
                let matched = false;
                Object.keys(concertPriceMap).forEach(key => {
                    if (key.includes(seatClass) || seatClass.includes(key)) {
                        totalPrice += concertPriceMap[key];
                        matched = true;
                    }
                });

                if (!matched) {
                    console.warn(`⚠️ 현재 클릭한 좌석 등급(${seatClass})에 해당하는 가격을 찾지 못했습니다.`);
                }
            }
        }
    });

    return totalPrice;
}

/**
 * 3. [UI 출력 함수] 연산된 금액을 가져와 화면 레이아웃에 맞춰 갱신 및 return
 */
function calculateAndDisplayTotalPrice(selectedElements) {
    // 💡 분리된 순수 계산 함수를 호출하여 가격을 가져옵니다.
    const totalPrice = calculateSelectedSeatsPrice(selectedElements);

    // 유연한 엘리먼트 감지 (화면 구조 변화 대응)
    const totalDisplayEl =
        document.getElementById("total-price-display") ||
        document.getElementById("standing-total-price") ||
        document.querySelector(".right-sidebar span[style*='color']") ||
        document.querySelector(".right-sidebar div") ||
        document.evaluate("//span[contains(text(),'결제 예정 금액')]", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue?.parentElement;

    if (totalDisplayEl) {
        const priceTarget = totalDisplayEl.querySelector("strong") || totalDisplayEl.querySelector("span") || totalDisplayEl;

        if (priceTarget.innerHTML && priceTarget.innerHTML.includes("결제 예정 금액")) {
            priceTarget.innerHTML = `결제 예정 금액 <strong style="color: #3b82f6; font-size: 20px;">${totalPrice.toLocaleString()}원</strong>`;
        } else {
            priceTarget.innerText = totalPrice.toLocaleString() + "원";
        }
    }

    // 외부 전송부(submitBooking 등)에서 연동해 쓸 수 있도록 계산된 금액을 최종 리턴  
    return totalPrice;
}