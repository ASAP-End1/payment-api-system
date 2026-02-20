package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.common.dto.PageResponse;
import com.bootcamp.paymentdemo.point.consts.PointType;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    @Test
    @DisplayName("포인트 내역 조회 - 성공")
    void getPointHistory_성공() throws Exception {

        PointGetResponse response1 = new PointGetResponse(
                1L, 1L, BigDecimal.valueOf(200), PointType.EARNED, LocalDateTime.now(), LocalDate.now().plusYears(1));
        PointGetResponse response2 = new PointGetResponse(
                2L, 2L, BigDecimal.valueOf(100), PointType.SPENT, LocalDateTime.now(), null);

        List<PointGetResponse> content = List.of(response1, response2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<PointGetResponse> page = new PageImpl<>(content, pageable, content.size());
        PageResponse<PointGetResponse> pageResponse = new PageResponse<>(page);

        given(pointService.getPointHistory(anyString(), any(Pageable.class)))
                .willReturn(pageResponse);


        mockMvc.perform(get("/api/points")
                        .with(user("test@test.com"))
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200 OK"))
                .andExpect(jsonPath("$.message").value("포인트 내역 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("포인트 내역 조회 - 실패 (인증X)")
    void getPointHistory_실패() throws Exception {

        mockMvc.perform(get("/api/points")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value("false"))
                .andExpect(jsonPath("$.code").value("401 UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}