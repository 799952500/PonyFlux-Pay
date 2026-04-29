package com.payflow.admin.service.impl;

import com.payflow.admin.entity.Channel;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class ChannelServiceImpl implements ChannelService {

    private final ChannelMapper channelMapper;

    @Override
    public List<Channel> listAll() {
        return channelMapper.selectList(null);
    }

    @Override
    public Channel getById(Long id) {
        return channelMapper.selectById(id);
    }

    @Override
    public void create(Channel channel) {
        channelMapper.insert(channel);
    }

    @Override
    public void update(Long id, Channel channel) {
        channel.setId(id);
        channelMapper.updateById(channel);
    }

    @Override
    public void delete(Long id) {
        channelMapper.deleteById(id);
    }
}