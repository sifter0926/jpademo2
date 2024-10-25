package com.pgm.jpademo2.controller;

import com.pgm.jpademo2.dto.BoardDTO;
import com.pgm.jpademo2.dto.upload.UploadFileDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
            uploadFileDTO.getFiles().forEach(file -> {
                String originalName = file.getOriginalFilename();
                log.info(originalName);
            });
        }

    }

}
