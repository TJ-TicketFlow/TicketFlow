document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById('searchInput');
    const suggestionList = document.getElementById('suggestionList');

    searchInput.addEventListener('input', async (e) => {
        const query = e.target.value;
        if (query.length < 1) {
            suggestionList.style.display = 'none';
            return;
        }

        try {
            const response = await fetch(`/concert/suggest?q=${encodeURIComponent(query)}`);
            const data = await response.json();

            if (data.suggestions && data.suggestions.length > 0) {
                suggestionList.innerHTML = data.suggestions
                    .map(item => `<li>${item}</li>`).join('');
                suggestionList.style.display = 'block';
            } else {
                suggestionList.style.display = 'none';
            }
        } catch (error) { console.error("자동완성 에러:", error); }
    });

    suggestionList.addEventListener('click', (e) => {
        if (e.target.tagName === 'LI') {
            searchInput.value = e.target.innerText;
            suggestionList.style.display = 'none';
            searchInput.form.submit();
        }
    });

    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !suggestionList.contains(e.target)) {
            suggestionList.style.display = 'none';
        }
    });
});