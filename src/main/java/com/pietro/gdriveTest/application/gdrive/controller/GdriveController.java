package com.pietro.gdriveTest.application.gdrive.controller;

import com.pietro.gdriveTest.application.gdrive.gateway.GdriveGateway;
import com.pietro.gdriveTest.domain.gdrive.usecase.GdriveUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class GdriveController {

    private final GdriveGateway gdriveGateway;

    @GetMapping("/upload")
    public ResponseEntity<String> upload () {
        new GdriveUseCase(gdriveGateway).execute();
        return ResponseEntity.ok("Upload com sucesso!");
    }

}

