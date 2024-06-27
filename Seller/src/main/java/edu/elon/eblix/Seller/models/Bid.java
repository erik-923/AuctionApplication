package edu.elon.eblix.Seller.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Bid {
    private String bidId;
    private String email;
    private double bidAmount;
    private String acceptanceQueue;
}
