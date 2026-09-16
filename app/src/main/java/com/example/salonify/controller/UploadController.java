package com.example.salonify.controller;

import com.example.salonify.entity.User;
import com.example.salonify.support.Auth;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@RestController
public class UploadController {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_SIZE = 4L * 1024 * 1024; // 4MB

    private final Auth auth;

    @Value("${salonify.upload-dir}")
    private String uploadDir;

    public UploadController(Auth auth) {
        this.auth = auth;
    }

    @PostMapping("/api/upload")
    public ResponseEntity<?> upload(@RequestParam(value = "file", required = false) MultipartFile file,
                                    HttpServletRequest request) throws Exception {
        User me = auth.currentUser(request);
        if (me == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "no file"));
        if (!ALLOWED_TYPES.contains(file.getContentType()))
            return ResponseEntity.badRequest().body(Map.of("error", "invalid file type"));
        if (file.getSize() > MAX_SIZE)
            return ResponseEntity.badRequest().body(Map.of("error", "file too large (max 4MB)"));

        String original = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String safeName = original.replaceAll("[^A-Za-z0-9._-]", "_");
        String rel = me.getId() + "/" + System.currentTimeMillis() + "-" + safeName;
        Path dest = Path.of(uploadDir, rel);
        Files.createDirectories(dest.getParent());
        file.transferTo(dest);

        return ResponseEntity.ok(Map.of("url", "/uploads/" + rel));
    }
}
