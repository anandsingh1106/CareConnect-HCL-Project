package com.careconnect.clinical.repository;

import com.careconnect.clinical.model.NoteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteTemplateRepository extends JpaRepository<NoteTemplate, Long> {

    List<NoteTemplate> findByActiveTrueOrderByNameAsc();
}
