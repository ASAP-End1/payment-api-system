package com.bootcamp.paymentdemo.refund;

import com.bootcamp.paymentdemo.external.portone.client.PortOneClient;
import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.repository.MembershipRepository;
import com.bootcamp.paymentdemo.order.consts.OrderStatus;
import com.bootcamp.paymentdemo.order.entity.Order;
import com.bootcamp.paymentdemo.orderProduct.entity.OrderProduct;
import com.bootcamp.paymentdemo.orderProduct.repository.OrderProductRepository;
import com.bootcamp.paymentdemo.payment.consts.PaymentStatus;
import com.bootcamp.paymentdemo.payment.entity.Payment;
import com.bootcamp.paymentdemo.payment.repository.PaymentRepository;
import com.bootcamp.paymentdemo.product.entity.Product;
import com.bootcamp.paymentdemo.product.repository.ProductRepository;
import com.bootcamp.paymentdemo.refund.repository.RefundRepository;
import com.bootcamp.paymentdemo.refund.service.RefundService;
import com.bootcamp.paymentdemo.user.entity.User;
import com.bootcamp.paymentdemo.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@SpringBootTest
@Transactional
public class RefundServiceTest {

    @Autowired
    private RefundService refundService;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderProductRepository orderProductRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private PortOneClient portOneClient;

    private User user;
    private Payment payment;
    private Order order;
    private Product product;

    @BeforeEach
    void setUp() {
        Membership membership = membershipRepository.findByGradeName(MembershipGrade.NORMAL).orElseThrow();

        user = userRepository.save(
                User.register(
                        "test@test.com",
                        "password",
                        "홍길동",
                        "010-1234-1234",
                        membership
                )
        );

        order = Order.builder()
                .user(user)
                .totalAmount(new BigDecimal(10000))
                .usedPoints(new BigDecimal(1000))
                .finalAmount(new BigDecimal(9000))
                .orderStatus(OrderStatus.PENDING_CONFIRMATION)
                .build();

        payment = Payment.builder()
                .order(order)
                .dbPaymentId("db1")
                .totalAmount(new BigDecimal(9000))
                .status(PaymentStatus.PAID)
                .build();

        paymentRepository.save(payment);
        
        /*
            현재 Product 생성자 외부 사용 못함 
         */
        

        OrderProduct orderProduct = OrderProduct.builder()
                .order(order)
                .productId(product.getId())
                .count(2)
                .build();

        orderProductRepository.save(orderProduct);
    }

}