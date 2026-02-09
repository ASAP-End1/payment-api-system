package com.bootcamp.paymentdemo.payment.service;

import com.bootcamp.paymentdemo.payment.dto.PaymentConfirmResponse;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateRequest;
import com.bootcamp.paymentdemo.payment.dto.PaymentCreateResponse;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    // 실제 PortOne SDK를 사용하려면 아래 클라이언트가 주입되어야 합니다.
    // private final IamportClient iamportClient;

    // 결제 준비 Order랑 다른 부분들 개발 되면 이후 추가하겠음
    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        // [임시 ID 생성] PortOne 결제창의 merchant_uid로 사용됨
        String merchantUid = "order_mid_" + System.currentTimeMillis();

        // 실제로는 여기서 OrderRepository를 통해 주문 객체를 가져와야 합니다.
        // Payment payment = Payment.builder()
        //        .paymentId(merchantUid) // 아직 imp_uid가 없으므로 우리 임시 ID 부여
        //        .totalAmount(request.getTotalAmount())
        //        .pointsToUse(request.getPointsToUse())
        //        .status(PaymentStatus.PENDING)
        //        .build();
        // paymentRepository.save(payment);

        log.info("결제 대기 데이터 생성 완료: {}", merchantUid);
        return new PaymentCreateResponse(true, merchantUid, "PENDING");
    }


//    @Transactional
//    public PaymentConfirmResponse confirmPayment(String impUid) {
//        try {
//            // [SDK 통신] PortOne 서버에서 실제 결제 내역 조회
//            // IamportResponse<Payment> response = iamportClient.paymentByImpUid(impUid);
//            // BigDecimal realPaidAmount = response.getResponse().getAmount(); // 실제 결제된 금액
//
//            // [DB 조회] 우리가 가지고 있는 결제 정보 조회 (merchant_uid 또는 미리 저장한 imp_uid로)
//            // Payment payment = paymentRepository.findByPaymentId(impUid)
//            //        .orElseThrow(() -> new IllegalArgumentException("결제 내역이 없습니다."));
//
//            // [검증] DB에 저장된 금액과 실제 포트원에서 긁힌 금액이 같은지 확인
//            // if (payment.getTotalAmount().compareTo(realPaidAmount) == 0) {
//            //     payment.updateStatus(PaymentStatus.PAID); // 상태 변경
//            //     return new PaymentConfirmResponse(true, payment.getOrder().getOrderId(), "PAID");
//            // }
//
//            log.info("결제 검증 및 완료 처리: {}", impUid);
//            return new PaymentConfirmResponse(true, "TEMP_ORDER_ID", "PAID");
//
//        } catch (Exception e) {
//            log.error("결제 검증 중 오류 발생: {}", e.getMessage());
//            return new PaymentConfirmResponse(false, null, "FAILED");
//        }
//    }
}