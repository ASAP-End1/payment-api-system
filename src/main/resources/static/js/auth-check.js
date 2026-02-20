


function checkAuthentication() {

    const token = typeof getToken === 'function' ? getToken() : null;
    const currentPath = window.location.pathname;


    if (currentPath === '/pages/login' || currentPath === '/pages/register') {
        return;
    }


    if (!token) {
        window.location.href = '/pages/login';
        return;
    }


    displayUserInfo();
}


function displayUserInfo() {

    const email = typeof getEmailFromToken === 'function' ? getEmailFromToken() : null;
    const navbarActions = document.querySelector('.navbar-actions');

    if (email && navbarActions) {

        const userInfo = document.createElement('div');
        userInfo.style.cssText = 'display: flex; align-items: center; gap: 1rem; margin-right: 1rem;';
        userInfo.innerHTML = `
            <span style="color: var(--text-secondary); font-size: 0.9rem;">
                👤 ${email}
            </span>
            <button onclick="handleLogout()" class="btn btn-outline" style="padding: 0.4rem 1rem; font-size: 0.875rem;">
                로그아웃
            </button>
        `;


        navbarActions.insertBefore(userInfo, navbarActions.firstChild);
    }
}


async function handleLogout() {
    try {
        const token = getToken();


        const logoutUrl = await buildApiUrl('logout');

        const response = await fetch(logoutUrl, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            console.log('로그아웃 성공:', data);


            validateApiResponse('logout', data);
        }
    } catch (error) {
        console.error('로그아웃 실패:', error);
    } finally {

        removeToken();

        window.location.href = '/pages/login';
    }
}


document.addEventListener('DOMContentLoaded', function() {
    checkAuthentication();
});
