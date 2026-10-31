package com.careconnect.patient.web;

import com.careconnect.patient.dto.PatientDtos.DocumentDto;
import com.careconnect.patient.model.PatientEnums.DocumentCategory;
import com.careconnect.patient.service.DocumentService;
import com.careconnect.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Patient documents")
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/api/patients/{patientId}/documents")
    @PreAuthorize(Authz.CLINICAL_READ)
    public List<DocumentDto> list(@PathVariable Long patientId) {
        return documentService.list(patientId);
    }

    @Operation(summary = "Upload a lab report, scan or other document (PDF/PNG/JPEG/WEBP/TXT, max 10 MB)")
    @PostMapping(value = "/api/patients/{patientId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Authz.DOCUMENT_UPLOAD)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto upload(@PathVariable Long patientId,
                              @RequestParam("file") MultipartFile file,
                              @RequestParam(value = "category", required = false) DocumentCategory category,
                              @RequestParam(value = "description", required = false) String description) {
        return documentService.upload(patientId, file, category, description);
    }

    @GetMapping("/api/documents/{id}/download")
    @PreAuthorize(Authz.CLINICAL_READ)
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentService.DocumentFile file = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.document().getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.document().getFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }

    @DeleteMapping("/api/documents/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
