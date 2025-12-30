package com.codinghappy.fintechai.repository;

import com.codinghappy.fintechai.repository.entity.AnalysisResultEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResultEntity, Long> {

    List<AnalysisResultEntity> findByCompanyId(Long companyId);

    AnalysisResultEntity findTopByCompanyIdOrderByAnalysisTimeDesc(Long companyId);

    Page<AnalysisResultEntity> findBySuccessTrue(Pageable pageable);

    @Query("SELECT a FROM AnalysisResultEntity a WHERE a.paymentWillingnessScore >= :minScore")
    List<AnalysisResultEntity> findByScoreGreaterThanEqual(@Param("minScore") Integer minScore);

    @Query("SELECT a FROM AnalysisResultEntity a WHERE a.analysisTime >= :startTime AND a.analysisTime <= :endTime")
    List<AnalysisResultEntity> findByAnalysisTimeBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT AVG(a.paymentWillingnessScore) FROM AnalysisResultEntity a WHERE a.success = true")
    Double findAverageScore();

    @Query("SELECT COUNT(a) FROM AnalysisResultEntity a WHERE a.success = true AND a.paymentWillingnessScore >= 7")
    Long countHighScoreResults();

    @Query("DELETE FROM AnalysisResultEntity a WHERE a.analysisTime < :cutoffTime")
    void deleteOldResults(@Param("cutoffTime") LocalDateTime cutoffTime);
}