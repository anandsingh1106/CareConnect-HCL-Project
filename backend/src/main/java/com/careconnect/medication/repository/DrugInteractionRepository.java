package com.careconnect.medication.repository;

import com.careconnect.medication.model.DrugInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    /** Interactions between one drug and any drug in a set, whichever column each drug is stored in. */
    @Query("""
            select i from DrugInteraction i
            where (i.drugA.id = :drugId and i.drugB.id in :otherIds)
               or (i.drugB.id = :drugId and i.drugA.id in :otherIds)
            """)
    List<DrugInteraction> findBetween(@Param("drugId") Long drugId, @Param("otherIds") Collection<Long> otherIds);

    @Query("select i from DrugInteraction i where i.drugA.id = :drugId or i.drugB.id = :drugId")
    List<DrugInteraction> findForDrug(@Param("drugId") Long drugId);

    @Query("select i from DrugInteraction i order by i.drugA.name, i.drugB.name")
    List<DrugInteraction> findAllOrdered();

    @Query("""
            select count(i) > 0 from DrugInteraction i
            where (i.drugA.id = :a and i.drugB.id = :b) or (i.drugA.id = :b and i.drugB.id = :a)
            """)
    boolean existsPair(@Param("a") Long a, @Param("b") Long b);
}
