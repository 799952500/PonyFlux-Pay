package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.ChannelRoute;
import com.payflow.admin.mapper.ChannelRouteMapper;
import com.payflow.admin.service.ChannelRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class ChannelRouteServiceImpl implements ChannelRouteService {

    private final ChannelRouteMapper mapper;

    @Override
    public List<Map<String, Object>> listWithDetails() {
        return mapper.listWithDetails();
    }

    @Override
    public List<Map<String, String>> listAllMerchantsSimple() {
        return mapper.listAllMerchantsSimple();
    }

    @Override
    public ChannelRoute create(ChannelRoute route) {
        mapper.insert(route);
        return route;
    }

    @Override
    public void toggle(Long id) {
        ChannelRoute route = mapper.selectById(id);
        if (route == null) {
            throw new IllegalStateException("路由记录不存在");
        }
        route.setEnabled(route.getEnabled() == null || !route.getEnabled());
        mapper.updateById(route);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }
}