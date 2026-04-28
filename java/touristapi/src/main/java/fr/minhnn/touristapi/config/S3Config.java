package fr.minhnn.touristapi.config;

import fr.minhnn.touristapi.exceptions.BadRequestException;
import fr.minhnn.touristapi.exceptions.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
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
@RequiredArgsConstructor
public class S3Config {
    private final S3Properties s3Properties;

    @Bean
    public S3Client s3Client() {
        try (StsClient stsClient = StsClient.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()) {

            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                    .roleArn(s3Properties.getRoleArn())
                    .roleSessionName(s3Properties.getRoleSessionName())
                    .build();

            AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);
            Credentials myCreds = roleResponse.credentials();

            AwsSessionCredentials sessionCredentials = AwsSessionCredentials.create(
                    myCreds.accessKeyId(),
                    myCreds.secretAccessKey(),
                    myCreds.sessionToken()
            );

            return S3Client.builder()
                    .region(Region.of(s3Properties.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                    .build();
        } catch (StsException e) {
            throw new ForbiddenException("Failed to assume role for S3 access: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            throw new BadRequestException("An error occurred while configuring S3 client: " + e.getMessage());
        }
    }
}
