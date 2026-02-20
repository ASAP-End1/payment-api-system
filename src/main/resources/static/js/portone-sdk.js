


async function initPortOne() {
    const config = await getConfig();


    if (typeof PortOne === 'undefined') {
        console.error('PortOne SDK가 로드되지 않았습니다. HTML에 스크립트를 포함하세요.');
        throw new Error('PortOne SDK not loaded');
    }

    return config.portone;
}


async function openPortOnePayment(paymentData) {
    try {
        const portoneConfig = await initPortOne();


        const channelKeys = portoneConfig.channelKeys || {};
        const channelKey = channelKeys['kg-inicis'];

        if (!channelKey) {
            throw new Error('kg-inicis 채널키가 설정되지 않았습니다.');
        }


        console.log('1단계: 서버에 결제 시작 요청...');
        const createPaymentResult = await makeApiRequest('create-payment', {
            body: {
                orderNumber: paymentData.orderNumber,
                totalAmount: paymentData.totalAmount
            }
        });


        if (!validateApiResponse('create-payment', createPaymentResult)) {
            throw new Error('결제 시작 요청 응답 형식이 올바르지 않습니다.');
        }

        if (!createPaymentResult.success) {
            throw new Error('결제 시작 요청 실패');
        }


        const serverPaymentId = createPaymentResult.paymentId;
        console.log('서버에서 생성한 결제 ID:', serverPaymentId);


        console.log('2단계: PortOne 결제창 열기...');
        const paymentRequest = {
            storeId: portoneConfig.storeId,
            channelKey: channelKey,
            paymentId: serverPaymentId,
            orderName: paymentData.orderName || '상품 주문',
            totalAmount: paymentData.totalAmount,
            currency: paymentData.currency || 'KRW',
            payMethod: paymentData.payMethod || 'CARD',
            customer: paymentData.customer || {
                customerId: 'customer_001',
                fullName: '홍길동',
                phoneNumber: '01012345678',
                email: 'customer@example.com'
            },
            redirectUrl: window.location.href,
            noticeUrls: paymentData.noticeUrls || []
        };

        console.log('PortOne 결제창 열기:', paymentRequest);


        updateRequestDisplay(paymentRequest);
        updateEndpointDisplay('SDK', 'PortOne.requestPayment()');


        const response = await PortOne.requestPayment(paymentRequest);

        console.log('결제 응답:', response);


        if (response.code != null) {

            displayError({
                code: response.code,
                message: response.message
            });
            throw new Error(response.message);
        } else {

            displaySuccess({
                paymentId: response.paymentId,
                txId: response.txId,
                message: '결제창 완료. 서버에서 검증하세요.'
            });
            return response;
        }
    } catch (error) {
        console.error('결제 오류:', error);
        displayError({
            error: error.message,
            stack: error.stack
        });
        throw error;
    }
}


async function openPortOnePaymentWithPoints(paymentData) {
    try {
        const portoneConfig = await initPortOne();


        const channelKeys = portoneConfig.channelKeys || {};
        const channelKey = channelKeys['kg-inicis'];

        if (!channelKey) {
            throw new Error('kg-inicis 채널키가 설정되지 않았습니다.');
        }


        const pointsToUse = paymentData.pointsToUse || 0;
        if (pointsToUse < 0) {
            throw new Error('포인트는 0 이상이어야 합니다.');
        }


        console.log('1단계: 서버에 결제 시작 요청 (포인트 포함)...');
        const createPaymentResult = await makeApiRequest('create-payment', {
            body: {
                orderNumber: paymentData.orderNumber,
                totalAmount: paymentData.totalAmount,
                pointsToUse: pointsToUse
            }
        });


        if (!validateApiResponse('create-payment', createPaymentResult)) {
            throw new Error('결제 시작 요청 응답 형식이 올바르지 않습니다.');
        }

        if (!createPaymentResult.success) {
            throw new Error('결제 시작 요청 실패');
        }


        const serverPaymentId = createPaymentResult.paymentId;
        console.log('서버에서 생성한 결제 ID:', serverPaymentId);


        const finalAmount = Math.max(0, paymentData.totalAmount - pointsToUse);
        console.log(`포인트 차감: ${paymentData.totalAmount}원 - ${pointsToUse}P = ${finalAmount}원`);


        console.log('2단계: PortOne 결제창 열기...');
        const paymentRequest = {
            storeId: portoneConfig.storeId,
            channelKey: channelKey,
            paymentId: serverPaymentId,
            orderName: paymentData.orderName || '상품 주문',
            totalAmount: finalAmount,
            currency: paymentData.currency || 'KRW',
            payMethod: paymentData.payMethod || 'CARD',
            customer: paymentData.customer || {
                customerId: 'customer_001',
                fullName: '홍길동',
                phoneNumber: '01012345678',
                email: 'customer@example.com'
            },
            redirectUrl: window.location.href,
            noticeUrls: paymentData.noticeUrls || []
        };

        console.log('PortOne 결제창 열기 (포인트 차감 후):', paymentRequest);


        updateRequestDisplay(paymentRequest);
        updateEndpointDisplay('SDK', 'PortOne.requestPayment() [Points]');


        const response = await PortOne.requestPayment(paymentRequest);

        console.log('결제 응답:', response);


        if (response.code != null) {

            displayError({
                code: response.code,
                message: response.message
            });
            throw new Error(response.message);
        } else {

            displaySuccess({
                paymentId: response.paymentId,
                txId: response.txId,
                pointsUsed: pointsToUse,
                message: '결제창 완료 (포인트 차감). 서버에서 검증하세요.'
            });
            return response;
        }
    } catch (error) {
        console.error('결제 오류 (포인트):', error);
        displayError({
            error: error.message,
            stack: error.stack
        });
        throw error;
    }
}


