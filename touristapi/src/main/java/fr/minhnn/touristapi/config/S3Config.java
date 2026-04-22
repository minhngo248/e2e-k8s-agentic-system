package fr.minhnn.touristapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.StsException;

@Configuration
public class S3Config {
    private static final String ROLE_ARN = "arn:aws:iam::058264135127:role/AssumeRoleSpringBootApi";
    private static final String ROLE_SESSION_NAME = "AssumeRoleSpringBootApi";

    @Bean
    public S3Client s3Client() {
        try (StsClient stsClient = StsClient.builder()
                .region(Region.EU_WEST_3)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                    .roleArn(ROLE_ARN)
                    .roleSessionName(ROLE_SESSION_NAME)
                    .build();

            AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);
            Credentials myCreds = roleResponse.credentials();

            AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                    myCreds.accessKeyId(),
                    myCreds.secretAccessKey(),
                    myCreds.sessionToken()
            );

            return S3Client.builder()
                    .region(Region.EU_WEST_3)
                    .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                    .build();
        } catch (StsException e) {
            throw new IllegalStateException("Failed to assume role: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create S3 client", e);
        }
    }
}
