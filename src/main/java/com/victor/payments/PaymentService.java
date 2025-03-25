package com.victor.payments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    @Value("${flutterwave.secret-key}")
    private String secretKey;

    private final String CHARGE_URL = "https://api.flutterwave.com/v3/payments";
    private final String VERIFY_URL = "https://api.flutterwave.com/v3/transactions/";

    private final WebClient webClient;
    private final PaymentRepository paymentRepository;

    public PaymentService(WebClient webClient, PaymentRepository paymentRepository) {
        this.webClient = webClient;
        this.paymentRepository = paymentRepository;
    }

    public Mono<String> initiatePayment(String email, double amount) {
        String txRef = "tx_" + UUID.randomUUID().toString();

        Map<String, Object> requestBody = Map.of(
                "tx_ref", txRef,
                "amount", amount,
                "currency", "NGN",
                "redirect_url", "https://yourwebsite.com/payment-success",
                "customer", Map.of("email", email)
        );

        log.info("Initiating payment for email: {}, amount: {}, reference: {}", email, amount, txRef);

        return webClient.post()
                .uri(CHARGE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> log.info("Payment initiation response: {}", response))
                .map(response -> {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    return data.get("link").toString(); // Payment link
                });
    }

    public Mono<String> verifyPayment(String transactionId) {
        log.info("Verifying payment for transaction ID: {}", transactionId);

        return webClient.get()
                .uri(VERIFY_URL + transactionId + "/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(response -> log.info("Payment verification response: {}", response))
                .map(response -> {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    String status = data.get("status").toString();
                    log.info("Transaction {} verification status: {}", transactionId, status);
                    return "Transaction Status: " + status;
                });
    }

    public void handleSuccessfulPayment(Map<String, Object> data) {
        log.info("Received webhook data: {}", data);

        try {
            String transactionId = data.get("id").toString();
            String email = ((Map<String, Object>) data.get("customer")).get("email").toString();
            double amount = Double.parseDouble(data.get("amount").toString());
            String currency = data.get("currency").toString();
            String status = data.get("status").toString();

            log.info("Processing payment: transactionId={}, email={}, amount={}, currency={}, status={}",
                    transactionId, email, amount, currency, status);

            // Check if the payment already exists
            Optional<Payment> existingPayment = paymentRepository.findByTransactionId(transactionId);
            if (existingPayment.isPresent()) {
                log.warn("Duplicate webhook call detected for transaction ID: {}", transactionId);
                return; // Ignore duplicate webhook calls
            }

            // Save payment
            Payment payment = new Payment();
            payment.setTransactionId(transactionId);
            payment.setEmail(email);
            payment.setAmount(amount);
            payment.setCurrency(currency);
            payment.setStatus(status);
            payment.setCreatedAt(LocalDateTime.now());

            paymentRepository.save(payment);
            log.info("Payment saved successfully: transactionId={}", transactionId);

        } catch (Exception e) {
            log.error("Error processing webhook data: {}", e.getMessage(), e);
        }
    }


    public boolean verifyWebhookSignature(String receivedHash) {
        String expectedHash = org.springframework.util.DigestUtils.md5DigestAsHex(secretKey.getBytes());
        boolean isValid = expectedHash.equals(receivedHash);
        log.info("Webhook signature verification result: {}", isValid);
        return isValid;
    }



}
//echo "# flutterwave-card-payment" >> README.md
//git init
//git add README.md
//git commit -m "first commit"
//git branch -M main
//git remote add origin https://github.com/Victoruk11/flutterwave-card-payment.git
//git push -u origin main