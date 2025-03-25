package com.victor.payments;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payments")
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;
    private String email;
    private double amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
