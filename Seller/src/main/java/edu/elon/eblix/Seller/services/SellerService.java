package edu.elon.eblix.Seller.services;

import edu.elon.eblix.Seller.models.Bid;
import edu.elon.eblix.Seller.models.Item;
import edu.elon.eblix.Seller.models.WinningBid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SellerService {
    @Value("${auctionCompleteArn}")
    private String auctionCompleteArn;
    @Value("${forSaleArn}")
    private String forSaleArn;
    @Autowired
    private AWSSellerService awsSellerService;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public Item postItemForSale(Item item) {
        // create the id for the action and name the auction using the id
        item.setAuctionId(UUID.randomUUID().toString());
        String name = "auction-" + item.getAuctionId();
        // create the queue for bids to be sent
        item.setBidQueueUrl(this.awsSellerService.createQueue(name));
        item.setNotificationId(this.postItemMessage(item));
        System.out.println("Starting Auction: " + item.getAuctionId() + " in the category of: " + item.getProductCategory());
        System.out.println("Going to sleep for " + item.getAuctionLength() + " seconds...");

        // adding sleep operation to the executor to work in separatre thread
        executor.submit(() -> {
            try {
                Thread.sleep(item.getAuctionLength() * 1000);
            } catch (InterruptedException e) {
                System.out.println(e);
            }
            System.out.println("Woke Up");
            this.processBids(item);
        });

        return item;
    }

    public String postItemMessage(Item item) {
        Map<String, String> attributes = new HashMap();
        attributes.put("auctionId", item.getAuctionId());
        attributes.put("category", item.getProductCategory());
        attributes.put("length", Integer.valueOf(item.getAuctionLength()).toString());
        attributes.put("queue", item.getBidQueueUrl());
        Map<String, MessageAttributeValue> awsAttributes = this.convertAttributesMap(attributes);
        return this.awsSellerService.postItemNotification(this.forSaleArn, item.getProductTitle(), item.getDescription(), awsAttributes);
    }

    private Map<String, MessageAttributeValue> convertAttributesMap(Map<String, String> attributeMap) {
        Map<String, MessageAttributeValue> newAttributeMap = new HashMap();

        for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
            String attributeName = entry.getKey();
            String attributeValue = entry.getValue();
            MessageAttributeValue value = MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(attributeValue)
                    .build();
            newAttributeMap.put(attributeName, value);
        }
        return newAttributeMap;
    }

    public void processBids(Item item) {
        List<Bid> bidList = this.awsSellerService.convertBidList(this.awsSellerService.pollMessages(item.getBidQueueUrl()));
        Bid winningBid = this.awsSellerService.getBestBid(bidList);
        this.awsSellerService.deleteQueue(item.getBidQueueUrl());
        if (winningBid == null) {
            System.out.println("No bids were submitted.");
            return;
        }
        System.out.println("Item sold for: $" + winningBid.getBidAmount());
        WinningBid winningBidNotification = new WinningBid();
        winningBidNotification.setAmountOfBid(winningBid.getBidAmount());
        winningBidNotification.setAuctionId(item.getAuctionId());
        winningBidNotification.setBidId(winningBid.getBidId());
        this.awsSellerService.notifyWinner(winningBid.getAcceptanceQueue(), winningBidNotification);
        this.awsSellerService.postCompleteNotification(this.auctionCompleteArn, winningBid.getBidAmount(), item.getAuctionId(), winningBid.getBidId());
    }
}
