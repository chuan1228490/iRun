package com.ikeu.server.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web 工具类，提供客户端 IP 提取等常用 HTTP 操作方法。
 * @author ikeu
 * @since 2026/06/19
 */
public final class WebUtil {

    private WebUtil() {}

    /**
     * 从 HTTP 请求中提取客户端真实 IP，优先取 X-Forwarded-For 头（兼容反向代理）。
     * 多级代理时取最左侧第一个 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        int comma = ip.indexOf(',');
        if (comma > 0) {
            ip = ip.substring(0, comma).trim();
        }
        return ip != null ? ip : "unknown";
    }
}
