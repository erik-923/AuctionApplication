package edu.elon.eblix.Seller.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Item {
    private String auctionId;
    private String productTitle;
    private String productCategory;
    private String description;
    private int auctionLength;
    private String bidQueueUrl;
    private String notificationId;
}
