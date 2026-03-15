package com.example.autopost.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class ThreadsApiService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadsApiService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    @Value("${threads.access.token}")
    private String accessToken;

    public ThreadsApiService() {
        this.webClient = WebClient.builder().baseUrl("https://graph.threads.net").build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public String createMediaContainer(String mediaType, String text, String mediaUrl) {
        logger.info("Creating media container for mediaType: {}", mediaType);

        BodyInserters.FormInserter<String> form = BodyInserters.fromFormData("media_type", mediaType)
                .with("text", text)
                .with("access_token", accessToken);

        if ("IMAGE".equalsIgnoreCase(mediaType)) {
            form = form.with("image_url", mediaUrl == null ? "" : mediaUrl);
        } else if ("VIDEO".equalsIgnoreCase(mediaType)) {
            form = form.with("video_url", mediaUrl == null ? "" : mediaUrl);
        } else {
            form = form.with("media_url", mediaUrl == null ? "" : mediaUrl);
        }

        Mono<String> response = webClient.post()
                .uri("/me/threads")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .bodyToMono(String.class);

        try {
            String res = response.block();
            JsonNode node = objectMapper.readTree(res);
            String id = node.get("id").asText();
            logger.info("Created media container with ID: {}", id);
            return id;
        } catch (WebClientResponseException e) {
            logger.error("Threads API returned {} when creating media container: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Failed to create media container", e);
        }
        return null;
    }

    public boolean waitUntilFinished(String creationId) {
        String url = "/" + creationId;
        for (int attempt = 0; attempt < 10; attempt++) {
            logger.info("POLL MEDIA STATUS GET {} - attempt {}", url, attempt + 1);
            try {
                String res = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(url)
                                .queryParam("fields", "status")
                                .queryParam("access_token", accessToken)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                logger.info("POLL MEDIA STATUS Response body: {}", res);
                JsonNode node = objectMapper.readTree(res);
                String status = node.path("status").asText();
                if ("FINISHED".equalsIgnoreCase(status)) {
                    logger.info("Media processing FINISHED");
                    return true;
                }
                if ("ERROR".equalsIgnoreCase(status)) {
                    logger.error("Media processing ERROR");
                    return false;
                }
            } catch (WebClientResponseException e) {
                logger.error("Threads API returned {} when polling media status: {}", e.getStatusCode(), e.getResponseBodyAsString());
                return false;
            } catch (Exception e) {
                logger.error("Failed to poll media status", e);
                return false;
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        logger.error("Timeout after 30 seconds waiting for media status");
        return false;
    }

    public String publishMediaContainer(String containerId) {
        logger.info("Publishing media container with ID: {}", containerId);
        Mono<String> response = webClient.post()
                .uri("/me/threads_publish")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("creation_id", containerId)
                        .with("access_token", accessToken))
                .retrieve()
                .bodyToMono(String.class);

        try {
            String res = response.block();
            JsonNode node = objectMapper.readTree(res);
            String id = node.get("id").asText();
            logger.info("Published media with ID: {}", id);
            return id;
        } catch (WebClientResponseException e) {
            logger.error("Threads API returned {} when publishing media container: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Failed to publish media container: {}", containerId, e);
        }
        return null;
    }
}