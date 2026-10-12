package io.github.sigridpihel.bank_account_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
public class Currency {

    @Id
    private String code;

    private String name;
}
