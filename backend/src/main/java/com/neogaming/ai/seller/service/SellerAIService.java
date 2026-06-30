package com.neogaming.ai.seller.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neogaming.ai.client.AIServiceClient;
import com.neogaming.ai.client.AIServiceProperties;
import com.neogaming.ai.seller.dto.request.SellerOptimizeRequest;
import com.neogaming.ai.seller.dto.response.ImageEnhancementDTO;
import com.neogaming.ai.seller.dto.response.SellerAssistResultDTO;
import com.neogaming.ai.seller.dto.response.SellerBIDTO;
import com.neogaming.common.exception.AIServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerAIService {

    private final AIServiceClient aiServiceClient;
    private final AIServiceProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public SellerAssistResultDTO optimize(SellerOptimizeRequest request) {
        return aiServiceClient.sellerAssist(request);
    }

    public SellerBIDTO biQuery(String query, String sellerId) {
        return aiServiceClient.sellerBiQuery(query, sellerId);
    }

    public SellerAssistResultDTO analyzeImages(
            String name, String category, String brand, String sellerId,
            List<MultipartFile> images) {

        List<String> imagesBase64 = images == null ? List.of() : images.stream()
                .map(img -> {
                    try {
                        return Base64.getEncoder().encodeToString(img.getBytes());
                    } catch (IOException e) {
                        throw new AIServiceException("Error reading image: " + e.getMessage());
                    }
                })
                .toList();

        SellerOptimizeRequest request = new SellerOptimizeRequest(
                name, category, brand, null, null, null,
                List.of(), imagesBase64, sellerId, null
        );
        return aiServiceClient.sellerAssist(request);
    }

    public ImageEnhancementDTO enhanceImages(
            List<MultipartFile> images,
            List<String> operations,
            String productName,
            boolean generatePromotional) {

        String boundary = UUID.randomUUID().toString().replace("-", "");
        byte[] body = buildMultipartBody(boundary, images, operations, productName, generatePromotional);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/api/v1/ai/seller/enhance-images"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("Accept", "application/json")
                    .header("X-Internal-Token", props.getInternalToken())
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 400) {
                throw new AIServiceException("Image enhancement error [" + response.statusCode() + "]: " + response.body());
            }
            return objectMapper.readValue(response.body(), ImageEnhancementDTO.class);
        } catch (AIServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AIServiceException("Image enhancement unreachable: " + e.getMessage());
        }
    }

    private byte[] buildMultipartBody(
            String boundary,
            List<MultipartFile> images,
            List<String> operations,
            String productName,
            boolean generatePromotional) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String CRLF = "\r\n";
            String dashesBoundary = "--" + boundary;

            for (MultipartFile image : images) {
                String filename = image.getOriginalFilename() != null ? image.getOriginalFilename() : "image.jpg";
                String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

                out.write((dashesBoundary + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"images\"; filename=\"" + filename + "\"" + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Type: " + contentType + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(image.getBytes());
                out.write(CRLF.getBytes(StandardCharsets.UTF_8));
            }

            for (String op : operations) {
                out.write((dashesBoundary + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"operations\"" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(op.getBytes(StandardCharsets.UTF_8));
                out.write(CRLF.getBytes(StandardCharsets.UTF_8));
            }

            if (productName != null) {
                out.write((dashesBoundary + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"product_name\"" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
                out.write(productName.getBytes(StandardCharsets.UTF_8));
                out.write(CRLF.getBytes(StandardCharsets.UTF_8));
            }

            out.write((dashesBoundary + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"generate_promotional\"" + CRLF + CRLF).getBytes(StandardCharsets.UTF_8));
            out.write(String.valueOf(generatePromotional).getBytes(StandardCharsets.UTF_8));
            out.write(CRLF.getBytes(StandardCharsets.UTF_8));

            out.write((dashesBoundary + "--" + CRLF).getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException e) {
            throw new AIServiceException("Error building multipart body: " + e.getMessage());
        }
    }
}
