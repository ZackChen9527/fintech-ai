package com.codinghappy.fintechai.module.analysis.exception;

import lombok.Getter;

@Getter
public class AnalysisException extends RuntimeException {

    private final String errorCode;
    private final String companyName;

    public AnalysisException(String message) {
        super(message);
        this.errorCode = "ANALYSIS_001";
        this.companyName = null;
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ANALYSIS_002";
        this.companyName = null;
    }

    public AnalysisException(String message, String errorCode, String companyName) {
        super(message);
        this.errorCode = errorCode;
        this.companyName = companyName;
    }

    public AnalysisException(String message, Throwable cause, String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
        this.companyName = null;
    }
}