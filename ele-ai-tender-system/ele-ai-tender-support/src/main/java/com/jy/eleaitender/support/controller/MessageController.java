package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 消息中心控制器
 */
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "消息中心")
public class MessageController {

    @Autowired
    private IMessageService messageService;

    @GetMapping
    @Operation(summary = "分页查询我的消息")
    @RequireLogin
    public Result<Page<SupMessage>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Integer isRead) {
        return Result.success(messageService.getMyMessages(pageNum, pageSize, messageType, isRead));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量")
    @RequireLogin
    public Result<Long> unreadCount() {
        return Result.success(messageService.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记消息已读")
    @RequireLogin
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    @RequireLogin
    public Result<Void> markAllRead() {
        messageService.markAllRead();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        messageService.deleteById(id);
        return Result.success();
    }
}
