package com.payflow.admin.controller;

import com.payflow.admin.entity.Channel;
import com.payflow.admin.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @GetMapping
    public List<Channel> listAll() {
        return channelService.listAll();
    }

    @GetMapping("/{id}")
    public Channel getById(@PathVariable Long id) {
        return channelService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody Channel channel) {
        channelService.create(channel);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Channel channel) {
        channelService.update(id, channel);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return ResponseEntity.ok().build();
    }
}