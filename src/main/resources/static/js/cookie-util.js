


function setCookie(name, value, days = 1) {
    const expires = new Date();
    expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);



    document.cookie = `${name}=${value}; expires=${expires.toUTCString()}; path=/; SameSite=Strict`;
}


function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);

    if (parts.length === 2) {
        return parts.pop().split(';').shift();
    }

    return null;
}


function deleteCookie(name) {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
}


function saveToken(token) {
    setCookie('jwt_token', token, 1);
}


function getToken() {
    return getCookie('jwt_token');
}


function removeToken() {
    deleteCookie('jwt_token');
}


function decodeJWT(token) {
    try {
        const parts = token.split('.');
        if (parts.length !== 3) {
            return null;
        }


        const payload = parts[1];


        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));

        return JSON.parse(decoded);
    } catch (error) {
        console.error('JWT 디코딩 실패:', error);
        return null;
    }
}


function getEmailFromToken() {
    const token = getToken();
    if (!token) return null;

    const payload = decodeJWT(token);

    return payload ? payload.sub : null;
}
