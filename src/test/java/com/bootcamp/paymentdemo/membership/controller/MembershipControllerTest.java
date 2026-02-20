package com.bootcamp.paymentdemo.membership.controller;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import com.bootcamp.paymentdemo.membership.service.MembershipService;
import com.bootcamp.paymentdemo.security.provider.JwtTokenProvider;
import com.bootcamp.paymentdemo.security.repository.AccessTokenBlacklistRepository;
import com.bootcamp.paymentdemo.security.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.lang.reflect.Constructor;

@WebMvcTest(MembershipController.class)
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MembershipService membershipService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private AccessTokenBlacklistRepository blacklistRepository;

    private Membership normalGrade;
    private Membership vipGrade;
    private Membership vvipGrade;

    @BeforeEach
    void setUp() {
        normalGrade = mock(Membership.class);
        given(normalGrade.getGradeName()).willReturn(MembershipGrade.NORMAL);
        given(normalGrade.getAccRate()).willReturn(new BigDecimal("0.01"));
        given(normalGrade.getMinAmount()).willReturn(BigDecimal.ZERO);

        vipGrade = mock(Membership.class);
        given(vipGrade.getGradeName()).willReturn(MembershipGrade.VIP);
        given(vipGrade.getAccRate()).willReturn(new BigDecimal("0.03"));
        given(vipGrade.getMinAmount()).willReturn(new BigDecimal("50001"));

        vvipGrade = mock(Membership.class);
        given(vvipGrade.getGradeName()).willReturn(MembershipGrade.VVIP);
        given(vvipGrade.getAccRate()).willReturn(new BigDecimal("0.05"));
        given(vvipGrade.getMinAmount()).willReturn(new BigDecimal("150000"));
    }

    @Test
    @DisplayName("멤버십 등급 조회 성공")
    void getAllGrades_Success() throws Exception {

        List<Membership> grades = Arrays.asList(normalGrade, vipGrade, vvipGrade);
        given(membershipService.getAllGradePolices()).willReturn(grades);


        mockMvc.perform(get("/api/membership")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200 OK"))
                .andExpect(jsonPath("$.message").value("멤버십 등급 정책 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))

                .andExpect(jsonPath("$.data[0].gradeName").value("NORMAL"))
                .andExpect(jsonPath("$.data[0].accRate").value(0.01))
                .andExpect(jsonPath("$.data[0].minAmount").value(0))

                .andExpect(jsonPath("$.data[1].gradeName").value("VIP"))
                .andExpect(jsonPath("$.data[1].accRate").value(0.03))
                .andExpect(jsonPath("$.data[1].minAmount").value(50001))

                .andExpect(jsonPath("$.data[2].gradeName").value("VVIP"))
                .andExpect(jsonPath("$.data[2].accRate").value(0.05))
                .andExpect(jsonPath("$.data[2].minAmount").value(150000));

        verify(membershipService).getAllGradePolices();
    }


}