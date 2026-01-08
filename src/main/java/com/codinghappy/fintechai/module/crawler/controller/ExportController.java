package com.codinghappy.fintechai.module.crawler.controller;

import com.codinghappy.fintechai.module.crawler.service.PdfExportService;
import com.codinghappy.fintechai.repository.AnalysisResultRepository;
import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/export")
@RequiredArgsConstructor
public class ExportController {

    private final PdfExportService pdfExportService;
    private final AnalysisResultRepository analysisResultRepository;

    @GetMapping("/pdf/latest")
    public ResponseEntity<byte[]> exportLatestReport() throws IOException {
        // 1. 查出最近的 10 条高价值数据 (示例逻辑)
        List<AnalysisResultEntity> results = analysisResultRepository.findAll();
        // 生产环境建议只查最新的，或者加 where score >= 8
        
        // 2. 生成 PDF
        byte[] pdfBytes = pdfExportService.exportAnalysisReport(results);

        // 3. 返回文件流
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "fintech_leads_report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}