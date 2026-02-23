package com.yupi.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.FileConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.CosManager;
import com.yupi.springbootinit.model.dto.file.UploadFileRequest;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.FileUploadBizEnum;
import com.yupi.springbootinit.service.UserService;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口
 *
 * @author <a href="https://github.com/kkkano">kkkano</a>
 * @from <a href=“https://github.com/kkkano/BI”</a>
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    private static final long ONE_M = 1024 * 1024L;

    private static final List<String> USER_AVATAR_ALLOWED_SUFFIXES =
            Arrays.asList("jpeg", "jpg", "svg", "png", "webp");

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    /**
     * 文件上传
     *
     * @param multipartFile
     * @param uploadFileRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
            @Valid UploadFileRequest uploadFileRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(uploadFileRequest == null, ErrorCode.PARAMS_ERROR, "上传参数不能为空");
        String biz = uploadFileRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        if (fileUploadBizEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "不支持的上传业务类型: " + biz + "，可选值: " + FileUploadBizEnum.getValues());
        }
        validFile(multipartFile, fileUploadBizEnum);
        User loginUser = userService.getLoginUser(request);
        // 文件目录：根据业务、用户来划分
        String uuid = RandomStringUtils.randomAlphanumeric(8);
        String originalFilename = FileUtil.getName(multipartFile.getOriginalFilename());
        String filename = uuid + "-" + originalFilename;
        String filepath = String.format("/%s/%s/%s", fileUploadBizEnum.getValue(), loginUser.getId(), filename);
        File tempFile = null;
        try {
            // 上传文件
            tempFile = File.createTempFile("upload-", ".tmp");
            multipartFile.transferTo(tempFile);
            cosManager.putObject(filepath, tempFile);
            // 返回可访问地址
            return ResultUtils.success(FileConstant.COS_HOST + filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null) {
                // 删除临时文件
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 文件大小
        long fileSize = multipartFile.getSize();
        // 文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        ThrowUtils.throwIf(originalFilename == null, ErrorCode.PARAMS_ERROR, "文件名不能为空");
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        fileSuffix = fileSuffix == null ? "" : fileSuffix.toLowerCase(Locale.ROOT);
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            if (fileSize > ONE_M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 1M");
            }
            if (!USER_AVATAR_ALLOWED_SUFFIXES.contains(fileSuffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "文件类型错误，仅支持: " + USER_AVATAR_ALLOWED_SUFFIXES);
            }
        }
    }
}
