package com.jy.eleaitender.core.controller.external;

import com.jy.eleaitender.common.interaction.dto.InteractionPolicyFileVO;
import com.jy.eleaitender.common.interaction.dto.PolicyFileQueryResponse;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.response.PolicyFileVO;
import com.jy.eleaitender.core.service.IPolicyFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 外部政策文件API控制器
 * 供外部系统通过签名认证调用
 */
@RestController
@RequestMapping(value = "/api/external/policy-file", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "外部政策文件API")
public class ExternalPolicyFileController {

    @Autowired
    private IPolicyFileService policyFileService;

    @GetMapping("/all")
    @RequireLogin
    @Operation(summary = "获取全部可用政策文件（系统级+用户级合并）")
    public Result<PolicyFileQueryResponse> getPolicyFile() {
        List<PolicyFileVO> voList = policyFileService.getAllAvailable(null);

        List<InteractionPolicyFileVO> policyFileList = new ArrayList<>();
        voList.forEach(vo -> {
            InteractionPolicyFileVO policyFile = new InteractionPolicyFileVO();
            policyFile.setId(vo.getId());
            policyFile.setFileCategory(vo.getFileCategory());
            policyFile.setApplicableCategory(vo.getApplicableCategory());
            policyFile.setFileId(vo.getFileId());
            policyFile.setFileName(vo.getFileName());
            policyFile.setFileSize(vo.getFileSize());
            policyFile.setFileType(vo.getFileType());
            policyFile.setDescription(vo.getDescription());
            policyFile.setSource(vo.getSource());
            policyFileList.add(policyFile);
        });

        PolicyFileQueryResponse response = new PolicyFileQueryResponse();
        response.setPolicyFileList(policyFileList);
        return Result.success(response);
    }

}
