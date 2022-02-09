package com.sjtu.oss.test;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class OssTest {

    public static void main(String[] args) {
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String format = sdf.format(date);
        System.out.println(format);
    }
}
