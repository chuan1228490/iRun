package com.ikeu.common.constant;

/**
 * 信用分常量，定义阈值、冻结时长、原因类型。
 * @author ikeu
 * @since 2026/06/17
 */
public final class CreditConstant {

    private CreditConstant() {}

    /** 初始信用分 */
    public static final int CREDIT_INITIAL = 100;

    /** 冻结阈值（低于该值触发冻结，恢复时也恢复至此值） */
    public static final int CREDIT_FREEZE_THRESHOLD = 60;

    /** 冻结天数 */
    public static final int CREDIT_FREEZE_DAYS = 3;

    /** 按时完成加分 */
    public static final int REWARD_ON_TIME = 1;

    /** 提前完成加分 */
    public static final int REWARD_EARLY = 5;

    /** 信用变动原因类型 */
    public static final class ReasonType {
        private ReasonType() {}
        public static final String TIMEOUT = "TIMEOUT";
        public static final String COMPLAINT = "COMPLAINT";
        public static final String MANUAL = "MANUAL";
        public static final String REWARD = "REWARD";
        public static final String RECOVER = "RECOVER";
    }
}
