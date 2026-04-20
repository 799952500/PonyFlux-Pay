package com.payflow.admin.service;

import com.payflow.admin.entity.Merchant;
import java.util.List;

public interface MerchantService {
    List<Merchant> listAll();
    Merchant getById(Long id);
    Merchant getByMerchantId(String merchantId);
    void create(Merchant merchant);
    void update(Long id, Merchant merchant);
    void delete(Long id);
}