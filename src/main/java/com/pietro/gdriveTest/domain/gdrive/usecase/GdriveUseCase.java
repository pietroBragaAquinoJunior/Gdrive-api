package com.pietro.gdriveTest.domain.gdrive.usecase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GdriveUseCase {

    private final Output output;

    public void execute() {

        output.uploadArquivo();

    }

    public interface Output {
        void uploadArquivo();
    }
}

