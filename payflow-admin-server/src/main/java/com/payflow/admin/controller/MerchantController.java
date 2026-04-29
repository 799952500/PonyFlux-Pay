package com.payflow.admin.controller;

import com.payflow.admin.entity.Merchant;
import com.payflow.admin.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户管理 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 查询所有商户列表
     *
     * @return 商户列表
     */
    @GetMapping
    public List<Merchant> listAll() {
        return merchantService.listAll();
    }

    /**
     * 根据ID查询商户详情
     *
     * @param id 商户ID
     * @return 商户信息
     */
    @GetMapping("/{id}")
    public Merchant getById(@PathVariable Long id) {
        return merchantService.getById(id);
    }

    /**
     * 根据商户号查询商户
     *
     * @param merchantId 商户号
     * @return 商户信息
     */
    @GetMapping("/code/{merchantId}")
    public Merchant getByMerchantId(@PathVariable String merchantId) {
        return merchantService.getByMerchantId(merchantId);
    }

    /**
     * 创建商户
     *
     * @param merchant 商户信息
     * @return 无
     */
    @PostMapping
    public ResponseEntity<Void> create(@RequestBody Merchant merchant) {
        merchantService.create(merchant);
        return ResponseEntity.ok().build();
    }

    /**
     * 更新商户信息
     *
     * @param id      商户ID
     * @param merchant 商户信息
     * @return 无
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Merchant merchant) {
        merchantService.update(id, merchant);
        return ResponseEntity.ok().build();
    }

    /**
     * 删除商户
     *
     * @param id 商户ID
     * @return 无
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id == null) {
            throw new IllegalArgumentException("商户ID不能为空");
        }
        merchantService.delete(id);
        return ResponseEntity.ok().build();
    }
}