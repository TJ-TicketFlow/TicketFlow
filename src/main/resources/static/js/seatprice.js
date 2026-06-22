// src/main/resources/static/js/seat-price.js 중 일부

// src/main/resources/static/js/seat-price.js

let concertPriceMap = {};
let isSinglePrice = false;
let defaultSinglePrice = 0;

function initPriceMap(priceInfoStr) {
    concertPriceMap = {};
    isSinglePrice = false;
    defaultSinglePrice = 0;

    if (!priceInfoStr) {
        console.warn("⚠️ 백엔드에서 전달된 가격 정보 문자열이 비어있습니다!");
        return;
    }

    console.log("▶ 가격 계산 모듈이 처리할 데이터:", priceInfoStr);

    // 쉼표(,)를 기준으로 등급이 나뉘어 있는지 확인
    // 단, 가격 자체에 포함된 쉼표(88,000)와 구분용 쉼표를 구별하기 위해 공백을 포함하거나 단어를 체크합니다.
    // 가장 안전한 방법은 원(원)이나 석(석) 뒤의 쉼표를 기준으로 쪼개는 것입니다.
    const parts = priceInfoStr.split(/석\s*|원\s*,\s*/);

    // 만약 잘 안 쪼개졌다면 일반 쉼표로 분리 시도하되, 숫자 뒤의 쉼표인지 확인
    let items = priceInfoStr.split(/,(?=\s*[^0-9]*[A-Za-z가-힣])/);
    if (items.length === 1 && priceInfoStr.includes(',')) {
        // "등급 158,000, 등급 88,000" 형태 대응
        items = priceInfoStr.split(/,(?=\s*[A-Za-z가-힣]+)/);
    }

    if (items.length === 1) {
        // 1. 단일가 처리
        const numericStr = priceInfoStr.replace(/[^0-9]/g, '');
        if (numericStr) {
            isSinglePrice = true;
            defaultSinglePrice = parseInt(numericStr, 10);
            console.log(`가격 계산 모듈: [단일가] 전 좌석 ${defaultSinglePrice}원 적용`);
            return;
        }
    } else {
        // 2. 차등가 처리 ("VIP패키지석 158,000원", "GA스탠딩석 88,000원")
        items.forEach(item => {
            if (!item.trim()) return;

            // 문자열에서 오직 숫자만 싹 긁어모으기 (쉼표 제거 포함)
            const numericStr = item.replace(/[^0-9]/g, '');
            const price = parseInt(numericStr, 10);

            // 숫자, 쉼표, '원', 공백을 제외한 순수 글자(등급명) 추출
            const textOnly = item.replace(/[0-9,원\s]/g, '').toUpperCase();

            if (textOnly && !isNaN(price)) {
                if (textOnly.includes("VIP")) {
                    concertPriceMap["VIP"] = price;
                } else if (textOnly.includes("GA") || textOnly.includes("스탠딩") || textOnly.includes("일반")) {
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
 * 2. 현재 선택된 좌석들의 총 금액 계산 및 출력
 */
function calculateAndDisplayTotalPrice(selectedElements) {
    const priceDisplayEl = document.getElementById("total-price-display");
    if (!priceDisplayEl) return;

    let totalPrice = 0;

    selectedElements.forEach(seatEl => {
        // 단일가 플래그가 켜져 있으면 좌석 등급 무관하게 기본가 적용
        if (isSinglePrice) {
            totalPrice += defaultSinglePrice;
        }
        // 등급별 차등가 적용인 경우
        else {
            const seatClass = seatEl.dataset.seatClass ? seatEl.dataset.seatClass.toUpperCase() : "";
            if (concertPriceMap[seatClass]) {
                totalPrice += concertPriceMap[seatClass];
            } else {
                console.warn(`⚠️ 현재 클릭한 좌석 등급(${seatClass})에 해당하는 가격이 가격표에 없습니다.`);
            }
        }
    });

    // 화면에 최종 금액 갱신
    priceDisplayEl.innerText = totalPrice.toLocaleString() + "원";
}