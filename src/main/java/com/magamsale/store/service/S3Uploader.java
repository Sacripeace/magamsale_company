package com.magamsale.store.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 로그용
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile multipartFile) throws IOException {
        String originalFileName = multipartFile.getOriginalFilename();
        // 파일명 중복 방지 (uuid_원래이름.jpg)
        String fileName = UUID.randomUUID() + "_" + originalFileName;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        try (InputStream inputStream = multipartFile.getInputStream()) {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, inputStream, metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead)); // 누구나 볼 수 있게 설정
        }

        // 업로드 후 S3 주소(https://...)를 리턴
        String s3Url = amazonS3.getUrl(bucket, fileName).toString();
        log.info("S3 업로드 성공: {}", s3Url);
        return s3Url;
    }
}