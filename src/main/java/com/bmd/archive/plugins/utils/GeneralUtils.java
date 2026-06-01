package com.bmd.archive.plugins.utils;

import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.*;

public class GeneralUtils {

    private static final int TIMEOUT = 10;

    private static RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT * 1000)
            .setConnectionRequestTimeout(TIMEOUT * 1000)
            .setSocketTimeout(TIMEOUT * 1000).build();

    public static boolean testConnection(String url){
        try (CloseableHttpClient client =
                     HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            HttpHead httpHead = new HttpHead(url);

            CloseableHttpResponse response;
            try {
                response = client.execute(httpHead);
            } catch (IOException e) {
                return false;
            }

            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode == HttpStatus.SC_NOT_FOUND)
                return false;

            return statusCode < 500;

        } catch (IOException e) {
            return false;
        }
    }

    public static String randomHex(){
        Random rand = new Random();
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return String.format("#%02x%02x%02x", r, g, b);
    }

    public static int[] hexToRBG(String hex){
        int i = Integer.decode(hex);
        return new int[]{(i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF};
    }

}
