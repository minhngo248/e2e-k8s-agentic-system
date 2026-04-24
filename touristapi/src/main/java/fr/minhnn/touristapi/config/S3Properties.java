package fr.minhnn.touristapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "s3")
public class S3Properties {
    private String roleArn;
    private String roleSessionName;
    private String bucketName;
    private String region;
}
