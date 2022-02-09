package com.sjtu.yygh.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.sjtu.yygh.oss.service.FileService;
import com.sjtu.yygh.oss.utils.ConstantOssPropertiesUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    @Override
    public String upload(MultipartFile file) {
        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-shanghai.aliyuncs.com。
        String endpoint = ConstantOssPropertiesUtils.ENDPOINT;
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = ConstantOssPropertiesUtils.ACCESS_KEY_ID;
        String accessKeySecret = ConstantOssPropertiesUtils.SECRET;
        String bucketName = ConstantOssPropertiesUtils.BUCKET;

        try {
            // 创建OSSClient实例。
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // 填写本地文件的完整路径。如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
            InputStream inputStream = file.getInputStream();
            String fileName = file.getOriginalFilename();
            //为了解决fileName重复的问题，生成一个随机uuid值进行拼接
            String uuid = UUID.randomUUID().toString().replaceAll("-","");
            fileName = uuid + fileName;
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            String format = sdf.format(date);
            fileName = format + "/" + fileName;
            // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
            ossClient.putObject(bucketName, fileName, inputStream);
            //https://yygh-swd-sjtu.oss-cn-shanghai.aliyuncs.com/leetcode437.png
            String url = "https://" + bucketName + "." + endpoint + "/" + fileName;
            // 关闭OSSClient。
            ossClient.shutdown();
            return url;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
