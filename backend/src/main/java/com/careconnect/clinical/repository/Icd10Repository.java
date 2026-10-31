package com.careconnect.clinical.repository;

import com.careconnect.clinical.model.Icd10Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Icd10Repository extends JpaRepository<Icd10Code, String> {

    /** Type-ahead search by code prefix or description words. */
    @Query("""
            select c from Icd10Code c
            where (:active is null or c.active = :active)
              and (:q is null or upper(c.code) like :codePrefix or lower(c.description) like :q)
            """)
    Page<Icd10Code> search(@Param("q") String q, @Param("codePrefix") String codePrefix,
                           @Param("active") Boolean active, Pageable pageable);
}
