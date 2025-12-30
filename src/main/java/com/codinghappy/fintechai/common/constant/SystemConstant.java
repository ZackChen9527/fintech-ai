package com.codinghappy.fintechai.common.constant;

public class SystemConstant {

    // 系统配置
    public static final String SYSTEM_NAME = "金融精准获客系统";
    public static final String VERSION = "1.0.0";

    // 分析相关常量
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;
    public static final int MAX_BATCH_ANALYSIS_SIZE = 100;

    // 评分相关
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 10;
    public static final int HIGH_SCORE_THRESHOLD = 7;

    // 业务类型
    public static final String BUSINESS_CROSS_BORDER_PAYMENT = "跨境支付";
    public static final String BUSINESS_OVERSEAS_LOAN = "海外借贷";
    public static final String BUSINESS_OTHER = "其他金融";

    // API相关
    public static final String DEEPSEEK_API_BASE_URL = "https://api.deepseek.com";
    public static final String DEEPSEEK_MODEL = "deepseek-chat";
    public static final int DEEPSEEK_MAX_TOKENS = 1000;

    // 缓存相关
    public static final int CACHE_EXPIRE_MINUTES = 30;
    public static final String CACHE_PREFIX_ANALYSIS = "analysis:";
    public static final String CACHE_PREFIX_COMPANY = "company:";

    // 限流相关
    public static final int RATE_LIMIT_DEFAULT_TOKENS = 5;
    public static final int RATE_LIMIT_BURST_CAPACITY = 10;
    public static final long RATE_LIMIT_TIMEOUT_MS = 1000L;

    // 重试相关
    public static final int RETRY_MAX_ATTEMPTS = 3;
    public static final long RETRY_INITIAL_INTERVAL_MS = 1000L;
    public static final double RETRY_MULTIPLIER = 2.0;

    // 错误码
    public static final String ERROR_CODE_RATE_LIMIT = "RATE_LIMIT_001";
    public static final String ERROR_CODE_API_FAILURE = "API_FAILURE_001";
    public static final String ERROR_CODE_INVALID_INPUT = "INPUT_ERROR_001";
    public static final String ERROR_CODE_ANALYSIS_FAILED = "ANALYSIS_ERROR_001";

    // 数据源
    public static final String DATA_SOURCE_LINKEDIN = "linkedin";
    public static final String DATA_SOURCE_TIANYANCHA = "tianyancha";
    public static final String DATA_SOURCE_MANUAL = "manual";

    // 任务调度
    public static final String JOB_GROUP_ANALYSIS = "analysis-group";
    public static final String JOB_GROUP_CRAWLER = "crawler-group";
    public static final String TRIGGER_GROUP_DEFAULT = "default-group";
}