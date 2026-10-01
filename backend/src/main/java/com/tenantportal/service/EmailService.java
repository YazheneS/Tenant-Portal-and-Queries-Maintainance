package com.tenantportal.service;

import com.tenantportal.model.Bill;
import com.tenantportal.model.BillDispute;
import com.tenantportal.model.MaintenanceQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Module 3 (status-change emails) + Module 4 (dispute status-change emails).
 *
 * Uses Spring Mail (JavaMailSender) — needs spring.mail.* properties set
 * (host/port/username/password) pointing at your SMTP provider. Until those
 * are configured, JavaMailSender will fail at send time; every method here
 * catches that and logs instead of throwing, so a missing mail config never
 * breaks the actual business operation (assigning a vendor, resolving a
 * dispute) that triggered the notification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@tenantportal.local}")
    private String fromAddress;

    @Value("${app.mail.owner-address:}")
    private String ownerAddress;

    public void notifyMaintenanceStatusChanged(MaintenanceQuery query) {
        if (query.getTenant() == null || query.getTenant().getEmail() == null) return;
        send(query.getTenant().getEmail(),
                "Update on your maintenance request: " + query.getTitle(),
                "Your request \"" + query.getTitle() + "\" is now " + query.getStatus() + ".");
    }

    public void notifyOwnerNewComplaint(MaintenanceQuery query) {
        if (ownerAddress == null || ownerAddress.isBlank()) return;
        send(ownerAddress,
                "New maintenance request: " + query.getTitle(),
                "A new " + query.getCategory() + " request (" + query.getPriority()
                        + " priority) was raised: " + query.getTitle());
    }

    public void notifyOwnerDisputeRaised(Bill bill, BillDispute dispute) {
        if (ownerAddress == null || ownerAddress.isBlank()) return;
        send(ownerAddress,
                "New bill dispute raised",
                "Tenant " + dispute.getTenant().getName() + " disputed a line item on bill #"
                        + bill.getId() + ". Reason: " + dispute.getReason());
    }

    public void notifyTenantDisputeStatusChanged(BillDispute dispute) {
        if (dispute.getTenant() == null || dispute.getTenant().getEmail() == null) return;
        send(dispute.getTenant().getEmail(),
                "Update on your bill dispute",
                "Your dispute is now " + dispute.getStatus()
                        + (dispute.getOwnerResponse() != null ? ". Owner response: " + dispute.getOwnerResponse() : ""));
    }

    public void notifyRentEscalationUpcoming(String tenantEmail, String unitLabel, java.math.BigDecimal newRent) {
        if (tenantEmail == null) return;
        send(tenantEmail,
                "Upcoming rent change notice",
                "Your rent for " + unitLabel + " will change to " + newRent + " starting next cycle.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Don't let a mail server hiccup break the underlying transaction.
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
