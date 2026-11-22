package com.payshield.transaction.Specifications;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.payshield.transaction.Dto.RequestDto.TransactionFilter;
import com.payshield.transaction.Entity.Transaction;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> withFilters(
            TransactionFilter filter) {

        return (root, query, criteriaBuilder) -> {

            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            if (filter == null) {
                return criteriaBuilder.conjunction();
            }

            // Customer reference
            if (filter.customerRef() != null
                    && !filter.customerRef().isBlank()) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("customerRef"),
                                filter.customerRef().trim()
                        )
                );
            }

            // Merchant reference
            if (filter.merchantRef() != null
                    && !filter.merchantRef().isBlank()) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("merchantRef"),
                                filter.merchantRef().trim()
                        )
                );
            }

            // Payment status
            if (filter.paymentStatus() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("paymentStatus"),
                                filter.paymentStatus().name()
                        )
                );
            }

            // Transaction type
            if (filter.transactionType() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("transactionType"),
                                filter.transactionType().name()
                        )
                );
            }

            // Channel
            if (filter.channel() != null) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("channel"),
                                filter.channel().name()
                        )
                );
            }

            // Currency
            if (filter.currency() != null
                    && !filter.currency().isBlank()) {

                predicates.add(
                        criteriaBuilder.equal(
                                root.get("currency"),
                                filter.currency().trim()
                        )
                );
            }

            // From date
            if (filter.fromDate() != null) {

                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("occurredAt"),
                                filter.fromDate()
                        )
                );
            }

            // To date
            if (filter.toDate() != null) {

                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("occurredAt"),
                                filter.toDate()
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]
                    )
            );
        };
    }
}