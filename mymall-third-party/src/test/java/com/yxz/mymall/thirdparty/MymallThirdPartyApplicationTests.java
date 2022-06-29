package com.yxz.mymall.thirdparty;


import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class MymallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Test
    void contextLoads() throws FileNotFoundException {
        InputStream inputStream = new FileInputStream("C:\\Users\\XinzeYu\\Pictures\\eedb756e720667fcf2fb8a65e434f7a2.jpg");
        ossClient.putObject("mymall-yxz", "la.jpg", inputStream);
        ossClient.shutdown();
    }

}
