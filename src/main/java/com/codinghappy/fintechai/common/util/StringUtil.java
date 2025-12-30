package com.codinghappy.fintechai.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public class StringUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9\\s-]{8,20}$");
    private static final Pattern URL_PATTERN =
            Pattern.compile("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");

    /**
     * 检查字符串是否为空或空白
     */
    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    /**
     * 检查字符串是否非空
     */
    public static boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * 截取字符串，添加省略号
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 提取关键词
     */
    public static List<String> extractKeywords(String text, int maxKeywords) {
        if (isBlank(text)) {
            return Collections.emptyList();
        }

        // 移除标点符号，转换为小写
        String cleaned = text.toLowerCase()
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", " ");

        // 分割单词
        String[] words = cleaned.split("\\s+");

        // 统计词频
        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            if (word.length() > 1 && !isStopWord(word)) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        // 按词频排序
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(wordCount.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // 返回前N个关键词
        List<String> keywords = new ArrayList<>();
        for (int i = 0; i < Math.min(sorted.size(), maxKeywords); i++) {
            keywords.add(sorted.get(i).getKey());
        }

        return keywords;
    }

    /**
     * 判断是否为停用词
     */
    private static boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "了", "在", "是", "我", "有", "和", "就",
                "不", "人", "都", "一", "一个", "上", "也", "很",
                "到", "说", "要", "去", "你", "会", "着", "没有",
                "看", "好", "自己", "这", "the", "and", "of", "to",
                "a", "in", "is", "you", "that", "it", "he", "was",
                "for", "on", "are", "as", "with", "his", "they", "i",
                "at", "be", "this", "have", "from", "or", "one", "had",
                "by", "word", "but", "what", "some", "we", "can", "out",
                "other", "were", "all", "there", "when", "up", "use",
                "your", "how", "said", "an", "each", "she"
        ));

        return stopWords.contains(word.toLowerCase());
    }

    /**
     * 验证邮箱格式
     */
    public static boolean isValidEmail(String email) {
        return isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 验证电话号码格式
     */
    public static boolean isValidPhone(String phone) {
        return isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 验证URL格式
     */
    public static boolean isValidUrl(String url) {
        return isNotBlank(url) && URL_PATTERN.matcher(url).matches();
    }

    /**
     * 隐藏敏感信息
     */
    public static String maskSensitiveInfo(String text) {
        if (isBlank(text)) {
            return text;
        }

        // 邮箱脱敏
        if (isValidEmail(text)) {
            String[] parts = text.split("@");
            if (parts.length == 2) {
                String username = parts[0];
                if (username.length() <= 2) {
                    return "*@" + parts[1];
                }
                return username.charAt(0) + "***" +
                        username.charAt(username.length() - 1) + "@" + parts[1];
            }
        }

        // 手机号脱敏
        if (isValidPhone(text)) {
            String digits = text.replaceAll("[^0-9]", "");
            if (digits.length() >= 7) {
                return digits.substring(0, 3) + "****" +
                        digits.substring(digits.length() - 4);
            }
        }

        // 默认脱敏：显示前2后2，中间用*代替
        if (text.length() <= 4) {
            return "****";
        }
        return text.substring(0, 2) + "***" +
                text.substring(text.length() - 2);
    }

    /**
     * 计算字符串相似度
     */
    public static double similarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }
        if (str1.equals(str2)) {
            return 1.0;
        }

        // 简单的编辑距离相似度计算
        int len1 = str1.length();
        int len2 = str2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[j][0] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        int maxLen = Math.max(len1, len2);
        if (maxLen == 0) {
            return 1.0;
        }

        return 1.0 - (double) dp[len1][len2] / maxLen;
    }

    /**
     * 将List转换为逗号分隔的字符串
     */
    public static String join(List<?> list) {
        return join(list, ",");
    }

    /**
     * 将List转换为指定分隔符的字符串
     */
    public static String join(List<?> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(list.get(i));
        }

        return sb.toString();
    }

    /**
     * 将逗号分隔的字符串转换为List
     */
    public static List<String> split(String str) {
        return split(str, ",");
    }

    /**
     * 将字符串按指定分隔符转换为List
     */
    public static List<String> split(String str, String delimiter) {
        if (isBlank(str)) {
            return Collections.emptyList();
        }

        String[] parts = str.split(delimiter);
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (isNotBlank(trimmed)) {
                result.add(trimmed);
            }
        }

        return result;
    }
}