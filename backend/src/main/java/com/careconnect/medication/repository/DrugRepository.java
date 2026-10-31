package com.careconnect.medication.repository;

import com.careconnect.medication.model.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DrugRepository extends JpaRepository<Drug, Long> {

    @Query("""
            select d from Drug d
            where (:active is null or d.active = :active)
              and (:q is null or lower(d.name) like :q or lower(d.genericName) like :q
                   or lower(d.code) like :q or lower(d.drugClass) like :q)
            """)
    Page<Drug> search(@Param("q") String q, @Param("active") Boolean active, Pageable pageable);

    @Query("select d from Drug d where d.active = true and d.stockQty <= d.reorderLevel order by d.stockQty asc")
    List<Drug> findLowStock();

    @Query("select count(d) from Drug d where d.active = true and d.stockQty <= d.reorderLevel")
    long countLowStock();

    boolean existsByCodeIgnoreCase(String code);
}
