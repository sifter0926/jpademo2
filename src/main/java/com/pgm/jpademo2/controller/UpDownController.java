package com.pgm.jpademo2.controller;

import com.pgm.jpademo2.dto.BoardDTO;
import com.pgm.jpademo2.dto.upload.UploadFileDTO;
import com.pgm.jpademo2.dto.upload.UploadResultDTO;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnailator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@Log4j2
@RequestMapping("/upload")
public class UpDownController {

    @Value("${com.pgm.jpademo2.upload.path}")
    private String uploadPath;

    @GetMapping("/uploadForm")
    public void uploadForm(){
    }
    @PostMapping("/uploadPro")
    public void uploadPro(UploadFileDTO uploadFileDTO, BoardDTO boardDTO, Model model){
        //log.info(uploadFileDTO);
        log.info(boardDTO);
        if(uploadFileDTO.getFiles() !=null){
            final List<UploadResultDTO> list = new ArrayList<>();
            uploadFileDTO.getFiles().forEach(multipartFile -> {
                String originalName = multipartFile.getOriginalFilename();
                log.info(originalName);

                String uuid = UUID.randomUUID().toString();
                Path savePath = Paths.get(uploadPath, uuid+"_"+ originalName);
                boolean image = false;
                try {
                    multipartFile.transferTo(savePath); // 서버에 파일저장
                    //이미지 파일의 종류라면
                    if(Files.probeContentType(savePath).startsWith("image")){
                        image = true;
                        File thumbFile = new File(uploadPath, "s_" + uuid+"_"+ originalName);
                        Thumbnailator.createThumbnail(savePath.toFile(), thumbFile, 100,100);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                list.add(UploadResultDTO.builder()
                        .uuid(uuid)
                        .fileName(originalName)
                        .image(image)
                        .build()
                );
                model.addAttribute("list", list);
                model.addAttribute("uploadPath", uploadPath);
            });
        }
    }
    @GetMapping("/view/{fileName}")
    @ResponseBody
    public ResponseEntity<Resource> viewFileGet(@PathVariable("fileName") String fileName){
        Resource resource = new FileSystemResource(uploadPath+File.separator + fileName);
        String resourceName = resource.getFilename();
        HttpHeaders headers = new HttpHeaders();

        try{
            headers.add("Content-Type", Files.probeContentType( resource.getFile().toPath() ));
        } catch(Exception e){
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

}
