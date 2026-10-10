package io.github.sigridpihel.bank_account_service.repository;

import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<Account, Long> {
}
