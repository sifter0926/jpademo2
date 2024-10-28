package com.pgm.jpademo2.controller;

import com.pgm.jpademo2.dto.BoardDTO;
import com.pgm.jpademo2.dto.PageRequestDTO;
import com.pgm.jpademo2.dto.PageResponseDTO;
import com.pgm.jpademo2.dto.upload.UploadFileDTO;
import com.pgm.jpademo2.dto.upload.UploadResultDTO;
import com.pgm.jpademo2.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnailator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
@RequestMapping("/board")
@Log4j2
@RequiredArgsConstructor
public class BoardController {

    @Value("${com.pgm.upload.path}")
    private String uploadPath;

    private final BoardService boardService;

    @GetMapping("/list")
    public void list(PageRequestDTO pageRequestDTO, Model model){

        PageResponseDTO<BoardDTO> responseDTO = boardService.list(pageRequestDTO);
        log.info(responseDTO);
        model.addAttribute("responseDTO", responseDTO);
    }

    @GetMapping("/register")
    public void registerGET(){

    }

    @PostMapping("/register")
    public String registerPost(UploadFileDTO uploadFileDTO,
                               BoardDTO boardDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes){
        List<String> strFileNames=null;
        if(uploadFileDTO.getFiles()!=null &&
                !uploadFileDTO.getFiles().get(0).getOriginalFilename().equals("") ){
            strFileNames=fileUload(uploadFileDTO);
            log.info("!!!!!!!!!!!!!!!!"+strFileNames.size());
        }
        boardDTO.setFileNames(strFileNames);

        log.info("board POST register.......");
//        if(bindingResult.hasErrors()) {
//            log.info("has errors.......");
//            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors() );
//            return "redirect:/board/register";
//        }
        log.info(boardDTO);
        Long bno  = boardService.register(boardDTO);
        redirectAttributes.addFlashAttribute("result", bno);
        return "redirect:/board/list";
    }

    @GetMapping({"/read", "/modify"})
    public void read(Long bno, PageRequestDTO pageRequestDTO, Model model){
        BoardDTO boardDTO = boardService.readOne(bno);
        log.info(boardDTO);
        model.addAttribute("dto", boardDTO);
    }
    @PostMapping("/modify")
    public String modify(UploadFileDTO uploadFileDTO,
                         PageRequestDTO pageRequestDTO,
                          @Valid BoardDTO boardDTO,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes){

        log.info("board modify post......." + boardDTO);

        List<String> strFileNames=null;
        if(uploadFileDTO.getFiles()!=null &&
                !uploadFileDTO.getFiles().get(0).getOriginalFilename().equals("") ){

            List<String> fileNames = boardDTO.getFileNames();

            if(fileNames != null && fileNames.size() > 0){
                removeFile(fileNames);
            }

            strFileNames=fileUload(uploadFileDTO);
            log.info("!!!!!!!!!!!!!!!!"+strFileNames.size());
            boardDTO.setFileNames(strFileNames);
        }

        if(bindingResult.hasErrors()) {
            log.info("has errors.......");
            String link = pageRequestDTO.getLink();
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors() );
            redirectAttributes.addAttribute("bno", boardDTO.getBno());
            return "redirect:/board/modify?"+link;
        }

        boardService.modify(boardDTO);
        redirectAttributes.addFlashAttribute("result", "modified");
        redirectAttributes.addAttribute("bno", boardDTO.getBno());
        return "redirect:/board/read";
    }
    @PostMapping("/remove")
    public String remove(BoardDTO boardDTO, RedirectAttributes redirectAttributes) {
        log.info("remove post.. " + boardDTO);

        List<String> fileNames=boardDTO.getFileNames();
        if(fileNames !=null && fileNames.size()>0) {
            log.info("remove controller"+fileNames.size());
            removeFile(fileNames);
        }
        boardService.remove(boardDTO.getBno());

        redirectAttributes.addFlashAttribute("result", "removed");
        return "redirect:/board/list";
    }

    private List<String> fileUload(UploadFileDTO uploadFileDTO){

        List<String> list = new ArrayList<>();
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
                    Thumbnailator.createThumbnail(savePath.toFile(), thumbFile, 200,200);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            list.add(uuid+"_"+originalName);
        });
        return list;
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

    private void removeFile(List<String> fileNames){
        log.info("AAAAA"+fileNames.size());

        for(String fileName:fileNames){
            log.info("fileRemove method: "+fileName);
            Resource resource = new FileSystemResource(uploadPath+File.separator + fileName);
            String resourceName = resource.getFilename();

           // Map<String, Boolean> resultMap = new HashMap<>();
            boolean removed = false;

            try {
                String contentType = Files.probeContentType(resource.getFile().toPath());
                removed = resource.getFile().delete();

                //섬네일이 존재한다면
                if(contentType.startsWith("image")){
                    String fileName1=fileName.replace("s_","");
                    File originalFile = new File(uploadPath+File.separator + fileName1);
                    originalFile.delete();
                }

            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }
}
