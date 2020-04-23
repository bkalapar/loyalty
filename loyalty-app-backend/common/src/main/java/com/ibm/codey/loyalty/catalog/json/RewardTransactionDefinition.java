package com.ibm.codey.loyalty.catalog.json;

import java.math.BigDecimal;

import javax.json.bind.annotation.JsonbProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class RewardTransactionDefinition {

    @JsonbProperty
    private String userId;

    @JsonbProperty
    private String category;

    @JsonbProperty
    private BigDecimal pointsEarned;

}
