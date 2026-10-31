package com.careconnect.patient.service;

import com.careconnect.common.exception.ApiException;
import com.careconnect.common.web.AuditContext;
import com.careconnect.config.AppProperties;
import com.careconnect.patient.dto.PatientDtos.DocumentDto;
import com.careconnect.patient.mapper.PatientMapper;
import com.careconnect.patient.model.Patient;
import com.careconnect.patient.model.PatientDocument;
import com.careconnect.patient.model.PatientEnums.DocumentCategory;
import com.careconnect.patient.repository.PatientDocumentRepository;
import com.careconnect.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded documents on disk under a random name and keeps metadata in Oracle.
 * Only PDF, images and plain text are accepted; the stored name never contains user input.
 */
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "application/pdf", ".pdf",
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "text/plain", ".txt");

    private final PatientRepository patients;
    private final PatientDocumentRepository documents;
    private final PatientMapper mapper;
    private final AppProperties properties;

    public record DocumentFile(PatientDocument document, Resource resource) {
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> list(Long patientId) {
        patients.require(patientId);
        return documents.findByPatientIdAndDeletedFalseOrderByCreatedAtDesc(patientId).stream().map(mapper::toDto).toList();
    }

    @Transactional
    public DocumentDto upload(Long patientId, MultipartFile file, DocumentCategory category, String description) {
        Patient patient = patients.require(patientId);
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Choose a file to upload.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw ApiException.badRequest("Only PDF, PNG, JPEG, WEBP and plain text files are allowed.");
        }
        if (description != null && description.length() > 500) {
            throw ApiException.badRequest("Description must be at most 500 characters.");
        }

        String storageKey = patient.getId() + "/" + UUID.randomUUID() + extension;
        Path target = resolve(storageKey);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store uploaded file", e);
        }

        PatientDocument document = new PatientDocument();
        document.setPatient(patient);
        document.setCategory(category == null ? DocumentCategory.OTHER : category);
        document.setFileName(safeFileName(file.getOriginalFilename(), extension));
        document.setContentType(contentType);
        document.setSizeBytes(file.getSize());
        document.setStorageKey(storageKey);
        document.setDescription(description);
        return mapper.toDto(documents.save(document));
    }

    @Transactional(readOnly = true)
    public DocumentFile download(Long documentId) {
        PatientDocument document = documents.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> ApiException.notFound("Document", documentId));
        AuditContext.setPatientId(document.getPatient().getId());
        Path path = resolve(document.getStorageKey());
        if (!Files.exists(path)) {
            throw ApiException.notFound("Document file", documentId);
        }
        return new DocumentFile(document, new PathResource(path));
    }

    @Transactional
    public void softDelete(Long documentId) {
        PatientDocument document = documents.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> ApiException.notFound("Document", documentId));
        AuditContext.setPatientId(document.getPatient().getId());
        document.setDeleted(true);
    }

    private Path resolve(String storageKey) {
        Path base = Paths.get(properties.storage().uploadDir()).toAbsolutePath().normalize();
        Path path = base.resolve(storageKey).normalize();
        if (!path.startsWith(base)) {
            throw ApiException.badRequest("Invalid document path.");
        }
        return path;
    }

    private static String safeFileName(String original, String extension) {
        String name = original == null ? "document" + extension : Paths.get(original).getFileName().toString();
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_");
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
