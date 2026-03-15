package com.example.autopost.controller;

import com.example.autopost.model.PageResponse;
import com.example.autopost.model.Post;
import com.example.autopost.model.Status;
import com.example.autopost.service.PostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class HomeController {

    private final PostService postService;

    public HomeController(PostService postService) {
        this.postService = postService;
    }

    @Value("${posts.page-size}")
    private int pageSize;

    @GetMapping("/")
    public String listPosts(@RequestParam(defaultValue = "0") int page, Model model) {

        PageResponse<Post> postPage = postService.getPosts(page, pageSize);

        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postPage.getTotalPages());

        return "index";
    }

    @PostMapping("/publish/{id}")
    public String publishPost(@PathVariable String id) {
        postService.publishPost(id);
        return "redirect:/";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable String id) {
        postService.deletePost(id);
        return "redirect:/";
    }

    @PostMapping("/create")
    public String createPost(@RequestParam String text,
                             @RequestParam String mediaUrl,
                             @RequestParam String mediaType,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime) {
        Post post = new Post(text, mediaUrl, mediaType);
        postService.schedulePost(post, scheduledTime);
        return "redirect:/";
    }

    @GetMapping("/posts/{id}")
    public String detailPost(@PathVariable String id, Model model) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return "redirect:/";
        }
        model.addAttribute("post", post);
        return "detail";
    }

    @PostMapping("/posts/{id}/update")
    public String updatePost(@PathVariable String id,
                             @RequestParam String text,
                             @RequestParam String mediaUrl,
                             @RequestParam String mediaType,
                             @RequestParam String status,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime) {
        Post post = postService.getPostById(id);
        if (post == null) {
            return "redirect:/";
        }
        post.setText(text);
        post.setMediaUrl(mediaUrl);
        post.setMediaType(mediaType);
        post.setStatus(Status.valueOf(status));
        post.setScheduledTime(scheduledTime);
        post.setUpdatedAt(LocalDateTime.now());

        if (post.getStatus().equals(Status.DRAFT) || post.getStatus().equals(Status.SCHEDULED)) {
            post.setPublishedAt(null);
        }

        postService.savePost(post);
        return "redirect:/";
    }
}
