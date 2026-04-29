package com.payflow.admin.service;

import com.payflow.admin.entity.ChannelRoute;

import java.util.List;
import java.util.Map;
/**
 * @author Lucas
 */

public interface ChannelRouteService {
    List<Map<String, Object>> listWithDetails();

    List<Map<String, String>> listAllMerchantsSimple();

    ChannelRoute create(ChannelRoute route);

    void toggle(Long id);

    void delete(Long id);
}