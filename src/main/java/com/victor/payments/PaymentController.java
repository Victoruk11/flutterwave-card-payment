package com.victor.payments;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

        private final PaymentService paymentService;

        public PaymentController(PaymentService paymentService) {
            this.paymentService = paymentService;
        }

        @PostMapping("/initiate")
        public Mono<ResponseEntity<String>> initiatePayment(@RequestBody PaymentRequest request) {
            return paymentService.initiatePayment(request.email(), request.amount())
                    .map(link -> ResponseEntity.ok("Payment Link: " + link));
        }

        @GetMapping("/verify/{transactionId}")
        public Mono<ResponseEntity<String>> verifyPayment(@PathVariable String transactionId) {
            return paymentService.verifyPayment(transactionId)
                    .map(ResponseEntity::ok);
        }

    }

