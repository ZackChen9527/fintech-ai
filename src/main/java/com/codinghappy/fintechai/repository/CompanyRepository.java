package com.codinghappy.fintechai.repository;


import com.codinghappy.fintechai.repository.entity.CompanyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    Optional<CompanyEntity> findByName(String name);

    List<CompanyEntity> findByIndustry(String industry);

    List<CompanyEntity> findByLocation(String location);

    Page<CompanyEntity> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT c FROM CompanyEntity c WHERE c.description LIKE %:keyword%")
    List<CompanyEntity> findByDescriptionContaining(@Param("keyword") String keyword);

    @Query("SELECT c FROM CompanyEntity c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate")
    List<CompanyEntity> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT DISTINCT c.industry FROM CompanyEntity c WHERE c.industry IS NOT NULL")
    List<String> findAllIndustries();

    @Query("SELECT COUNT(c) FROM CompanyEntity c WHERE c.isActive = true")
    long countActiveCompanies();
}