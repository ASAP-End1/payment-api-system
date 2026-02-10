package com.bootcamp.paymentdemo.common.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {
    private List<T> content;
    private int pageNum;
    private int pageSize;
    private long total;
    private int totalPages;

    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.pageNum = page.getNumber() + 1;
        this.pageSize = page.getSize();
        this.total = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}