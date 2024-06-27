package edu.elon.eblix.Seller.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.elon.eblix.Seller.models.Bid;
import edu.elon.eblix.Seller.models.WinningBid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AWSSellerService {
    private final SnsClient snsClient;
    private final SqsClient sqsClient;
    private final ObjectMapper mapper;

    @Autowired
    public AWSSellerService(SnsClient snsClient, SqsClient sqsClient) {
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.mapper = new ObjectMapper();
    }

    public String createQueue(String name) {
        Map<QueueAttributeName, String> attributesMap = new HashMap();
        attributesMap.put(QueueAttributeName.VISIBILITY_TIMEOUT, "20");

        CreateQueueRequest request = CreateQueueRequest.builder()
                .queueName(name)
                .attributes(attributesMap)
                .build();
        CreateQueueResponse response = this.sqsClient.createQueue(request);
        return response.queueUrl();
    }


    public String postItemNotification(String topicARN, String productTitle, String itemDescription, Map<String, MessageAttributeValue> attributes) {
        PublishRequest request = (PublishRequest)PublishRequest.builder()
                .topicArn(topicARN)
                .subject(productTitle)
                .message(itemDescription)
                .messageAttributes(attributes)
                .build();
        PublishResponse response = this.snsClient.publish(request);
        return response.messageId();
    }

    public List<Message> pollMessages(String queueUrl) {
        List<Message> messages = new ArrayList();
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .build();

        ReceiveMessageResponse response = this.sqsClient.receiveMessage(request);
        while(response.hasMessages()) {
            List<Message> newMessages = response.messages();
            messages.addAll(newMessages);
            response = this.sqsClient.receiveMessage(request);
        }

        return messages;
    }

    public Bid getBestBid(List<Bid> bidList) {
        if (bidList.isEmpty()) {
            return null;
        }
        Bid bestMessage = bidList.get(0);
        Double bestBid = bestMessage.getBidAmount();
        for (Bid bid : bidList) {
            if (bid.getBidAmount() > bestBid) {
                bestMessage = bid;
                bestBid = bid.getBidAmount();
            }
        }
        return bestMessage;
    }

    private Bid convertToBidDTO(String messageBody) {
        try {
            return this.mapper.readValue(messageBody, Bid.class);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public String convertWinningBidToJson(WinningBid bid) {
        try {
            return this.mapper.writeValueAsString(bid);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public List<Bid> convertBidList(List<Message> messageList) {
        List<Bid> bidsList = new ArrayList<>();
        for (Message message : messageList) {
            bidsList.add(convertToBidDTO(message.body()));
        }

        return bidsList;
    }

    public void notifyWinner(String queueUrl, WinningBid winningBid) {
        SendMessageRequest sendMessageRequest = (SendMessageRequest)SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(convertWinningBidToJson(winningBid))
                .build();
        this.sqsClient.sendMessage(sendMessageRequest);
    }

    public void deleteQueue(String queueURL) {
        DeleteQueueRequest request = DeleteQueueRequest.builder()
                .queueUrl(queueURL)
                .build();
        this.sqsClient.deleteQueue(request);
    }

    public void postCompleteNotification(String auctionCompleteArn, Double salePrice, String auctionId, String bidId) {
        MessageAttributeValue auctionIdValue = MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(auctionId)
                .build();
        MessageAttributeValue bidIdValue = MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(bidId)
                .build();
        Map<String, MessageAttributeValue> attributes = new HashMap<String, MessageAttributeValue>();
        attributes.put("auctionId", auctionIdValue);
        attributes.put("bidId", bidIdValue);
        PublishRequest request = PublishRequest.builder()
                .topicArn(auctionCompleteArn)
                .subject("Auction Complete")
                .message("Sold for $" + salePrice)
                .messageAttributes(attributes)
                .build();
        PublishResponse publishResponse = this.snsClient.publish(request);
    }
}
