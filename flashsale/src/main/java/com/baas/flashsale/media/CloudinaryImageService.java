package com.baas.flashsale.media;

import com.baas.flashsale.common.BusinessException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CloudinaryImageService {
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final Cloudinary cloudinary;

    public CloudinaryUploadResult uploadCampaignThumbnail(Long campaignId, MultipartFile file) {
        validateImage(file);

        try {
            Map<?, ?> result = uploadWithRetry(file, "campaigns/" + campaignId);
            String publicId = String.valueOf(result.get("public_id"));
            String secureUrl = String.valueOf(result.get("secure_url"));
            return new CloudinaryUploadResult(secureUrl, optimizedUrl(publicId), publicId);
        } catch (IOException ex) {
            throw new BusinessException("THUMBNAIL_UPLOAD_FAILED", HttpStatus.BAD_GATEWAY, "Could not upload thumbnail");
        }
    }

    public void deleteQuietly(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, Map.of("invalidate", true));
        } catch (IOException ignored) {
            // Upload replacement should not fail only because old media cleanup failed.
        }
    }

    private Map<?, ?> uploadWithRetry(MultipartFile file, String folder) throws IOException {
        IOException lastError = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return cloudinary.uploader().upload(file.getBytes(), Map.of(
                        "folder", folder,
                        "resource_type", "image",
                        "overwrite", true
                ));
            } catch (IOException ex) {
                lastError = ex;
            }
        }
        throw lastError;
    }

    private String optimizedUrl(String publicId) {
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation<>()
                        .width(400)
                        .height(300)
                        .crop("fill")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Thumbnail file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Thumbnail file must be <= 5MB");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Only jpg, png and webp images are allowed");
        }
    }
}
