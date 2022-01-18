package com.sjtu.excel;

import com.alibaba.excel.EasyExcel;

import java.util.ArrayList;
import java.util.List;

public class TestWrite {

    public static void main(String[] args) {
        String fileName = "D:\\data\\excel\\01.xlsx";
        List<UserData> userDataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UserData userData = new UserData();
            userData.setUid(i);
            userData.setUsername("jack" + i);
            userDataList.add(userData);
        }
        EasyExcel.write(fileName, UserData.class).sheet("用户信息").doWrite(userDataList);


    }
}
