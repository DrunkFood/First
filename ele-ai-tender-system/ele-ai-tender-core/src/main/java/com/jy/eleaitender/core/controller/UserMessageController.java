package com.jy.eleaitender.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.service.IUserMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 消息中心控制器（编制中心）
 */
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "消息中心")
public class UserMessageController {

    @Autowired
    private IUserMessageService userMessageService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询消息")
    public Result<Page<SupMessage>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Integer isRead) {
        return Result.success(userMessageService.getMyMessages(pageNum, pageSize, messageType, isRead));
    }

    @PutMapping("/{id}/read")
    @RequireLogin
    @Operation(summary = "标记已读")
    public Result<Void> markRead(@PathVariable Long id) {
        userMessageService.markRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    @RequireLogin
    @Operation(summary = "全部标记已读")
    public Result<Void> markAllRead() {
        userMessageService.markAllRead();
        return Result.success();
    }

    @GetMapping("/unread-count")
    @RequireLogin
    @Operation(summary = "未读消息数")
    public Result<Long> unreadCount() {
        return Result.success(userMessageService.getUnreadCount());
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除消息")
    public Result<Void> deleteById(@PathVariable Long id) {
        userMessageService.deleteById(id);
        return Result.success();
    }
}
