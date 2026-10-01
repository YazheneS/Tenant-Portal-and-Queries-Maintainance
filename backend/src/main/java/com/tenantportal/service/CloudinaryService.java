package com.tenantportal.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Map;

/**
 * Module 3 — photo uploads for maintenance complaints.
 *
 * Needs three properties set (put these in Doppler, not application.properties
 * directly — they're real secrets): cloudinary.cloud-name, cloudinary.api-key,
 * cloudinary.api-secret. Get them from cloudinary.com dashboard after signing
 * up (free tier is plenty for this).
 */
@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Value("${cloudinary.cloud-name:}") String cloudName,
                              @Value("${cloudinary.api-key:}") String apiKey,
                              @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @SuppressWarnings("unchecked")
    public String uploadComplaintPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided");
        }
        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "tenant-portal/complaints"));
            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Cloudinary upload failed", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Photo upload failed");
        }
    }
}
