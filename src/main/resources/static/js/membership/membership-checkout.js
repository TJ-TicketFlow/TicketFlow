function goToLemonSqueezy(userEmail, variantId) {
    if (!userEmail) {
        alert("이메일 정보를 확인할 수 없습니다.");
        return;
    }

    const requestData = {
        email: userEmail,
        variantId: variantId
    };

    fetch('/api/create-checkout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestData)
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(err => { throw new Error(err.error || '결제창 생성 실패'); });
            }
            return response.json();
        })
        .then(data => {
            if (data.url) {
                const iframeTarget = document.getElementById('iframe-target');
                const lsOverlay = document.getElementById('ls-overlay');

                if (iframeTarget && lsOverlay) {
                    iframeTarget.innerHTML = `<iframe class="ls-iframe" src="${data.url}" allow="payment"></iframe>`;
                    lsOverlay.classList.add('open');
                    document.body.style.overflow = 'hidden';
                } else {
                    console.error("오류: 화면에서 'iframe-target' 또는 'ls-overlay' 태그를 찾을 수 없습니다.");
                    window.location.href = data.url;
                }
            } else {
                alert('결제 URL을 받아오지 못했습니다.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert("결제 요청 실패: " + error.message);
        });
}

function closeModal() {
    document.getElementById('ls-overlay').classList.remove('open');
    document.getElementById('iframe-target').innerHTML = '';
    document.body.style.overflow = '';
}