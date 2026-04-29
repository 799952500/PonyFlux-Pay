package com.payflow.admin.controller;

import com.payflow.admin.entity.Channel;
import com.payflow.admin.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 渠道管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/channels")
@RequiredArgsConstructor
public class AdminChannelController {

    private final ChannelService channelService;

    /**
     * 查询所有渠道列表
     *
     * @return 渠道列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listChannels() {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                channelService.listAll()
        ));
    }

    /**
     * 根据ID查询渠道详情
     *
     * @param id 渠道ID
     * @return 渠道详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getChannel(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                channelService.getById(id)
        ));
    }

    /**
     * 创建渠道
     *
     * @param channel 渠道信息
     * @return 创建的渠道
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createChannel(@RequestBody Channel channel) {
        channelService.create(channel);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", channel));
    }

    /**
     * 更新渠道信息
     *
     * @param id      渠道ID
     * @param channel 更新后的渠道信息
     * @return 更新后的渠道
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateChannel(@PathVariable Long id, @RequestBody Channel channel) {
        channelService.update(id, channel);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", channel));
    }

    /**
     * 删除渠道
     *
     * @param id 渠道ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteChannel(@PathVariable Long id) {
        channelService.delete(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", true));
    }

    /**
     * 启用/禁用渠道
     *
     * @param id 渠道ID
     * @return 操作后的渠道
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleChannel(@PathVariable Long id) {
        Channel channel = channelService.getById(id);
        if (channel == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "渠道不存在", "data", Map.of()));
        }
        channel.setEnabled(channel.getEnabled() == null || !channel.getEnabled());
        channelService.update(id, channel);
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                channel
        ));
    }
}
