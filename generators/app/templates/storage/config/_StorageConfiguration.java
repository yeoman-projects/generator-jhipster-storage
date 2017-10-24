package <%=packageName%>.storage.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import <%=packageName%>.config.HeyiConstants;
import <%=packageName%>.profile.util.DefaultProfileUtil;
import <%=packageName%>.storage.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.util.Arrays;

/**
 * 数据储存配置
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    private final Logger log = LoggerFactory.getLogger(StorageConfiguration.class);
    private final StorageProperties storageProperties;
    private final Environment env;

    @Autowired
    public StorageConfiguration(@SuppressWarnings("SpringJavaAutowiringInspection") StorageProperties storageProperties, Environment env) {
        this.storageProperties = storageProperties;
        this.env = env;
    }

    /**
     * S3 储存客户端
     *
     * @return 客户端
     */
    @Bean
    @ConditionalOnProperty(value = "bigbug.storage.s3.enable", havingValue = "true")
    AmazonS3Client amazonS3Client() {
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        BasicAWSCredentials basicAWSCredentials =
                new BasicAWSCredentials(
                        storageProperties.getStorage().getS3().getAccessKey(),
                        storageProperties.getStorage().getS3().getSecretKey());

        return (AmazonS3Client) AmazonS3ClientBuilder.standard()
                .withClientConfiguration(clientConfig)
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                storageProperties.getStorage().getS3().getEndpoint(), Regions.DEFAULT_REGION.getName()))
                .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                .build();
    }

    @Bean
    StorageService storageService() throws IOException {
        if (Arrays.stream(DefaultProfileUtil.getActiveProfiles(env))
                .anyMatch(profile -> profile.equals(HeyiConstants.SPRING_PROFILE_DEVELOPMENT)))
            return new StorageService(null);

        return new StorageService(amazonS3Client());
    }

}
