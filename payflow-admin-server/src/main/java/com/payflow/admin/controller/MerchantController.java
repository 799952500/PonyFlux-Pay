package com.payflow.admin.controller;

import com.payflow.admin.entity.Merchant;
import com.payflow.admin.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public List<Merchant> listAll() {
        return merchantService.listAll();
    }

    @GetMapping("/{id}")
    public Merchant getById(@PathVariable Long id) {
        return merchantService.getById(id);
    }

    @GetMapping("/code/{merchantId}")
    public Merchant getByMerchantId(@PathVariable String merchantId) {
        return merchantService.getByMerchantId(merchantId);
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody Merchant merchant) {
        merchantService.create(merchant);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Merchant merchant) {
        merchantService.update(id, merchant);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        merchantService.delete(id);
        return ResponseEntity.ok().build();
    }
}