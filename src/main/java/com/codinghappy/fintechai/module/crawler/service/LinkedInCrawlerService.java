package com.codinghappy.fintechai.module.crawler.service;


import com.codinghappy.fintechai.common.constant.SystemConstant;
import com.codinghappy.fintechai.module.crawler.dto.CompanyProfileDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class LinkedInCrawlerService implements CrawlerService {

    private final Random random = new Random();

    @Override
    public CompanyProfileDTO crawlCompany(String linkedinUrl) {
        log.info("抓取LinkedIn公司信息: {}", linkedinUrl);

        // 这里应该实现真实的LinkedIn抓取逻辑
        // 由于LinkedIn有反爬机制，实际项目需要：
        // 1. 使用代理IP池
        // 2. 设置合理的请求间隔
        // 3. 处理验证码
        // 4. 解析HTML或使用官方API（如果有权限）

        // 模拟数据返回
        return createMockCompanyProfile(linkedinUrl);
    }

    @Override
    public List<CompanyProfileDTO> batchCrawl(List<String> urls) {
        log.info("批量抓取LinkedIn公司信息，数量: {}", urls.size());
        List<CompanyProfileDTO> results = new ArrayList<>();

        for (String url : urls) {
            try {
                results.add(crawlCompany(url));
                // 避免请求过快
                Thread.sleep(1000 + random.nextInt(1000));
            } catch (Exception e) {
                log.error("抓取失败: {}", url, e);
            }
        }

        return results;
    }

    @Override
    public List<CompanyProfileDTO> searchCompanies(String keyword, int limit) {
        log.info("搜索LinkedIn公司: {}, 限制: {}", keyword, limit);

        // 模拟搜索
        List<CompanyProfileDTO> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, 5); i++) {
            results.add(createMockCompanyProfile(
                    "https://www.linkedin.com/company/mock-" + keyword.toLowerCase() + "-" + i
            ));
        }

        return results;
    }

    @Override
    public boolean supports(String url) {
        return url != null &&
                (url.contains("linkedin.com") || url.contains("领英"));
    }

    @Override
    public String getDataSourceName() {
        return SystemConstant.DATA_SOURCE_LINKEDIN;
    }

    private CompanyProfileDTO createMockCompanyProfile(String linkedinUrl) {
        String[] industries = {"金融科技", "银行", "支付", "保险", "投资", "区块链"};
        String[] locations = {"上海", "北京", "深圳", "香港", "新加坡", "纽约"};
        String[] sizes = {"1-10人", "11-50人", "51-200人", "201-500人", "501-1000人", "1000+人"};

        String companyName = "示例金融科技公司-" + random.nextInt(1000);
        String industry = industries[random.nextInt(industries.length)];
        String location = locations[random.nextInt(locations.length)];
        String size = sizes[random.nextInt(sizes.length)];

        CompanyProfileDTO dto = new CompanyProfileDTO();
        dto.setCompanyName(companyName);
        dto.setLinkedinUrl(linkedinUrl);
        dto.setWebsite("https://www.example-" + random.nextInt(100) + ".com");
        dto.setLocation(location);
        dto.setIndustry(industry);
        dto.setCompanySize(size);
        dto.setEmployeeCount(50 + random.nextInt(950));
        dto.setFoundedYear(2000 + random.nextInt(24));
        dto.setDescription(generateMockDescription(industry, location));
        dto.setTags(generateTags(industry));
        dto.setDataSource(SystemConstant.DATA_SOURCE_LINKEDIN);

        return dto;
    }

    private String generateMockDescription(String industry, String location) {
        String[] templates = {
                "%s是一家位于%s的%s公司，专注于为全球客户提供创新的金融解决方案。",
                "我们在%s的%s公司，致力于通过技术革新%s行业。",
                "作为一家在%s的领先%s企业，我们提供全面的%s服务。"
        };

        String template = templates[random.nextInt(templates.length)];
        String service = industry.contains("支付") ? "跨境支付" :
                industry.contains("借贷") ? "海外借贷" : "金融服务";

        return String.format(template, location, industry, service);
    }

    private String generateTags(String industry) {
        List<String> tags = new ArrayList<>();
        tags.add(industry);

        if (industry.contains("支付")) {
            tags.add("跨境支付");
            tags.add("国际结算");
            tags.add("外汇");
        }
        if (industry.contains("借贷") || industry.contains("金融")) {
            tags.add("海外借贷");
            tags.add("跨境融资");
            tags.add("国际信贷");
        }

        tags.add("金融科技");
        tags.add("B2B");

        return String.join(",", tags);
    }
}