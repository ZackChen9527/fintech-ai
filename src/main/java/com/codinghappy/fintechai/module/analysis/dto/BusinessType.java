package com.codinghappy.fintechai.module.analysis.dto;

public enum BusinessType {
    /** 跨境支付业务 */
    CROSS_BORDER_PAYMENT("跨境支付", "提供国际间的货币转移和支付服务"),

    /** 海外借贷业务 */
    OVERSEAS_LOAN("海外借贷", "向海外客户提供贷款或融资服务"),

    /** 其他金融业务 */
    OTHER_FINANCIAL("其他金融", "其他类型的金融服务");

    private final String name;
    private final String description;

    BusinessType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}