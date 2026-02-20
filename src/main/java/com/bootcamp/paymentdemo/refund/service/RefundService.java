package com.bootcamp.paymentdemo.refund.service;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundRequest;
import com.bootcamp.paymentdemo.external.portone.dto.PortOneRefundResponse;
import com.bootcamp.paymentdemo.external.portone.error.PortOneErrorCase;
import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.order.service.OrderService;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.product.service.ProductService;
import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.dto.RefundResponse;
import com.bootcamp.paymentdemo.refund.entity.Refund;
import com.bootcamp.paymentdemo.external.portone.exception.PortOneException;
import com.bootcamp.paymentdemo.refund.exception.RefundException;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.bootcamp.paymentdemo.refund.consts.ErrorEnum.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final RefundHistoryService refundHistoryService;
    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final OrderService orderService;
    private final ProductService productService;
    private final OrderProductRepository orderProductRepository;
    private final MembershipService membershipService;

    @Transactional
    public RefundResponse refundAll(String dbPaymentId, @Valid RefundRequest refundRequest) {

        Payment lockedPayment = paymentRepository.findByDbPaymentIdWithLock(dbPaymentId).orElseThrow(
                () -> new RefundException(ERR_PAYMENT_NOT_FOUND)
        );

        validateRefundable(lockedPayment);

        String refundGroupId = "rfnd-grp-" + UUID.randomUUID();

        refundHistoryService.saveRequestHistory(lockedPayment.getId(), lockedPayment.getTotalAmount(), refundRequest.getReason(), refundGroupId);

        String portOneRefundId = null;

        try {

            PortOneRefundRequest portOneRefundRequest = new PortOneRefundRequest(refundRequest.getReason());

            PortOneRefundResponse portOnePaymentResponse = portOneClient.refundPayment(lockedPayment.getPaymentId(), portOneRefundRequest);

            validatePortOneResponse(portOnePaymentResponse);

            portOneRefundId = portOnePaymentResponse.getCancellation().getId();

            completeRefund(lockedPayment, refundRequest.getReason(), portOneRefundId, refundGroupId);

            return new RefundResponse(lockedPayment.getOrder().getId(), lockedPayment.getOrder().getOrderNumber());

        } catch (PortOneException e) {
            log.error("PortOne API 호출 실패: dbPaymentId={}, refundGroupId={}", dbPaymentId, refundGroupId, e);

            refundHistoryService.saveFailHistory(lockedPayment.getId(), lockedPayment.getTotalAmount(), refundRequest.getReason(), portOneRefundId, refundGroupId);

            throw e;

        } catch (RefundException e) {
            log.error("서버 내부 오류 발생: dbPaymentId={}, refundGroupId={}", dbPaymentId, refundGroupId, e);

            refundHistoryService.saveFailHistory(lockedPayment.getId(), lockedPayment.getTotalAmount(), refundRequest.getReason(), portOneRefundId, refundGroupId);

            throw e;

        } catch (Exception e) {
            log.error("서버 내부 오류 발생: dbPaymentId={}, refundGroupId={}", dbPaymentId, refundGroupId, e);

            refundHistoryService.saveFailHistory(lockedPayment.getId(), lockedPayment.getTotalAmount(), refundRequest.getReason(), portOneRefundId, refundGroupId);

            throw e;
        }
    }


    private void validateRefundable(Payment payment) {

        if(payment.getOrder().isCanceled()) {
            throw new RefundException(ERR_REFUND_ALREADY_PROCESSED);
        }

        if(payment.isRefund()) {
            throw new RefundException(ERR_REFUND_ALREADY_PROCESSED);
        }

        if(!payment.getOrder().isAwaitingConfirmation()) {
            throw new RefundException(ERR_REFUND_INVALID_STATUS);
        }

        if(!payment.isPaid()) {
            throw new RefundException(ERR_REFUND_INVALID_STATUS);
        }

    }


    private void completeRefund(Payment payment, String reason, String portOneRefundId, String refundGroupId) {
        Refund completedRefund = Refund.createCompleted(
                payment.getId(), payment.getTotalAmount(), reason,  portOneRefundId,  refundGroupId
        );


        refundRepository.save(completedRefund);


        payment.refund();
        orderService.cancelOrder(payment.getOrder().getId(), reason);


        List<OrderProduct> orderProducts = orderProductRepository.findByOrder_Id(payment.getOrder().getId());

        orderProducts.forEach(orderProduct ->
                productService.increaseStock(orderProduct.getProductId(), orderProduct.getCount())
        );


        membershipService.handleRefund(
                payment.getOrder().getUser().getUserId(),
                payment.getOrder().getFinalAmount(),
                payment.getOrder().getId()
        );
    }

    private void validatePortOneResponse(PortOneRefundResponse response) {
        if (response == null || response.getCancellation() == null) {
            throw new PortOneException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PortOne 응답이 비어있습니다"
            );
        }
        if (!"SUCCEEDED".equals(response.getCancellation().getStatus())) {

            HttpStatus status =
                    PortOneErrorCase.caseToHttpStatus(
                            response.getCancellation().getType()
                    );

            throw new PortOneException(
                    status,
                    response.getCancellation().getMessage()
            );
        }
    }
}
