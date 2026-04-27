package com.payflow.admin.service;

import com.payflow.admin.entity.Merchant;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;

public interface MerchantService {
    List<Merchant> listAll();
    IPage<Merchant> page(int page, int pageSize, String keyword, String status);
    Merchant getById(Long id);
    Merchant getByMerchantId(String merchantId);
    void create(Merchant merchant);
    void update(Long id, Merchant merchant);
    void delete(Long id);
}