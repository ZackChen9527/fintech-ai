package com.codinghappy.fintechai.module.crawler.service;

import com.codinghappy.fintechai.repository.CompanyRepository;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final CompanyRepository companyRepository;

    public byte[] exportAnalysisReport(List<AnalysisResultEntity> results) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);

        // ✅ 核心修复1：加载中文字体 (STSong-Light 是 font-asian 包里自带的)
        PdfFont titleFont = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        PdfFont contentFont = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);

        // 封面
        document.add(new Paragraph("Fintech 行业潜客情报分析")
                .setFont(titleFont)
                .setFontSize(24)
                .setBold()
                .setFontColor(new DeviceRgb(0, 51, 102))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));

        document.add(new Paragraph("生成日期: " + LocalDate.now())
                .setFont(contentFont)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(40));

        // ✅ 核心修复2：去重集合
        Set<String> processedCompanies = new HashSet<>();

        for (AnalysisResultEntity result : results) {
            CompanyEntity company = companyRepository.findById(result.getCompanyId()).orElse(null);
            if (company == null) continue;

            // 如果已经导出过这家公司，跳过
            if (processedCompanies.contains(company.getName())) {
                continue;
            }
            processedCompanies.add(company.getName());

            // --- 开始绘制卡片 ---
            Table card = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
            card.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            card.setMarginBottom(25);

            // 头部：公司名 + 分数
            Table header = new Table(UnitValue.createPercentArray(new float[]{7, 3})).useAllAvailableWidth();
            header.setBackgroundColor(new DeviceRgb(240, 245, 255));

            // 公司名 (用中文字体)
            header.addCell(new Cell().add(new Paragraph(company.getName())
                            .setFont(titleFont).setFontSize(14).setBold())
                    .setBorder(Border.NO_BORDER).setPadding(10));

            // 分数
            String scoreText = "意向分: " + result.getPaymentWillingnessScore() + "/10";
            header.addCell(new Cell().add(new Paragraph(scoreText)
                            .setFont(titleFont).setFontSize(12).setBold().setFontColor(new DeviceRgb(200, 0, 0)))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT).setPadding(10));

            card.addCell(new Cell().add(header).setBorder(Border.NO_BORDER));

            // 内容区域
            Table body = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();

            // 业务本质
            body.addCell(createSectionTitle("所属赛道", titleFont));
            body.addCell(createContent(result.getBusinessTypes(), contentFont));

            // 深度情报 (直接展示 analysisReason，它是中文的)
            // 这里我们需要稍微清洗一下格式，把 JSON 里的 \n 换行符处理好
            body.addCell(createSectionTitle("AI 深度情报分析", titleFont));
            body.addCell(createContent(result.getAnalysisReason(), contentFont));

            card.addCell(new Cell().add(body).setBorder(Border.NO_BORDER).setPadding(10));
            document.add(card);
        }

        document.close();
        return out.toByteArray();
    }

    private Cell createSectionTitle(String title, PdfFont font) {
        return new Cell().add(new Paragraph(title)
                        .setFont(font).setFontSize(10).setBold().setFontColor(ColorConstants.DARK_GRAY))
                .setBorder(Border.NO_BORDER)
                .setPaddingTop(5);
    }

    private Cell createContent(String content, PdfFont font) {
        return new Cell().add(new Paragraph(content != null ? content : "暂无数据")
                        .setFont(font).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPaddingBottom(8);
    }
}