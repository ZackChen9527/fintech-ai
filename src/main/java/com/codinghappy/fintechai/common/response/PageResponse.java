package com.codinghappy.fintechai.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private boolean success;

    private String code;

    private String message;

    private List<T> data;

    private PageInfo pageInfo;

    private Long timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {

        private int pageNumber;

        private int pageSize;

        private long totalElements;

        private int totalPages;

        private boolean hasNext;

        private boolean hasPrevious;

        private boolean isFirst;

        private boolean isLast;

        // 计算总页数
        public static int calculateTotalPages(long totalElements, int pageSize) {
            if (pageSize <= 0) {
                return 0;
            }
            return (int) Math.ceil((double) totalElements / pageSize);
        }

        // 判断是否有下一页
        public static boolean hasNextPage(int pageNumber, int totalPages) {
            return pageNumber < totalPages;
        }

        // 判断是否有上一页
        public static boolean hasPreviousPage(int pageNumber) {
            return pageNumber > 1;
        }

        // 判断是否是第一页
        public static boolean isFirstPage(int pageNumber) {
            return pageNumber == 1;
        }

        // 判断是否是最后一页
        public static boolean isLastPage(int pageNumber, int totalPages) {
            return pageNumber == totalPages;
        }
    }

    // 成功响应
    public static <T> PageResponse<T> success(List<T> data, int pageNumber, int pageSize,
                                              long totalElements) {
        int totalPages = PageInfo.calculateTotalPages(totalElements, pageSize);

        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(PageInfo.hasNextPage(pageNumber, totalPages))
                .hasPrevious(PageInfo.hasPreviousPage(pageNumber))
                .isFirst(PageInfo.isFirstPage(pageNumber))
                .isLast(PageInfo.isLastPage(pageNumber, totalPages))
                .build();

        return PageResponse.<T>builder()
                .success(true)
                .code("0000")
                .message("成功")
                .data(data)
                .pageInfo(pageInfo)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 失败响应
    public static <T> PageResponse<T> error(String code, String message) {
        return PageResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    // 空页响应
    public static <T> PageResponse<T> empty(int pageNumber, int pageSize) {
        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(0)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .isFirst(true)
                .isLast(true)
                .build();

        return PageResponse.<T>builder()
                .success(true)
                .code("0000")
                .message("成功")
                .pageInfo(pageInfo)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}