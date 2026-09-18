package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.exception.UNIHEALTHException;
import gr.uniwa.unihealth.backend.service.FileHandlerService;
import gr.uniwa.unihealth.backend.service.util.ServiceUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileHandlerServiceImpl implements FileHandlerService {

  /** Maximum number of keys a single delete request accepts. */
  private static final int DELETE_BATCH_SIZE = 1000;

  private final S3Client s3Client;

  private final TenantContext tenantContext;

  @Value("${s3-bucket.name}")
  private String filesBucketSuffix;

  /**
   * Anything an S3 bucket name may not contain, replaced with a hyphen.
   *
   * <p>Only the character rule is enforced. The length, leading and trailing character, and
   * IP-address-lookalike rules are not, because tenant ids are filenames this project controls and
   * already satisfy them — a fuller validator here would be guarding against a case that cannot
   * currently arise.
   */
  private static final Pattern ILLEGAL_BUCKET_CHARS = Pattern.compile("[^a-z0-9.-]");

  /**
   * Buckets that are known to exist, so that the lazy initialisation is only executed once per
   * tenant.
   */
  private final Set<String> initializedBuckets = ConcurrentHashMap.newKeySet();

  @PostConstruct
  public void init() {
    tenantContext.runForEachTenant(tenantId -> {
      try {
        ensureBucketExists();
      } catch (RuntimeException e) {
        log.warn(
            "File system is not reachable at startup, bucket for tenant {} could not be initialized." + " It will be created on first use. Cause: {}",
            tenantId, e.getMessage());
      }
    }, "FileHandlerServiceImpl.init");
  }

  /**
   * Makes sure the bucket of the current tenant exists, creating it if needed. Called at startup on
   * a best effort basis and lazily before every upload, so that the application can start while the
   * file system is still unavailable.
   */
  private void ensureBucketExists() {
    String bucketName = getBucketName();
    if (initializedBuckets.contains(bucketName)) {
      return;
    }
    try {
      ListBucketsResponse bucketList = s3Client.listBuckets();
      Bucket bucket =
          bucketList.buckets().stream().filter(b -> b.name().equals(bucketName)).findFirst()
              .orElse(null);
      if (bucket == null) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        s3Client.waiter()
            .waitUntilBucketExists(HeadBucketRequest.builder().bucket(bucketName).build());
        log.info("Bucket {} created", bucketName);
      } else {
        log.info("Bucket {} already exists", bucketName);
      }
      initializedBuckets.add(bucketName);
    } catch (S3Exception e) {
      throw new UNIHEALTHException(null, e.awsErrorDetails().errorMessage(), e);
    } catch (SdkClientException e) {
      throw new UNIHEALTHException(null, "Service could not be contacted for a response", e);
    }
  }

  @Override
  public PutObjectResponse uploadFile(String fileId, String contentType, Long contentSize,
      VirusScanServiceImpl.InputStreamSupplier inputStreamSupplier) {
    ensureBucketExists();
    String bucketName = getBucketName();
    try (InputStream content = inputStreamSupplier.get()) {
      return s3Client.putObject(b -> b.bucket(bucketName).key(fileId).contentType(contentType),
          RequestBody.fromInputStream(content, contentSize));
    } catch (IOException e) {
      throw new UNIHEALTHException(null, "Error while uploading file", e);
    } catch (S3Exception e) {
      uploadErrorHelper(fileId);
      throw new UNIHEALTHException(null, "Error while writing file, rollback executed", e);
    } catch (SdkClientException e) {
      uploadErrorHelper(fileId);
      throw new UNIHEALTHException(null, "Service could not be contacted for a response", e);
    }
  }

  private void uploadErrorHelper(String fileId) {
    try {
      deleteFile(fileId);
    } catch (UNIHEALTHException cleanupFailure) {
      log.error("Could not remove object {} after a failed upload", fileId, cleanupFailure);
    }
  }

  @Override
  public DeleteObjectResponse deleteFile(String fileId) {
    String bucketName = getBucketName();
    try {
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(bucketName).key(fileId).build();
      return s3Client.deleteObject(deleteObjectRequest);
    } catch (S3Exception | SdkClientException e) {
      throw new UNIHEALTHException(null, "Error while deleting file", e);
    }
  }

  @Override
  public void deleteFiles(Collection<String> fileIds) {
    List<String> keys =
        CollectionUtils.emptyIfNull(fileIds).stream().filter(StringUtils::isNotBlank).distinct()
            .toList();
    if (keys.isEmpty()) {
      return;
    }

    String bucketName = getBucketName();
    if (keys.size() <= DELETE_BATCH_SIZE) {
      deleteObjectBatch(bucketName, keys);
    } else {
      ServiceUtils.executeAfterCommit(() -> deleteObjects(bucketName, keys));
    }
  }

  /**
   * One request per {@value #DELETE_BATCH_SIZE} keys, the most a single batch delete request
   * accepts.
   */
  private void deleteObjects(String bucketName, List<String> keys) {
    for (int from = 0; from < keys.size(); from += DELETE_BATCH_SIZE) {
      deleteObjectBatch(bucketName,
          keys.subList(from, Math.min(from + DELETE_BATCH_SIZE, keys.size())));
    }
  }

  private void deleteObjectBatch(String bucketName, List<String> keys) {
    try {
      DeleteObjectsResponse response = s3Client.deleteObjects(
          DeleteObjectsRequest.builder().bucket(bucketName).delete(Delete.builder().objects(
                  keys.stream().map(key -> ObjectIdentifier.builder().key(key).build()).toList())
              .build()).build());

      response.errors().forEach(
          error -> log.error("Could not delete object {} from bucket {}: {}", error.key(),
              bucketName, error.message()));
    } catch (S3Exception | SdkClientException e) {
      log.error("Could not delete objects {} from bucket {}", keys, bucketName, e);
    }
  }

  @Override
  public InputStream getFile(String id) {
    String bucket = getBucketName();
    try {
      return s3Client.getObject(request -> request.bucket(bucket).key(id),
          ResponseTransformer.toInputStream());
    } catch (S3Exception e) {
      throw new UNIHEALTHException(null, "Error retrieving file from file system.", e);
    }
  }

  /**
   * The current tenant's bucket.
   *
   * <p>The tenant id cannot be used verbatim. S3 bucket names are DNS labels, so they allow only
   * lowercase letters, digits, dots and hyphens — and the tenant ids here are {@code uniwa_c1} and
   * {@code uniwa_c2}. The AWS SDK rejects an underscore client-side, before the request is even
   * sent, so it fails no matter how lenient the storage behind it happens to be.
   */
  private String getBucketName() {
    String raw = tenantContext.getCurrentTenant().toLowerCase(Locale.ROOT) + filesBucketSuffix;
    return ILLEGAL_BUCKET_CHARS.matcher(raw).replaceAll("-");
  }
}
