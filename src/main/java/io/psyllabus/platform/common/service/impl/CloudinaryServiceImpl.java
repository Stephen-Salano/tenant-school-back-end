package io.psyllabus.platform.common.service.impl;

import io.psyllabus.platform.common.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements StorageService {

//    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(byte[] bytes, String folder) throws IOException {
        return "";
//        log.debug("Uploading image to Cloudinary folder: {}", folder);
//        Map<?, ?> uploadResult = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
//                "folder", folder,
//                "resource_type", "auto"
//        ));
//        String url = (String) uploadResult.get("secure_url");
//        log.info("Image uploaded successfully: {}", url);
//        return url;
    }

    @Override
    public void deleteImage(String publicId) throws IOException {
//        log.info("Deleting image from Cloudinary: {}", publicId);
//        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
