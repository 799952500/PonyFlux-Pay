package com.payflow.admin.controller;

import com.payflow.admin.entity.PaymentMethod;
import com.payflow.admin.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public List<PaymentMethod> listAll() {
        return paymentMethodService.listAll();
    }

    @GetMapping("/channel/{channelId}")
    public List<PaymentMethod> listByChannelId(@PathVariable Long channelId) {
        return paymentMethodService.listByChannelId(channelId);
    }

    @GetMapping("/{id}")
    public PaymentMethod getById(@PathVariable Long id) {
        return paymentMethodService.getById(id);
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody PaymentMethod method) {
        paymentMethodService.create(method);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody PaymentMethod method) {
        paymentMethodService.update(id, method);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentMethodService.delete(id);
        return ResponseEntity.ok().build();
    }
}