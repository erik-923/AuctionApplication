package edu.elon.eblix.Seller.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WinningBid {
    private String auctionId;
    private double amountOfBid;
    private String bidId;
}
