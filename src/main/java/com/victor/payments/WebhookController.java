package com.victor.payments;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
@Slf4j
public class WebhookController {

        private final PaymentService paymentService;

        public WebhookController(PaymentService paymentService) {
            this.paymentService = paymentService;
        }

        @PostMapping
        public ResponseEntity<String> receiveWebhook(
                @RequestBody Map<String, Object> payload,
                @RequestHeader(value = "verif-hash", required = false) String signature) {

            // Verify webhook signature (if required)
            if (signature != null && !paymentService.verifyWebhookSignature(signature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            // Process only successful payments
            if ("charge.completed".equals(payload.get("event"))) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                paymentService.handleSuccessfulPayment(data);
            }

            return ResponseEntity.ok("Webhook received successfully");
        }
    }
