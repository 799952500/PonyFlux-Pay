package com.payflow.admin.controller;

import com.payflow.admin.entity.MerchantPaymentMethod;
import com.payflow.admin.service.MerchantPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/merchant-payment-methods")
@RequiredArgsConstructor
public class MerchantPaymentMethodController {

    private final MerchantPaymentMethodService service;

    @GetMapping("/merchant/{merchantId}")
    public List<MerchantPaymentMethod> listByMerchantId(@PathVariable Long merchantId) {
        return service.listByMerchantId(merchantId);
    }

    @GetMapping("/method/{paymentMethodId}")
    public List<MerchantPaymentMethod> listByPaymentMethodId(@PathVariable Long paymentMethodId) {
        return service.listByPaymentMethodId(paymentMethodId);
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody MerchantPaymentMethod mpm) {
        service.create(mpm);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody MerchantPaymentMethod mpm) {
        service.update(id, mpm);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/merchant/{merchantId}/method/{paymentMethodId}")
    public ResponseEntity<Void> deleteByMerchantAndMethod(
            @PathVariable Long merchantId,
            @PathVariable Long paymentMethodId) {
        service.deleteByMerchantAndMethod(merchantId, paymentMethodId);
        return ResponseEntity.ok().build();
    }
}