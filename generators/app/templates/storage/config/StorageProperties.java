package <%=packageName%>.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 配置声明.
 */
@ConfigurationProperties(prefix = "<%=prefixName%>", ignoreUnknownFields = false)
public class StorageProperties {

    private final Storage storage = new Storage();

    public Storage getStorage() {
        return storage;
    }

    public static class Storage {

        private S3 s3;

        public S3 getS3() {
            return s3;
        }

        public void setS3(S3 s3) {
            this.s3 = s3;
        }

        public static class S3 {
            private boolean enabled = false;
            private String endpoint;
            private String accessKey;
            private String secretKey;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public String getAccessKey() {
                return accessKey;
            }

            public void setAccessKey(String accessKey) {
                this.accessKey = accessKey;
            }

            public String getSecretKey() {
                return secretKey;
            }

            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }
        }
    }
}
