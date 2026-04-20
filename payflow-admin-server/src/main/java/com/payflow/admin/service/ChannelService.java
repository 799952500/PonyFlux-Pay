package com.payflow.admin.service;

import com.payflow.admin.entity.Channel;
import java.util.List;

public interface ChannelService {
    List<Channel> listAll();
    Channel getById(Long id);
    void create(Channel channel);
    void update(Long id, Channel channel);
    void delete(Long id);
}