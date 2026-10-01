package com.tenantportal.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.tenantportal.model.Payment;
import com.tenantportal.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayOutputStream;

/**
 * Module 5 — generates a simple PDF receipt for a successful Payment.
 * Kept intentionally plain (no logo/letterhead) since branding wasn't
 * specified anywhere in the master plan — easy to extend once you have one.
 */
@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final PaymentRepository paymentRepository;

    public byte[] generateReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.SUCCESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No receipt for an unsuccessful payment");
        }

        try {
            Document document = new Document();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 12);

            document.add(new Paragraph("Payment Receipt", titleFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Receipt #: " + payment.getId(), normalFont));
            document.add(new Paragraph("Date: " + payment.getPaidAt(), normalFont));
            document.add(new Paragraph("Tenant: " + payment.getTenant().getName(), normalFont));
            document.add(new Paragraph("Bill #: " + payment.getBill().getId(), normalFont));
            document.add(new Paragraph("Amount Paid: Rs. " + payment.getAmount(), normalFont));
            document.add(new Paragraph("Razorpay Payment ID: " + payment.getRazorpayPaymentId(), normalFont));
            document.add(new Paragraph("Status: " + payment.getStatus(), normalFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate receipt");
        }
    }
}
