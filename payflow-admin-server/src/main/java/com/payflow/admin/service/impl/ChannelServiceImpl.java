package com.payflow.admin.service.impl;

import com.payflow.admin.entity.Channel;
import com.payflow.admin.mapper.ChannelMapper;
import com.payflow.admin.service.ChannelService;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
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
    private final ResourceDeleteGuardService resourceDeleteGuardService;

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
        resourceDeleteGuardService.assertDeletable(ResourceType.CHANNEL, id);
        channelMapper.deleteById(id);
    }
}