import json
import random
import time
import uuid

import boto3

class Bidder:
    def __init__(self):
        config = self.parse_properties()
        self.for_sale_topic_arn = config['forSaleArn']
        self.auction_complete_topic_arn = config['auctionCompleteArn']
        self.categories = config['categories']
        self.name = config['bidderName']
        self.account_balance = 100
        self.sqs_client = boto3.client('sqs')
        self.sns_client = boto3.client('sns')
        self.submitted_bids = []

        self.for_sale_queue_url = None
        self.accepted_bids_queue_url = None
        self.auction_complete_queue_url = None

    def setup(self):
        self.create_for_sale_queue()
        self.create_accepted_bids_queue()
        self.create_auction_complete_queue()

    def parse_properties(self):
        props = {}
        with open('application.properties', 'r') as f:
            for line in f:
                key, value = line.strip().split('=')
                if key == 'categories':
                    value = value.split(',')
                props[key] = value

        return props

    def create_for_sale_queue(self):
        response = self.sqs_client.create_queue(
            QueueName=self.name + '-for-sale-queue'
        )
        self.for_sale_queue_url = response['QueueUrl']
        response = self.sqs_client.get_queue_attributes(
            QueueUrl=self.for_sale_queue_url,
            AttributeNames=['QueueArn']
        )
        queue_arn = response['Attributes']['QueueArn']
        policy = {
                "Policy": json.dumps(
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Sid": self.for_sale_topic_arn,
                                "Effect": "Allow",
                                "Principal": {
                                    "AWS": "*"
                                },
                                "Action": "sqs:SendMessage",
                                "Resource": queue_arn,
                                "Condition": {
                                    "ArnEquals": {
                                        "aws:SourceArn": self.for_sale_topic_arn
                                    }
                                }
                            }
                        ]
                    }

                )
            }
        policy_response = self.sqs_client.set_queue_attributes(
            QueueUrl=self.for_sale_queue_url,
            Attributes=policy
        )

        filter_policy = {"FilterPolicy": json.dumps({
            "category": self.categories,
        })}

        subscription_response = self.sns_client.subscribe(
            TopicArn=self.for_sale_topic_arn,
            Protocol='sqs',
            Endpoint=queue_arn,
            Attributes=filter_policy
        )


    def create_accepted_bids_queue(self):
        # Create SQS queue for accepted bids
        response = self.sqs_client.create_queue(
            QueueName= self.name + '-accepted-bids-queue'
        )
        self.accepted_bids_queue_url = response['QueueUrl']

    def create_auction_complete_queue(self):
        # Create SQS queue for auction complete topic
        response = self.sqs_client.create_queue(
            QueueName=self.name + '-auction-complete-queue'
        )
        self.auction_complete_queue_url = response['QueueUrl']

        # Get the ARN of the created SQS queue
        response = self.sqs_client.get_queue_attributes(
            QueueUrl=self.auction_complete_queue_url,
            AttributeNames=['QueueArn']
        )
        queue_arn = response['Attributes']['QueueArn']
        policy = {
            "Policy": json.dumps(
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Sid": self.auction_complete_topic_arn,
                            "Effect": "Allow",
                            "Principal": {
                                "AWS": "*"
                            },
                            "Action": "sqs:SendMessage",
                            "Resource": queue_arn,
                            "Condition": {
                                "ArnEquals": {
                                    "aws:SourceArn": self.auction_complete_topic_arn
                                }
                            }
                        }
                    ]
                }

            )
        }
        policy_response = self.sqs_client.set_queue_attributes(
            QueueUrl=self.auction_complete_queue_url,
            Attributes=policy
        )
        # Subscribe the SQS queue to the SNS topic
        subscription_response = self.sns_client.subscribe(
            TopicArn=self.auction_complete_topic_arn,
            Protocol='sqs',
            Endpoint=queue_arn
        )

    def run(self):
        while True:
            self.check_for_new_items()
            self.check_accepted_bids()
            self.check_completed_auctions()
            time.sleep(20)

    def check_for_new_items(self):
        print("Checking for new items...")
        response = self.sqs_client.receive_message(
            QueueUrl=self.for_sale_queue_url,
            MaxNumberOfMessages=10,
            VisibilityTimeout=30,
            WaitTimeSeconds=2
        )

        if 'Messages' in response:
            for message in response['Messages']:
                message_body = json.loads(message['Body'])
                self.make_bid(message_body)

                self.sqs_client.delete_message(
                    QueueUrl=self.for_sale_queue_url,
                    ReceiptHandle=message['ReceiptHandle']
                )
        else:
            print("No available items")

    def check_accepted_bids(self):
        print("Checking for accepted bids...")
        response = self.sqs_client.receive_message(
            QueueUrl=self.accepted_bids_queue_url,
            MaxNumberOfMessages=10,
            VisibilityTimeout=30,
            WaitTimeSeconds=2
        )

        if 'Messages' in response:
            for message in response['Messages']:
                attributes = json.loads(message['Body'])

                print(f"Accepted Bid for Auction ID: {attributes['auctionId']}, Amount: {attributes['amountOfBid']}")
                print(f"Current Balance: {round(self.account_balance, 2)}")
                self.sqs_client.delete_message(
                    QueueUrl=self.accepted_bids_queue_url,
                    ReceiptHandle=message['ReceiptHandle']
                )
        else:
            print('No accepted bids found')

    def check_completed_auctions(self):
        print("Checking for completed auctions...")
        response = self.sqs_client.receive_message(
            QueueUrl=self.auction_complete_queue_url,
            MaxNumberOfMessages=10,
            VisibilityTimeout=30,
            WaitTimeSeconds=2
        )

        if 'Messages' in response:
            for message in response['Messages']:
                message_body = json.loads(message['Body'])

                self.handle_completed_auction(message_body)

                self.sqs_client.delete_message(
                    QueueUrl=self.auction_complete_queue_url,
                    ReceiptHandle=message['ReceiptHandle']
                )
        else:
            print('No completed auctions found')

    def make_bid(self, item_body):
        if self.account_balance > 0:
            attributes = item_body['MessageAttributes']
            amount = round(random.uniform(0, self.account_balance), 2)
            bid_id = str(uuid.uuid4())
            bid = {
                'bidId': bid_id,
                'email': self.name + '@elon.edu',
                'bidAmount': amount,
                'acceptanceQueue': self.accepted_bids_queue_url
            }

            self.sqs_client.send_message(
                QueueUrl=attributes['queue']['Value'],
                MessageBody=json.dumps(bid)
            )
            completed_bid = {
                'auctionId': attributes['auctionId']['Value'],
                'amount': amount,
                'bidId': bid_id
            }
            self.submitted_bids.append(completed_bid)
            self.account_balance -= amount
            print(f"Bid placed for Auction ID: {attributes['auctionId']['Value']}, Amount: {amount}")
        else:
            print("Insufficient funds")

    def handle_completed_auction(self, message_body):
        attributes = message_body['MessageAttributes']
        for d in self.submitted_bids:
            if d.get('bidId') == attributes['bidId']['Value']:
                self.submitted_bids.remove(d)
                pass
            elif d.get('auctionId') == attributes['auctionId']['Value']:
                amount = float(d.get('amount'))
                self.account_balance += amount

                print(f"Auction Completed for Auction ID: {attributes['auctionId']['Value']}")
                print(f"Current Balance: {round(self.account_balance, 2)}")
        print('No Completed Auctions Found')




if __name__ == "__main__":
    # Create bidder instance and set up
    bidder = Bidder()
    bidder.setup()

    # Run the bidder
    bidder.run()
