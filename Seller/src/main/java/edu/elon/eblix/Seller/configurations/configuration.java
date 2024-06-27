package edu.elon.eblix.Seller.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class configuration {
    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.accessKey}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.sessionToken}")
    private String sessionToken;

    @Bean
    public SnsClient snsClient() {
        AwsSessionCredentials credentials = AwsSessionCredentials.create(
                this.accessKey,
                this.secretKey,
                this.sessionToken
        );

        return SnsClient.builder()
                .region(Region.of(this.awsRegion))
                .credentialsProvider(() -> credentials)
                .build();
    }

    @Bean
    public SqsClient sqsClient() {
        AwsSessionCredentials credentials = AwsSessionCredentials.create(
                this.accessKey,
                this.secretKey,
                this.sessionToken
        );

        return SqsClient.builder()
                .region(Region.of(this.awsRegion))
                .credentialsProvider(() -> credentials)
                .build();
    }
}
