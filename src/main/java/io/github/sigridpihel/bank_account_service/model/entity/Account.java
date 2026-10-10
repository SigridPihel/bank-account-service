package io.github.sigridpihel.bank_account_service.model.entity;

import org.springframework.data.annotation.Id;

public class Account {

    @Id
    private Long id;

    private String accountNumber;
}
