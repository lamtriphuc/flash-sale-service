package com.baas.flashsale.media;

public record CloudinaryUploadResult(
        String secureUrl,
        String optimizedUrl,
        String publicId
) {
}
