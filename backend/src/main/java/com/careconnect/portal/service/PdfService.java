package com.careconnect.portal.service;

import com.careconnect.clinical.dto.ClinicalDtos.VitalDto;
import com.careconnect.medication.dto.MedicationDtos.PrescriptionDto;
import com.careconnect.patient.dto.PatientDtos.PatientDto;
import com.careconnect.portal.dto.PortalDtos.VisitSummaryDto;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds printable PDFs (visit summary and prescription) with OpenPDF. */
@Service
public class PdfService {

    private static final Color BRAND = new Color(8, 126, 121);
    private static final Color MUTED = new Color(100, 116, 122);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
    private final Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, BRAND);
    private final Font label = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, MUTED);
    private final Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private final Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

    public byte[] visitSummary(PatientDto patient, VisitSummaryDto visit) {
        return build(doc -> {
            header(doc, "Visit Summary");
            patientBlock(doc, patient);

            section(doc, "Visit");
            PdfPTable table = grid();
            cell(table, "Date", visit.encounterDate().format(DATE_TIME));
            cell(table, "Doctor", visit.providerName());
            cell(table, "Department", visit.departmentName());
            cell(table, "Visit type", visit.encounterType().name().replace('_', ' '));
            doc.add(table);
            text(doc, "Reason for visit", visit.chiefComplaint());

            section(doc, "Diagnoses");
            list(doc, visit.diagnoses(), "No diagnosis recorded.");

            VitalDto v = visit.vitals();
            if (v != null) {
                section(doc, "Vital signs");
                PdfPTable vitals = grid();
                cell(vitals, "Blood pressure", v.systolic() == null ? "-" : v.systolic() + "/" + v.diastolic() + " mmHg");
                cell(vitals, "Pulse", v.pulse() == null ? "-" : v.pulse() + " /min");
                cell(vitals, "Temperature", v.temperature() == null ? "-" : v.temperature() + " °C");
                cell(vitals, "SpO2", v.spo2() == null ? "-" : v.spo2() + " %");
                cell(vitals, "Weight", v.weightKg() == null ? "-" : v.weightKg() + " kg");
                cell(vitals, "BMI", v.bmi() == null ? "-" : v.bmi().toPlainString());
                doc.add(vitals);
            }

            section(doc, "Doctor's assessment");
            doc.add(new Paragraph(orDash(visit.assessment()), body));
            section(doc, "Plan and advice");
            doc.add(new Paragraph(orDash(visit.plan()), body));
            section(doc, "Medicines prescribed");
            list(doc, visit.medications(), "No medicines prescribed at this visit.");
            footer(doc);
        });
    }

    public byte[] prescription(PatientDto patient, PrescriptionDto rx) {
        return build(doc -> {
            header(doc, "Prescription " + rx.rxNo());
            patientBlock(doc, patient);

            section(doc, "Rx");
            PdfPTable table = grid();
            cell(table, "Medicine", rx.drugName() + " (" + rx.genericName() + ")");
            cell(table, "Dose", rx.dose());
            cell(table, "Route", rx.route().name());
            cell(table, "Frequency", rx.frequency());
            cell(table, "Duration", rx.durationDays() + " days");
            cell(table, "Quantity", String.valueOf(rx.quantity()));
            cell(table, "Status", rx.status().name().replace('_', ' '));
            cell(table, "Prescribed on", rx.createdAt() == null ? "-" : rx.createdAt().format(DATE_TIME));
            doc.add(table);
            text(doc, "Instructions", rx.instructions());

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Prescribed by: " + rx.prescriberName(), body));
            if (rx.verifiedByName() != null) {
                doc.add(new Paragraph("Verified by pharmacist: " + rx.verifiedByName(), body));
            }
            if (rx.dispensedAt() != null) {
                doc.add(new Paragraph("Dispensed on " + rx.dispensedAt().format(DATE_TIME) + " (" + rx.dispensedQty() + " units)", body));
            }
            footer(doc);
        });
    }

    // ------------------------------------------------------------------ helpers

    private interface Content {
        void write(Document doc) throws DocumentException;
    }

    private byte[] build(Content content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 42, 42, 42, 42);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            content.write(doc);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not create PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private void header(Document doc, String documentTitle) throws DocumentException {
        Paragraph brand = new Paragraph("CareConnect", title);
        doc.add(brand);
        doc.add(new Paragraph("Patient-Provider Electronic Health Record", small));
        Paragraph t = new Paragraph(documentTitle, heading);
        t.setSpacingBefore(10);
        doc.add(t);
        doc.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.8f, 100, BRAND, Element.ALIGN_CENTER, -4)));
    }

    private void patientBlock(Document doc, PatientDto p) throws DocumentException {
        PdfPTable table = grid();
        table.setSpacingBefore(10);
        cell(table, "Patient", p.fullName());
        cell(table, "MRN", p.mrn());
        cell(table, "Date of birth", p.dateOfBirth().format(DATE) + " (" + p.age() + " y)");
        cell(table, "Gender", p.gender().name());
        doc.add(table);
    }

    private void section(Document doc, String name) throws DocumentException {
        Paragraph p = new Paragraph(name, heading);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private void text(Document doc, String name, String value) throws DocumentException {
        if (value == null || value.isBlank()) {
            return;
        }
        Paragraph p = new Paragraph();
        p.setSpacingBefore(6);
        p.add(new Phrase(name + ": ", label));
        p.add(new Phrase(value, body));
        doc.add(p);
    }

    private void list(Document doc, List<String> items, String empty) throws DocumentException {
        if (items == null || items.isEmpty()) {
            doc.add(new Paragraph(empty, body));
            return;
        }
        for (String item : items) {
            doc.add(new Paragraph("•  " + item, body));
        }
    }

    private PdfPTable grid() {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        return table;
    }

    private void cell(PdfPTable table, String name, String value) {
        PdfPCell nameCell = new PdfPCell(new Phrase(name, label));
        nameCell.setBorder(0);
        nameCell.setPadding(3);
        PdfPCell valueCell = new PdfPCell(new Phrase(orDash(value), body));
        valueCell.setBorder(0);
        valueCell.setPadding(3);
        table.addCell(nameCell);
        table.addCell(valueCell);
    }

    private void footer(Document doc) throws DocumentException {
        Paragraph p = new Paragraph("Generated " + LocalDateTime.now().format(DATE_TIME)
                + " from the CareConnect patient portal. This document contains confidential health information.", small);
        p.setSpacingBefore(24);
        doc.add(p);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
