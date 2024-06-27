# Auction Marketplace Application

Welcome to the Auction Marketplace Application! This project is a robust auction system where sellers can post items, and bidders can subscribe to receive notifications about new items and bid on them. The application leverages AWS SNS and SQS for a scalable and efficient notification and bidding system.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technologies Used](#technologies-used)
- [Setup](#setup)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [Usage](#usage)
- [License](#license)
- [Contact](#contact)

## Overview

This application provides a marketplace for auctioning items. Sellers can list items, and bidders can subscribe to item categories they are interested in. Notifications are sent via AWS SNS and SQS to keep bidders informed about new items and auction outcomes.

## Features

- **Seller Functionality**:
  - Post new items for auction.
  - Monitor bids on items.

- **Bidder Functionality**:
  - Subscribe to item categories to receive notifications about new items.
  - Place bids on items.
  - Receive notifications if they win an auction or if an auction ends.

- **Notification System**:
  - AWS SNS for topic-based notifications.
  - AWS SQS for handling bid submissions and auction outcomes.

## Architecture

The application is built using Java and Python, integrated with AWS SNS and SQS for messaging and notifications. Below is a high-level overview of the architecture:

1. **AWS SNS (Simple Notification Service)**:
   - Used to notify bidders about new items and auction statuses.
   - Bidders subscribe to specific item type topics.

2. **AWS SQS (Simple Queue Service)**:
   - Each seller has three SQS queues:
     - **Incoming Notifications Queue**: Receives notifications about new bids.
     - **Winning Bids Queue**: Notifies the seller about the winning bid when an auction ends.
     - **Completed Auctions Queue**: Notifies all subscribers when an auction ends.

3. **Java and Python Integration**:
   - Java handles the backend logic for posting items and handling auctions.
   - Python is used for managing notifications and interactions with AWS services.

## Technologies Used

- **Java**: Core application logic and backend.
- **Python**: AWS SNS and SQS interactions.
- **AWS SNS**: Notification service.
- **AWS SQS**: Queue service for bid management and notifications.
