package com.example.autopost.service;

import com.example.autopost.model.PageResponse;
import com.example.autopost.model.Post;
import com.example.autopost.model.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    private final ObjectMapper objectMapper;
    private final ThreadsApiService threadsApiService;
    private final String dataDir = "data";

    public PostService(ThreadsApiService threadsApiService) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.threadsApiService = threadsApiService;
        try {
            Files.createDirectories(Paths.get(dataDir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        File dir = new File(dataDir);
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".json")) {
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        Post post = objectMapper.readValue(content, Post.class);
                        if (post.getCreatedAt() == null) {
                            post.setCreatedAt(post.getId() != null ? getCreatedAtFromFile(file.toPath()) : LocalDateTime.now());
                        }
                        posts.add(post);
                    } catch (IOException e) {
                        logger.error("Failed to read post file {}", file.getName(), e);
                    }
                }
            }
        }
        return posts;
    }

    public PageResponse<Post> getPosts(int page, int size) {

        PageResponse<Post> response = new PageResponse<>();
        List<Post> content = new ArrayList<>();

        File dir = new File(dataDir);

        if (!dir.exists()) {
            response.setContent(content);
            response.setCurrentPage(page);
            response.setTotalPages(0);
            response.setTotalElements(0);
            return response;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (files == null || files.length == 0) {
            response.setContent(content);
            response.setCurrentPage(page);
            response.setTotalPages(0);
            response.setTotalElements(0);
            return response;
        }

        // sort theo timestamp id (mới nhất trước)
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        int totalElements = files.length;
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int start = page * size;
        int end = Math.min(start + size, totalElements);

        if (start >= totalElements) {
            response.setContent(content);
            response.setCurrentPage(page);
            response.setTotalPages(totalPages);
            response.setTotalElements(totalElements);
            return response;
        }

        for (int i = start; i < end; i++) {

            File file = files[i];

            try {

                String contentStr = Files.readString(file.toPath());

                Post post = objectMapper.readValue(contentStr, Post.class);

                if (post.getCreatedAt() == null) {
                    post.setCreatedAt(
                            post.getId() != null
                                    ? getCreatedAtFromFile(file.toPath())
                                    : LocalDateTime.now()
                    );
                }

                content.add(post);

            } catch (IOException e) {
                logger.error("Failed to read post file {}", file.getName(), e);
            }
        }

        response.setContent(content);
        response.setCurrentPage(page);
        response.setTotalPages(totalPages);
        response.setTotalElements(totalElements);

        return response;
    }

    public Post getPostById(String id) {
        Path path = Paths.get(dataDir, id + ".json");
        if (Files.exists(path)) {
            try {
                String content = new String(Files.readAllBytes(path));
                Post post = objectMapper.readValue(content, Post.class);
                if (post.getCreatedAt() == null) {
                    post.setCreatedAt(getCreatedAtFromFile(path));
                }
                return post;
            } catch (IOException e) {
                logger.error("Failed to read post file {}", id, e);
            }
        }
        return null;
    }

    private LocalDateTime getCreatedAtFromFile(Path path) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault());
        } catch (IOException e) {
            return LocalDateTime.now();
        }
    }

    public boolean deletePost(String postId) {
        Path path = Paths.get(dataDir, postId + ".json");
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.error("Failed to delete post file: {}", postId, e);
            return false;
        }
    }

    public void savePost(Post post) {
        if (post.getId() == null) {
            String time = java.time.format.DateTimeFormatter
                    .ofPattern("yyyyMMddHHmmss")
                    .format(LocalDateTime.now());

            String random = UUID.randomUUID().toString().substring(0, 4);

            post.setId(time + "_" + random);
        }
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(LocalDateTime.now());
        }
        post.setUpdatedAt(LocalDateTime.now());
        if (post.getStatus() == null) {
            post.setStatus(Status.DRAFT);
        }
        try {
            String json = objectMapper.writeValueAsString(post);
            Path path = Paths.get(dataDir, post.getId() + ".json");
            Files.write(path, json.getBytes());
            logger.info("Saved post (status={}) with ID: {}", post.getStatus(), post.getId());
        } catch (IOException e) {
            logger.error("Failed to save post with ID: {}", post.getId(), e);
        }
    }

    public void schedulePost(Post post, LocalDateTime scheduledTime) {
        post.setScheduledTime(scheduledTime);
        savePost(post);
        logger.info("Scheduled post with ID: {} for {}", post.getId(), scheduledTime);
    }

    @Scheduled(fixedRateString = "#{${scheduler.post.interval-minutes} * 60000}")
    public void checkScheduledPosts() {
        logger.info("Checking for scheduled posts");
        List<Post> posts = getAllPosts();
        LocalDateTime now = LocalDateTime.now();
        for (Post post : posts) {
            if (Status.SCHEDULED.equals(post.getStatus()) && post.getScheduledTime() != null && post.getScheduledTime().isBefore(now) || post.getScheduledTime().isEqual(now)) {
                logger.info("Publishing scheduled post with ID: {}", post.getId());
                publishPost(post.getId());
            }
        }
    }

    public void publishPost(String postId) {
        logger.info("Attempting to publish post with ID: {}", postId);
        // Load post
        Post post = loadPost(postId);
        if (post == null) {
            logger.info("Post not found: {}", postId);
            return;
        }
        if (!(Status.DRAFT.equals(post.getStatus()) || Status.SCHEDULED.equals(post.getStatus()))) {
            logger.info("Post not in a publishable state (must be DRAFT or SCHEDULED): {} (status={})", postId, post.getStatus());
            return;
        }

        // Step 1: Create media container
        String containerId = threadsApiService.createMediaContainer(post.getMediaType(), post.getText(), post.getMediaUrl());
        if (containerId == null) {
            post.setStatus(Status.FAILED);
            savePost(post);
            logger.error("Failed to create media container for post: {}", postId);
            return;
        }

        // Step 1.5: Wait for media processing to finish
        if (!threadsApiService.waitUntilFinished(containerId)) {
            post.setStatus(Status.FAILED);
            savePost(post);
            logger.error("Media container did not finish processing: {}", containerId);
            return;
        }

        // Step 2: Publish
        String mediaId = threadsApiService.publishMediaContainer(containerId);
        if (mediaId != null) {
            post.setStatus(Status.PUBLISHED);
            post.setPublishedAt(java.time.LocalDateTime.now());
            post.setThreadsMediaId(mediaId);
            logger.info("Successfully published post with ID: {}, media ID: {}", postId, mediaId);
        } else {
            post.setStatus(Status.FAILED);
            logger.error("Failed to publish media container for post: {}", postId);
        }
        savePost(post);
    }

    private Post loadPost(String postId) {
        Path path = Paths.get(dataDir, postId + ".json");
        if (Files.exists(path)) {
            try {
                String content = new String(Files.readAllBytes(path));
                Post post = objectMapper.readValue(content, Post.class);
                logger.info("Loaded post with ID: {}", postId);
                return post;
            } catch (IOException e) {
                logger.error("Failed to load post with ID: {}", postId, e);
            }
        } else {
            logger.info("Post file not found: {}", postId);
        }
        return null;
    }
}