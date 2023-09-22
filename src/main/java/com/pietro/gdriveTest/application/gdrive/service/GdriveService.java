package com.pietro.gdriveTest.application.gdrive.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.model.FileList;
import lombok.Data;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Data
@Service
public class GdriveService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    @Value("${gdrive.environment.application-name}")
    private String APPLICATION_NAME;
    @Value("${gdrive.environment.credentials}")
    private String CREDENTIALS;
    @Value("${gdrive.environment.account}")
    private String GOOGLE_ACCOUNT;
    private static final Logger LOGGER = LoggerFactory.getLogger(GdriveService.class);

    private GoogleCredentials getGoogleCredentials() throws Exception {
        val inputStream = GdriveService.class.getClassLoader().getResourceAsStream(CREDENTIALS);
        val credenciais = GoogleCredentials.fromStream(inputStream).createScoped(Collections.singleton(DriveScopes.DRIVE)).createDelegated(GOOGLE_ACCOUNT);
        inputStream.close();
        return credenciais;
    }

    private Drive getDrive() throws Exception {
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        val requestInitializer = new HttpCredentialsAdapter(getGoogleCredentials());
        return new Drive.Builder(httpTransport, JSON_FACTORY, requestInitializer).setApplicationName(APPLICATION_NAME).build();
    }






    public List<File> searchFile(String text) throws Exception {
        List<File> files = new ArrayList<File>();
        String pageToken = null;
        do {
            FileList result = getDrive().files().list()
                    .setQ("mimeType='application/pdf' and fullText contains '" + text +"'" ) // PARÂMETROS
                    .setSpaces("drive")
                    .setFields("files(mimeType,id,name, modifiedTime,thumbnailLink,webViewLink,parents, size)") // RETORNO
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                System.out.printf("Found file: %s (%s)\n",
                        file.getName(), file.getId());
            }
            files.addAll(result.getFiles());
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        return files;
    }







    public String upload(InputStream arquivoInputStream, String filename, String contentType, String parentId) throws Exception {
        val content = new InputStreamContent(contentType, arquivoInputStream);
        val fileMetadata = new File();
        fileMetadata.setName(filename);
        fileMetadata.setParents(Collections.singletonList(parentId));
        val file = getDrive().files().create(fileMetadata, content).setFields("id,parents").execute();
        LOGGER.info("File uploaded: " + filename + " " + file.getId());
        return file.getId();
    }

    public ResponseEntity<Void> uploadFile(MultipartFile arquivo, String parentId) throws Exception {
//        val content = new InputStreamContent(contentType, arquivoInputStream);
        val fileMetadata = new File();
        System.out.println(arquivo.getOriginalFilename());
        fileMetadata.setName(arquivo.getOriginalFilename());
        fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
        fileMetadata.setParents(Collections.singletonList(parentId));
        val file = getDrive().files().create(fileMetadata).setFields("id").execute();
        LOGGER.info("File uploaded: " + arquivo.getOriginalFilename() + " " + file.getId());
        return ResponseEntity.ok().build();
//        return file.getId();
    }

    public byte[] download(String params) throws Exception {
        val outputStream = new ByteArrayOutputStream();
        val arquivo = getDrive().files().list().setQ(params).execute().getFiles().get(0);
        getDrive().files().get(arquivo.getId()).executeMediaAndDownloadTo(outputStream);
        byte[] stream = outputStream.toByteArray();
        outputStream.close();
        return stream;
    }

    public byte[] downloadById(String arquivoId) throws Exception {
        val outputStream = new ByteArrayOutputStream();
        getDrive().files().get(arquivoId).executeMediaAndDownloadTo(outputStream);
        byte[] stream = outputStream.toByteArray();
        outputStream.close();
        return stream;
    }


    public List<File> getFiles(String folderId) throws Exception {
        val params = "'" + folderId + "' in parents";
        val fileList = getDrive().files().list().setQ(params)
                .setFields("files(mimeType,id,name, modifiedTime,thumbnailLink,webViewLink,parents, size)")
                .execute();
        return fileList.getFiles();
    }

    public File getFileById(String fileId) throws Exception {
        return getDrive().files().get(fileId)
                .setFields("files(mimeType,id,name,thumbnailLink,webViewLink)")
                .execute();
    }

    public File copyFile(String fileId, String oldParentId, String newParentId) throws Exception {
        val file = getDrive().files().copy(fileId, null).execute();
        moveFile(fileId, oldParentId, newParentId);
        return file;
    }


    public String findFolderId(String foldername, String parentId) throws Exception {
        val fileList = getDrive().files().list()
                .setQ("mimeType='application/vnd.google-apps.folder' and name='" + foldername + "' and '"
                        + parentId + "' in parents")
                .setFields("files(mimeType,id,name,thumbnailLink,webViewLink)").execute();
        val files = fileList.getFiles();
        if (!files.isEmpty()) {
            return files.iterator().next().getId();
        } else {
            return null;
        }
    }

    public String createFolder(String foldername, String parentId) throws Exception {
        File fileMetadata = new File();
        fileMetadata.setName(foldername);
        fileMetadata.setMimeType("application/vnd.google-apps.folder");
        if (parentId != null) {
            List<String> parents = Arrays.asList(parentId);
            fileMetadata.setParents(parents);
        }
//        fileMetadata.setParents(Collections.singletonList(parentId));

        val file = getDrive().files().create(fileMetadata).setFields("id, parents").execute();
//                create(fileMetadata).setFields("id, name").execute();
        LOGGER.info("Folder created: " + foldername + " " + file.getId() + " in folder " + file.getParents());
        return file.getId();
    }

    public boolean deleteFile(String fileId) {
        try {
            getDrive().files().delete(fileId).execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean moveFile(String fileId, String oldParentId, String newParentId) {
        try {
            val file = getDrive().files().update(fileId, null).setAddParents(newParentId)
                    .setRemoveParents(oldParentId).setFields("id, parents").execute();

            LOGGER.info("File moved. FileId: " + file.getId() + " oldParentId: " + oldParentId
                    + " guessedParentId: " + newParentId + " newParentId: "
                    + file.getParents().iterator().next());

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String downloadFile(String fileName, String path, String parentFolderId) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FileList arquivos;
        String[] pastas = path.substring(1).split("/");
        String nextParent = parentFolderId;
        var interator = 0;

        for (String pasta : pastas) {
            interator += 1;
            System.out.println("PASTA " + interator + ": " + pasta);
            arquivos = getDrive().files().list()
                    .setQ("mimeType='application/vnd.google-apps.folder' and name='" + pasta + "' and '" + nextParent + "' in parents")
                    .setFields("files(mimeType,id,name, modifiedTime,thumbnailLink,webViewLink,parents, size)").execute();
            List<File> files = arquivos.getFiles();
            System.out.println("Quantidade de Arquivos na pasta: " + files.size());
            if (files.size() != 0) {
                nextParent = files.iterator().next().getId();
            }
        }

        File arquivo = getDrive().files().list().setQ("name='" + fileName + "'")
                .setFields("files(mimeType,id,name, modifiedTime,thumbnailLink,webViewLink,parents, size)")
                .execute().getFiles().get(0);
        System.out.println(arquivo.getDriveId());
        System.out.println(arquivo.getWebContentLink());
        System.out.println(arquivo.getWebViewLink());
        System.out.println(arquivo.getExportLinks());

        return arquivo.getWebViewLink();
    }

    /**
     * Método que retorna um array de bytes (o conteúdo do arquivo em si)
     * através do ID do arquivo armazenado no Drive.
     *
     * @param arquivoId - id do arquivo
     * @return byte[] - conteúdo do arquivo
     * @throws Exception
     */
    public byte[] downloadFileById(String arquivoId) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        getDrive().files().get(arquivoId).executeMediaAndDownloadTo(outputStream);
        return outputStream.toByteArray();
    }
}


