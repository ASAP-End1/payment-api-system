package com.bootcamp.paymentdemo.refund;

import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.point.service.PointService;
import com.bootcamp.paymentdemo.product.service.ProductService;
import com.bootcamp.paymentdemo.refund.dto.RefundRequest;
import com.bootcamp.paymentdemo.refund.portOne.client.PortOneRefundClient;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import com.bootcamp.paymentdemo.refund.service.RefundHistoryService;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
public class RefundServiceTest {

    @InjectMocks
    private RefundService refundService;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RefundHistoryService refundHistoryService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PortOneRefundClient portOneRefundClient;

    @Mock
    private ProductService productService;

    @Mock
    private PointService pointService;

    @Mock
    private OrderProductRepository orderProductRepository;

    private Payment payment;
    private Order order;
    private RefundRequest refundRequest;

    @BeforeEach
    public void setUp() {

    }




}
