package cn.tangtj.clouddisk.controller;

import cn.tangtj.clouddisk.entity.UploadFile;
import cn.tangtj.clouddisk.entity.User;
import cn.tangtj.clouddisk.entity.vo.Files;
import cn.tangtj.clouddisk.service.FileService;
import cn.tangtj.clouddisk.service.UserService;
import cn.tangtj.clouddisk.utils.FileUtil;
import cn.tangtj.clouddisk.utils.StringUtil;
import cn.tangtj.clouddisk.utils.UserUtil;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author tang
 */
@Controller
@RequestMapping("/file")
public class FileController {

    private final String fileSavePath;

    private final static String fileSaveDir = "upload";

    private final UserService userService;

    private final FileService fileService;
    /*初始化文件保存路径
    ServletContext 获取应用程序的真实路径，并将文件保存路径设置为该路径下的 fileSaveDir 目录。
    然后，它检查该目录是否存在，如果不存在，则创建该目录。*/

    @Autowired
    public FileController(UserService userService, FileService fileService, ServletContext servletContext) {
        this.userService = userService;
        this.fileService = fileService;
        fileSavePath = servletContext.getRealPath("") + fileSaveDir + File.separator;
        System.out.println("fileSavePath->>>>"+fileSavePath);
        File file = new File(fileSavePath);
        if (!file.exists()) {
            file.mkdir();
        }
    }

    @RequestMapping()
    public String index(Model model) {
        User user = UserUtil.getPrincipal();
        if (user == null) {
            return "redirect:/login";
        }
        List<UploadFile> list = fileService.findByUserId(user.getId());
        Files files = new Files(list);
        model.addAttribute("user", user);
        model.addAttribute("files", files);
        return "files";
    }

    /**
     * @ResponseBody 注解：这个注解告诉 Spring MVC 这个方法的返回值应该直接作为响应的内容，而不是视图名称。在这种情况下，返回的是一个字符串。
     *
     * public String fileUpload(MultipartFile file) 方法：这是处理文件上传的控制器方法。它接收一个 MultipartFile 参数，该参数用于接收上传的文件。
     *
     * 获取当前登录用户信息：通过 UserUtil.getPrincipal() 方法获取当前登录用户的信息（User 对象）。如果用户未登录或文件为空，则无法继续处理。
     *
     * 创建 UploadFile 对象：创建一个 UploadFile 对象，用于表示上传的文件信息。将文件名、用户ID、文件大小和上传日期等信息设置到 UploadFile 对象中。
     *
     * 生成文件的唯一映射名：使用一些文件属性（文件名、用户ID、上传日期等）来生成唯一的映射名，通常是文件的 MD5 哈希值。
     *
     * 检查用户文件上传限制：检查用户的上传文件大小和数量是否超出了限制。如果超出了限制，返回 "upload.fail" 表示上传失败。
     *
     * 设置映射名：将生成的唯一映射名设置到 UploadFile 对象中，以便后续保存到数据库和文件系统中。
     *
     * 保存文件到本地：使用 file.transferTo(localFile) 将上传的文件保存到本地文件系统，其中 localFile 是指定的文件保存路径。
     *
     * 保存文件信息到数据库：使用 fileService.save(f) 将文件信息保存到数据库中，以便后续检索和管理。
     *
     * 返回响应结果：如果文件上传成功，返回 "upload.success" 表示上传成功；如果发生异常或文件为空，返回 "upload.fail" 表示上传失败。
     * @param file
     * @return
     */
    @RequestMapping(value = "/upload")
    @ResponseBody
    public String fileUpload(MultipartFile file) {
        User user = UserUtil.getPrincipal();

        if (user != null && file != null && !file.isEmpty()) {
            UploadFile f = new UploadFile();
            f.setFileName(file.getOriginalFilename());
            f.setUserId(user.getId());
            f.setFileSize(file.getSize());
            f.setUploadDate(new Date());
            String md5Name = StringUtil.str2md5(f.getFileName() + "," + f.getUserId() + "," + f.getUploadDate().toString());

            Files files = new Files(fileService.findByUserId(user.getId()));

            if(files.getFilesSize() + f.getFileSize() > user.getFileMaxSize()){
                return "upload.fail";
            }
            if (files.getFilesCount() + 1 > user.getFileMaxCount()){
                return "upload,fail";
            }

            if (md5Name == null) {
                return "upload,fail";
            }
            f.setMappingName(md5Name);
            File localFile = new File(fileSavePath + f.getMappingName());
            try {
                file.transferTo(localFile);
                fileService.save(f);
                return "upload,success";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "upload,fail";
    }

    @RequestMapping("/download/{fileId}")
    public ResponseEntity<byte[]> fileDownload(@PathVariable("fileId") int fileId) {
        UploadFile fileInfo = fileService.findById(fileId);
        return FileUtil.createResponseEntityByFileInfo(fileInfo, fileSavePath);
    }

    @RequestMapping(value = "/shareFile", produces = "text/json; charset=utf-8")
    @ResponseBody
    public String shareFile(String fileIdStr) {
        if (fileIdStr == null) {
            return "分享失败";
        }
        int fileId = Integer.parseInt(fileIdStr);
        UploadFile fileInfo = fileService.findById(fileId);
        if (fileInfo != null) {
            if (fileService.shareFileById(fileInfo.getId()) != null) {
                return "分享成功";
            }
        }
        return "分享失败";
    }

    @RequestMapping(value = "/unShareFile", produces = "text/json; charset=utf-8")
    @ResponseBody
    public String unShareFile(String fileIdStr) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (fileIdStr == null) {
            return "取消失败";
        }
        int fileId = Integer.parseInt(fileIdStr);
        List<UploadFile> list = fileService.findByUserId(user.getId());
        if (list.stream().anyMatch(it->it.getId() == fileId)) {
            fileService.unshareFile(fileId);
            return "取消成功";
        }
        return "取消失败";
    }

    @RequestMapping("/delete/{fileId}")
    public String fileDelete(@PathVariable("fileId") int fileId) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        List<UploadFile> list = fileService.findByUserId(user.getId());
        list.stream().filter(it->it.getId()==fileId).findFirst().ifPresent(it->{
            fileService.deleteById(fileId);
            File file = new File(fileSavePath + it.getMappingName());
            if (file.exists()){
                file.delete();
            }
        });
        return "redirect:/file";
    }


    @RequestMapping("/cancel")
    public String logout(){
        Subject subject = SecurityUtils.getSubject();
        subject.logout();
        return "redirect:/";
    }
}
