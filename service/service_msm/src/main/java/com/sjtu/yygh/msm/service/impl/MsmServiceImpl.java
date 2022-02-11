package com.sjtu.yygh.msm.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.sjtu.yygh.msm.service.MsmService;
import com.sjtu.yygh.msm.utils.ConstantPropertiesUtils;
import com.sjtu.yygh.vo.msm.MsmVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class MsmServiceImpl implements MsmService {
    @Override
    public boolean send(String phone, String code) {
        if(StringUtils.isEmpty(phone)){
            return false;
        }
        //整合阿里云短信服务
        //设置相关参数 固定格式无须变动
//        DefaultProfile profile = DefaultProfile.
//                getProfile(ConstantPropertiesUtils.REGION_Id,
//                        ConstantPropertiesUtils.ACCESS_KEY_ID,
//                        ConstantPropertiesUtils.SECRET);
//        IAcsClient client = new DefaultAcsClient(profile);
//        CommonRequest request = new CommonRequest();
//        //request.setProtocol(ProtocolType.HTTPS);
//        request.setMethod(MethodType.POST);
//        request.setDomain("dysmsapi.aliyuncs.com");
//        request.setVersion("2017-05-25");
//        request.setAction("SendSms");

        //设置自己的短信服务参数
        //手机号
//        request.putQueryParameter("PhoneNumbers", phone);
//        //签名名称
//        request.putQueryParameter("SignName", "sjtu预约挂号平台");
//        //模板code
//        request.putQueryParameter("TemplateCode", "SMS_180051135");
//        //验证码  使用json格式   {"code":"123456"}
//        Map<String,Object> param = new HashMap();
//        param.put("code",code);
//        request.putQueryParameter("TemplateParam", JSONObject.toJSONString(param));


        //调用方法进行短信发送
//        try {
//            CommonResponse response = client.getCommonResponse(request);
//            System.out.println(response.getData());
//            return response.getHttpResponse().isSuccess();
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (ClientException e) {
//            e.printStackTrace();
//        }

        return true;
    }

    //mq发送短信封装
    @Override
    public boolean send(MsmVo msmVo) {
        if(!StringUtils.isEmpty(msmVo.getPhone())){
            String code = (String)msmVo.getParam().get("code");
            return this.send(msmVo.getPhone(),msmVo.getParam());
        }
        return false;
    }


    private boolean send(String phone, Map<String,Object> param) {
        if(StringUtils.isEmpty(phone)){
            return false;
        }
        //整合阿里云短信服务
        //设置相关参数 固定格式无须变动
//        DefaultProfile profile = DefaultProfile.
//                getProfile(ConstantPropertiesUtils.REGION_Id,
//                        ConstantPropertiesUtils.ACCESS_KEY_ID,
//                        ConstantPropertiesUtils.SECRET);
//        IAcsClient client = new DefaultAcsClient(profile);
//        CommonRequest request = new CommonRequest();
//        //request.setProtocol(ProtocolType.HTTPS);
//        request.setMethod(MethodType.POST);
//        request.setDomain("dysmsapi.aliyuncs.com");
//        request.setVersion("2017-05-25");
//        request.setAction("SendSms");

        //设置自己的短信服务参数
        //手机号
//        request.putQueryParameter("PhoneNumbers", phone);
//        //签名名称
//        request.putQueryParameter("SignName", "sjtu预约挂号平台");
//        //模板code
//        request.putQueryParameter("TemplateCode", "SMS_180051135");
//        //验证码  使用json格式   {"code":"123456"}
//        Map<String,Object> param = new HashMap();
//        param.put("code",code);
//        request.putQueryParameter("TemplateParam", JSONObject.toJSONString(param));


        //调用方法进行短信发送
//        try {
//            CommonResponse response = client.getCommonResponse(request);
//            System.out.println(response.getData());
//            return response.getHttpResponse().isSuccess();
//        } catch (ServerException e) {
//            e.printStackTrace();
//        } catch (ClientException e) {
//            e.printStackTrace();
//        }

        return true;
    }
}
