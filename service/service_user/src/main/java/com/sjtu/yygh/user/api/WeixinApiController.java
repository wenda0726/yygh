package com.sjtu.yygh.user.api;

import com.alibaba.fastjson.JSONObject;
import com.sjtu.yygh.common.exception.YyghException;
import com.sjtu.yygh.common.helper.JwtHelper;
import com.sjtu.yygh.common.result.Result;
import com.sjtu.yygh.common.result.ResultCodeEnum;
import com.sjtu.yygh.model.user.UserInfo;
import com.sjtu.yygh.user.service.UserInfoService;
import com.sjtu.yygh.user.utils.ConstantWxPropertiesUtils;
import com.sjtu.yygh.user.utils.HttpClientUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@RequestMapping("/api/ucenter/wx")
@Controller //只用作页面跳转，不用作返回数据，如果需要返回json数据，则需要添加ResponseBody注解
public class WeixinApiController {

    @Autowired
    private UserInfoService userInfoService;

    //扫描微信的登录二维码确认登录后，微信返回code值
    //根据code值请求特定的接口，获得accessToken
    //根据accessToken就可以获取扫码人的信息
    @GetMapping("callback")
    public String callback(String code, String state){
        System.out.println("code: " + code);
        System.out.println("state" + state);
        if(StringUtils.isEmpty(code) || StringUtils.isEmpty(state)){
            throw new YyghException(ResultCodeEnum.ILLEGAL_CALLBACK_REQUEST_ERROR);
        }
        //使用code和appid以及appscrect换取access_token
        StringBuffer baseAccessTokenUrl = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/oauth2/access_token")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&code=%s")
                .append("&grant_type=authorization_code");
        String accessTokenUrl = String.format(baseAccessTokenUrl.toString(),
                ConstantWxPropertiesUtils.WX_OPEN_APP_ID,
                ConstantWxPropertiesUtils.WX_OPEN_APP_SECRET, code);
        String result = "";
        try {
            result = HttpClientUtils.get(accessTokenUrl);
            System.out.println("通过code请求相应微信路径获得的结果: " + result);
            JSONObject jsonObject = JSONObject.parseObject(result);
            String accessToken = jsonObject.getString("access_token");
            String openid = jsonObject.getString("openid");
            UserInfo userInfo = userInfoService.selectByOpenid(openid);
            if(userInfo == null){
                String baseUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo" +
                        "?access_token=%s" +
                        "&openid=%s";
                String userInfoUrl = String.format(baseUserInfoUrl, accessToken, openid);
                String userInfoRes = "";
                //通过accessToken和openid查询扫码人的信息
                userInfoRes = HttpClientUtils.get(userInfoUrl);
                System.out.println("userInfoRes: " + userInfoRes);
                JSONObject userInfoJsonObj = JSONObject.parseObject(userInfoRes);
                String nickname = userInfoJsonObj.getString("nickname");
                String headimgurl = userInfoJsonObj.getString("headimgurl");
                //将扫码人的信息加入数据库
                userInfo = new UserInfo();
                userInfo.setNickName(nickname);
                userInfo.setStatus(1);
                userInfo.setOpenid(openid);
                userInfoService.save(userInfo);
            }
            Map<String, Object> map = new HashMap<>();
            String name = userInfo.getName();
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getNickName();
            }
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
            map.put("name", name);
            //如果openid为空，则需要绑定手机号，如果不为空则不需要绑定
            if(StringUtils.isEmpty(userInfo.getPhone())) {
                map.put("openid", userInfo.getOpenid());
            } else {
                map.put("openid", "");
            }
            String token = JwtHelper.createToken(userInfo.getId(), name);
            map.put("token", token);
            //跳转到相应的页面
            return "redirect:" +
                    ConstantWxPropertiesUtils.YYGH_BASE_URL + "/weixin/callback?token=" +map.get("token")
                    +"&openid=" +map.get("openid")+
                    "&name="+URLEncoder.encode((String)map.get("name"),"utf-8");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    //获取微信登录的相关信息
    @GetMapping("getLoginParam")
    @ResponseBody
    public Result genQrConnect(){
        String redirectUri = null;
        Map<String,Object> map = new HashMap<>();
        try {
            redirectUri = URLEncoder.encode(ConstantWxPropertiesUtils.WX_OPEN_REDIRECT_URL, "utf-8");
            map.put("redirect_uri",redirectUri);
            map.put("appid",ConstantWxPropertiesUtils.WX_OPEN_APP_ID);
            map.put("response_type","code");
            map.put("scope","snsapi_login");
            map.put("state", System.currentTimeMillis()+"");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return Result.ok(map);

    }
}
