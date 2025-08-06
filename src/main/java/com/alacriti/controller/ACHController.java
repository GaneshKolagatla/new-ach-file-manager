package com.alacriti.controller;

import com.alacriti.dto.ACHFileRequest;
import com.alacriti.service.ACHService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ach")
public class ACHController {

    @Autowired
    private ACHService achService;

    @PostMapping("/generate-ach")
    public ResponseEntity<String> generateAchFile(@RequestBody ACHFileRequest request) {
        try {
            String encryptedFileName = achService.generateAndEncryptAndSendACHFile(request);
            return ResponseEntity.ok("ACH file encrypted and uploaded successfully: " + encryptedFileName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error while processing ACH file: " + e.getMessage());
        }
    }
}
 