async function issuePortOneBillingKey(billingKeyData) {
    try {
        const portoneConfig = await initPortOne();

        // 정기결제: 토스 채널 키 사용 (고정)
        const channelKeys = portoneConfig.channelKeys || {};
        const channelKey = channelKeys.toss;

        if (!channelKey) {
            throw new Error('toss 채널키가 설정되지 않았습니다.');
        }

        const billingKeyRequest = {
            storeId: portoneConfig.storeId,
            channelKey: channelKey,
            billingKeyMethod: billingKeyData.billingKeyMethod || 'CARD',
            method: {
                card: {
                    credential: {}
                }
            },
            issueId: billingKeyData.issueId || `billing_${Date.now()}`,
            issueName: billingKeyData.issueName || '정기결제 등록',
            customer: billingKeyData.customer || {
                customerId: 'customer_001',
                fullName: '홍길동',
                phoneNumber: '01012345678',
                email: 'customer@example.com'
            },
            redirectUrl: window.location.href,
            noticeUrls: billingKeyData.noticeUrls || []
        };

        console.log('빌링키 발급 시작:', billingKeyRequest);

        // 요청 패널에 표시
        updateRequestDisplay(billingKeyRequest);
        updateEndpointDisplay('SDK', 'PortOne.requestIssueBillingKey()');

        // PortOne SDK 호출
        const response = await PortOne.requestIssueBillingKey(billingKeyRequest);

        console.log('빌링키 응답:', response);

        // 결과 표시
        if (response.code != null) {
            // 실패
            displayError({
                code: response.code,
                message: response.message
            });
            throw new Error(response.message);
        } else {
            // 성공
            const result = {
                billingKey: response.billingKey,
                issueId: response.issueId,
                message: '빌링키 발급 성공. 서버에 저장하세요.',
                // 서버 저장용 페이로드 샘플
                serverPayload: {
                    customerId: billingKeyRequest.customer.customerId,
                    billingKey: response.billingKey,
                    issueId: response.issueId,
                    cardInfo: response.card || null
                }
            };

            displaySuccess(result);
            return result;
        }
    } catch (error) {
        console.error('빌링키 발급 오류:', error);
        displayError({
            error: error.message,
            stack: error.stack
        });
        throw error;
    }
}


async function confirmPaymentTemplate(paymentId) {
    try {
        const result = await makeApiRequest('confirm-payment', {
            pathParams: paymentId
        });

        showNotification('결제 확정 성공!', 'success');
        return result;
    } catch (error) {
        showNotification('결제 확정 실패', 'error');
        throw error;
    }
}


async function cancelPaymentTemplate(paymentId, reason = 'Customer request') {
    try {
        const result = await makeApiRequest('cancel-payment', {
            pathParams: paymentId,
            body: {
                reason: reason
            }
        });

        showNotification('결제 취소 성공!', 'success');
        return result;
    } catch (error) {
        showNotification('결제 취소 실패', 'error');
        throw error;
    }
}
