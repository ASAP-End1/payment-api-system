


async function makeApiRequest(endpointKey, options = {}) {
    const {
        body = null,
        params = {},
        pathParams = {},
        returnHeaders = false
    } = options;

    try {

        const config = await getConfig();
        const endpointContract = config.api.endpoints[endpointKey];

        if (!endpointContract) {
            throw new Error(`엔드포인트 '${endpointKey}'를 설정에서 찾을 수 없습니다`);
        }


        const method = options.method || endpointContract.method || 'GET';


        const url = await buildApiUrl(endpointKey, pathParams);


        updateEndpointDisplay(method, url);


        if (body) {
            updateRequestDisplay(body);
        }


        showLoading();


        const fetchOptions = {
            method,
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        };


        const token = typeof getToken === 'function' ? getToken() : null;
        if (token) {
            fetchOptions.headers['Authorization'] = `Bearer ${token}`;
        }

        if (body && method !== 'GET') {
            fetchOptions.body = JSON.stringify(body);
        }

        const response = await fetch(url, fetchOptions);


        const authHeader = response.headers.get('Authorization');
        if (authHeader && authHeader.startsWith('Bearer ')) {
            const newToken = authHeader.substring(7);
            if (typeof saveToken === 'function') {
                saveToken(newToken);
                console.log('Access Token 자동 갱신됨');
            }
        }


        if (response.status === 401) {
            if (typeof removeToken === 'function') removeToken();
            window.location.href = '/pages/login';
            return;
        }
        const text = await response.text();
        const data = text ? JSON.parse(text) : {
            error: '응답 본문 없음',
            status: response.status
        };


        if (!response.ok) {
            displayError(data);

            const errorMessage = data.message || data.error || `HTTP ${response.status}: ${response.statusText}`;
            throw new Error(errorMessage);
        }

        displaySuccess(data);


        if (returnHeaders) {
            return {
                data,
                headers: Object.fromEntries(response.headers.entries())
            };
        }

        if (data && data.data !== undefined) {
            const unwrapped = data.data;


            if (unwrapped && typeof unwrapped === 'object' && !Array.isArray(unwrapped)) {
                if (unwrapped.success === undefined && data.success !== undefined) {
                    unwrapped.success = data.success;
                }
            }

            return unwrapped;
        }
        return data;
    } catch (error) {
        displayError({
            error: error.message,
            stack: error.stack
        });
        throw error;
    }
}


function updateEndpointDisplay(method, url) {
    const endpointBadge = document.getElementById('current-endpoint');
    if (endpointBadge) {
        endpointBadge.innerHTML = `
            <strong>${method}</strong> ${url}
        `;
    }
}


function updateRequestDisplay(body) {
    const requestTextarea = document.getElementById('request-body');
    if (requestTextarea) {
        requestTextarea.value = JSON.stringify(body, null, 2);
    }
}


function showLoading() {
    const responseBox = document.getElementById('response-output');
    if (responseBox) {
        responseBox.className = 'response-box';
        responseBox.textContent = '⏳ 로딩 중...';
    }
}


function displaySuccess(data) {
    const responseBox = document.getElementById('response-output');
    if (responseBox) {
        responseBox.className = 'response-box response-success';
        responseBox.textContent = JSON.stringify(data, null, 2);
    }
}


function displayError(error) {
    const responseBox = document.getElementById('response-output');
    if (responseBox) {
        responseBox.className = 'response-box response-error';
        responseBox.textContent = JSON.stringify(error, null, 2);
    }
}


async function sendCustomRequest() {
    const endpointInput = document.getElementById('custom-endpoint');
    const methodSelect = document.getElementById('request-method');
    const bodyTextarea = document.getElementById('request-body');

    if (!endpointInput || !bodyTextarea) {
        console.error('Required elements not found');
        return;
    }

    const url = endpointInput.value.trim();
    const method = methodSelect ? methodSelect.value : 'POST';
    let body = null;

    try {
        const bodyText = bodyTextarea.value.trim();
        if (bodyText && method !== 'GET') {
            body = JSON.parse(bodyText);
        }
    } catch (e) {
        displayError({ error: 'Invalid JSON in request body', details: e.message });
        return;
    }

    try {
        updateEndpointDisplay(method, url);
        showLoading();

        const fetchOptions = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };

        if (body && method !== 'GET') {
            fetchOptions.body = JSON.stringify(body);
        }

        const response = await fetch(url, fetchOptions);
        const data = await response.json();

        if (response.ok) {
            displaySuccess(data);
        } else {
            displayError(data);
        }
    } catch (error) {
        displayError({
            error: error.message,
            stack: error.stack
        });
    }
}


function clearResponse() {
    const responseBox = document.getElementById('response-output');
    if (responseBox) {
        responseBox.className = 'response-box';
        responseBox.textContent = '';
    }
}


function formatCurrency(amount, currency = 'KRW') {
    return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: currency
    }).format(amount);
}


function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `alert alert-${type}`;
    notification.textContent = message;
    notification.style.position = 'fixed';
    notification.style.top = '100px';
    notification.style.right = '20px';
    notification.style.zIndex = '9999';
    notification.style.minWidth = '300px';

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 3000);
}
