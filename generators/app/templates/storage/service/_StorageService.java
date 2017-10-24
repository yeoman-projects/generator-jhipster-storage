package <%=packageName%>.storage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * 对象储存服务
 */
@Component
public class StorageService {
    private final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final AmazonS3 s3Client;
    private final ConcurrentHashMap<String, Bucket> buckets;
    private boolean isLocalFileSystem;
    private File localFileSystemDirectory;

    @Autowired
    public StorageService(AmazonS3 s3Client) throws IOException {
        isLocalFileSystem = s3Client == null;

        if (isLocalFileSystem) {
            this.s3Client = null;
            this.buckets = null;
            localFileSystemDirectory =
                    new File(String.format("%s/%s", new File("build").getAbsolutePath(), "storage"));
            FileUtils.forceMkdir(localFileSystemDirectory);
        } else {
            this.s3Client = s3Client;
            this.buckets = new ConcurrentHashMap<>();
            this.s3Client.listBuckets().forEach(bucket -> buckets.put(bucket.getName(), bucket));
        }
    }

    /**
     * 创建对象
     *
     * @param bucketName     储存桶名称
     * @param key            键
     * @param input          输入流
     * @param objectMetadata 元数据
     * @return 储存对象结果
     */
    public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata objectMetadata) throws IOException {
        // Local file system
        if (isLocalFileSystem) return writeToLocalFileSystem(bucketName, key, input);

        // Remote API
        if (!buckets.containsKey(bucketName)) putBucket(bucketName);
        return s3Client.putObject(bucketName, key, input, objectMetadata);
    }

    /**
     * 创建安全对象
     *
     * @param bucketName     储存桶名称
     * @param key            键
     * @param input          输入流
     * @param objectMetadata 元数据
     * @return 储存对象结果
     */
    public PutObjectResult putSecureObject(String bucketName, String key, InputStream input, ObjectMetadata objectMetadata) throws IOException {
        if (isLocalFileSystem) return writeToLocalFileSystem(bucketName, key, input);

        PutObjectResult result = s3Client.putObject(bucketName, key, input, objectMetadata);
        s3Client.setObjectAcl(bucketName, key, CannedAccessControlList.Private);
        return result;
    }

    /**
     * 更新元数据
     *
     * @param bucketName     储存桶名称
     * @param key            键
     * @param objectMetadata 元数据
     * @return 复制对象结果
     */
    public CopyObjectResult putMetadata(String bucketName, String key, ObjectMetadata objectMetadata) {
        if (isLocalFileSystem) return new CopyObjectResult();

        CopyObjectRequest request = new CopyObjectRequest(bucketName, key, bucketName, key)
                .withNewObjectMetadata(objectMetadata);
        return s3Client.copyObject(request);
    }

    /**
     * 获取对象
     *
     * @param bucketName 储存桶名称
     * @param key        键
     * @return 对象结果
     */
    public S3Object getObject(String bucketName, String key) throws IOException {
        if (isLocalFileSystem) {
            S3Object s3object = new S3Object();
            s3object.setBucketName(bucketName);
            s3object.setKey(key);
            s3object.setObjectContent(FileUtils.openInputStream(
                    new File(String.format("%s/%s/%s", localFileSystemDirectory.getAbsolutePath(), bucketName, key))
            ));
            return s3object;
        }
        return s3Client.getObject(bucketName, key);
    }

    /**
     * 获取对象链接
     *
     * @param bucketName 储存桶名称
     * @param key        键
     * @return 对象链接
     */
    public URL getObjectUrl(String bucketName, String key) {
        if (isLocalFileSystem) {
            return null;
        }

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key);

        // 5分钟内有效
        Instant time = Instant.now();
        time.plus(5, MINUTES);
        request.setExpiration(new Date(time.toEpochMilli()));

        return s3Client.generatePresignedUrl(request);
    }

    /**
     * 创建储存桶
     *
     * @param bucketName 储存桶名称
     */
    private void putBucket(String bucketName) throws IOException {
        if (isLocalFileSystem) {
            FileUtils.forceMkdir(
                    new File(String.format("%s/%s", localFileSystemDirectory.getAbsolutePath(), bucketName)));
        } else {
            buckets.put(bucketName, s3Client.createBucket(bucketName));
        }
    }

    /**
     * 写入本地文件系统
     *
     * @param bucketName 文件夹名称
     * @param key        文件名称
     * @param input      文件流
     * @return 储存对象结果
     */
    private PutObjectResult writeToLocalFileSystem(String bucketName, String key, InputStream input) throws IOException {
        File directory = new File(String.format("%s/%s", localFileSystemDirectory.getAbsolutePath(), bucketName));
        FileUtils.forceMkdir(directory);

        File file = new File(String.format("%s/%s", directory.getAbsolutePath(), key));
        if (!file.exists()) file.createNewFile();

        FileUtils.writeByteArrayToFile(file, IOUtils.toByteArray(input));

        return new PutObjectResult();
    }
}
