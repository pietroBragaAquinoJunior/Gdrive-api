package com.pietro.gdriveTest.application.gdrive.gateway;

import com.pietro.gdriveTest.application.gdrive.service.GdriveService;
import com.pietro.gdriveTest.domain.gdrive.usecase.GdriveUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class GdriveGateway implements GdriveUseCase.Output {

    final GdriveService gdriveService;

    @Override
    public void uploadArquivo() {
        try {
            gdriveService.searchFile("nizar");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

