/*
 * 文件名：OSSUnit.java
 * 版权：深圳柚安米科技有限公司版权所有
 * 修改人：guohao
 * 修改时间：2017年3月6日
 * 修改内容：新增
 */
package oss.utils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import com.youanmi.scrm.commons.util.file.PropertiesUtils;
import com.youanmi.scrm.commons.util.string.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;


/**
 * OSS工具类
 * 
 * @author guohao 2017年3月6日
 * @version 1.0.0
 */
public class OSSUnit {

    // log
    private static final Logger LOG = LoggerFactory.getLogger(OSSUnit.class);

    // 阿里云API的内或外网域名
    private static String ENDPOINT;
    // 阿里云API的密钥Access Key ID
    private static String ACCESS_KEY_ID;
    // 阿里云API的密钥Access Key Secret
    private static String ACCESS_KEY_SECRET;
    // 空间
    public static String BUCKET_NAME;
    // 文件存储目录
    public static String DISK_NAME;

    public static String HOST;


    /**
     *
     * 获取classpath
     *
     * @return
     */
    public static String getClassPath() {
        URL url = OSSUnit.class.getClassLoader().getResource("");
        return url.getPath();
    }


    /**
     *
     * 初始化
     *
     * @param fileName
     */
    public static void build(String fileName) {

        // 获取OSS配置
        Properties pros = PropertiesUtils.getProperties(fileName);
        ENDPOINT = pros.getProperty("endpoint");
        ACCESS_KEY_ID = pros.getProperty("accessKeyId");
        ACCESS_KEY_SECRET = pros.getProperty("accessKeySecret");
        BUCKET_NAME = pros.getProperty("bucketName");
        DISK_NAME = formatDateTime(pros.getProperty("diskName"));
        HOST = pros.getProperty("ossHost");
        if (AssertUtils.isNull(ENDPOINT) || AssertUtils.isNull(ACCESS_KEY_ID)
                || AssertUtils.isNull(ACCESS_KEY_SECRET) || AssertUtils.isNull(BUCKET_NAME)
                || AssertUtils.isNull(DISK_NAME) || AssertUtils.isNull(HOST)) {
            throw new RuntimeException("OSS配置文件配置属性没有找到");
        }


    }


    private static String formatDateTime(String path){
        if(path==null){
            return path;
        }
        return path.replaceAll("\\$\\{year\\}", new SimpleDateFormat("yyyy").format(new Date()))
                .replaceAll("\\$\\{month\\}", new SimpleDateFormat("MM").format(new Date()))
                .replaceAll("\\$\\{day\\}", new SimpleDateFormat("dd").format(new Date()))
                .replaceAll("\\$\\{hours\\}", new SimpleDateFormat("HH").format(new Date()))
                .replaceAll("\\$\\{minutes\\}", new SimpleDateFormat("mm").format(new Date()))
                .replaceAll("\\$\\{seconds\\}", new SimpleDateFormat("ss").format(new Date()));
    }


    /**
     * 获取阿里云OSS客户端对象
     * */
    public static final OSSClient getOSSClient() {
        return new OSSClient(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
    }


    /**
     * 新建Bucket --Bucket权限:私有
     * 
     * @param bucketName
     *            bucket名称
     * @return true 新建Bucket成功
     * */
    public static final boolean createBucket(OSSClient client, String bucketName) {
        Bucket bucket = client.createBucket(bucketName);
        return bucketName.equals(bucket.getName());
    }


    /**
     * 删除Bucket
     * 
     * @param bucketName
     *            bucket名称
     * */
    public static final void deleteBucket(OSSClient client, String bucketName) {
        client.deleteBucket(bucketName);
        LOG.info("删除" + bucketName + "Bucket成功");
    }


    /**
     * 向阿里云的OSS存储中存储文件
     * 
     * @param client
     *            OSS客户端
     * @param file
     *            上传文件
     * @param bucketName
     *            bucket名称
     * @param diskName
     *            上传文件的目录 --bucket下文件的路径
     * @return String 唯一MD5数字签名
     * */
    public static final String uploadObject2OSS(OSSClient client, File file, String bucketName,
            String diskName) {
        String resultStr = null;
        try {
            InputStream is = new FileInputStream(file);
            String fileName = file.getName();
            Long fileSize = file.length();
            // 创建上传Object的Metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(is.available());
            metadata.setCacheControl("no-cache");
            metadata.setHeader("Pragma", "no-cache");
            metadata.setContentEncoding("utf-8");
            metadata.setContentType(getContentType(fileName));
            // metadata.setContentDisposition("filename/filesize=" + fileName +
            // "/" + fileSize + "Byte.");
            // 上传文件
            PutObjectResult putResult = client.putObject(bucketName, diskName + fileName, is, metadata);
            // 解析结果
            resultStr = putResult.getETag();
        }
        catch (Exception e) {
            LOG.error("上传阿里云OSS服务器异常." + e.getMessage(), e);
        }
        return resultStr;
    }


    /**
     * 根据key获取OSS服务器上的文件输入流
     * 
     * @param client
     *            OSS客户端
     * @param bucketName
     *            bucket名称
     * @param diskName
     *            文件路径
     * @param key
     *            Bucket下的文件的路径名+文件名
     */
    public static final InputStream getOSS2InputStream(OSSClient client, String bucketName, String diskName,
            String key) {
        OSSObject ossObj = client.getObject(bucketName, diskName + key);
        return ossObj.getObjectContent();
    }


    /**
     * 根据key删除OSS服务器上的文件
     * 
     * @param client
     *            OSS客户端
     * @param bucketName
     *            bucket名称
     * @param diskName
     *            文件路径
     * @param key
     *            Bucket下的文件的路径名+文件名
     */
    public static void deleteFile(OSSClient client, String bucketName, String diskName, String key) {
        client.deleteObject(bucketName, diskName + key);
        LOG.info("删除" + bucketName + "下的文件" + diskName + key + "成功");
    }


    /**
     * 通过文件名判断并获取OSS服务文件上传时文件的contentType
     * 
     * @param fileName
     *            文件名
     * @return 文件的contentType
     */
    public static final String getContentType(String fileName) {
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
        if ("bmp".equalsIgnoreCase(fileExtension))
            return "image/bmp";
        if ("gif".equalsIgnoreCase(fileExtension))
            return "image/gif";
        if ("jpeg".equalsIgnoreCase(fileExtension) || "jpg".equalsIgnoreCase(fileExtension)
                || "png".equalsIgnoreCase(fileExtension))
            return "image/jpeg";
        if ("html".equalsIgnoreCase(fileExtension))
            return "text/html";
        if ("txt".equalsIgnoreCase(fileExtension))
            return "text/plain";
        if ("vsd".equalsIgnoreCase(fileExtension))
            return "application/vnd.visio";
        if ("ppt".equalsIgnoreCase(fileExtension) || "pptx".equalsIgnoreCase(fileExtension))
            return "application/vnd.ms-powerpoint";
        if ("doc".equalsIgnoreCase(fileExtension) || "docx".equalsIgnoreCase(fileExtension))
            return "application/msword";
        if ("xml".equalsIgnoreCase(fileExtension))
            return "text/xml";
        return "text/html";
    }

}
