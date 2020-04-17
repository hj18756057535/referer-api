package com.hj.baidu.utils;

import com.hj.baidu.entity.RefererApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author jie hong
 */
@Component
@Slf4j
public class RefererApiUtils {
    private static final String ACCESS_KEY = "公钥";
    private static final String SECRET_KEY = "私钥";
    private final RestTemplate restTemplate;
    private static final DateTimeFormatter ALTERNATE_ISO8601_DATE_FORMAT =
            ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Charset UTF8 = Charset.forName(DEFAULT_ENCODING);


    public RefererApiUtils(@Qualifier("refererUrlRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String sha256Hex(String signingKey, String stringToSign) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey.getBytes(UTF8), "HmacSHA256"));
            return new String(Hex.encodeHex(mac.doFinal(stringToSign.getBytes(UTF8))));
        } catch (Exception e) {
            throw new RuntimeException("Fail to generate the signature", e);
        }
    }

    @Cacheable(value = "refererApiCache", key = "#eqid")
    public String getEqid(String eqid) {
        try {
            String requestUrl = "/v1/eqid/" + eqid;
            String time = ALTERNATE_ISO8601_DATE_FORMAT.print(new DateTime(new Date()));
            String authString = "bce-auth-v1/" + ACCESS_KEY + "/" + time + "/1800";
            String signingKey = this.sha256Hex(SECRET_KEY, authString);
            String canonicalHeader = "host:referer.bj.baidubce.com";
            String canonicalRequest = "GET\n" + requestUrl + "\n\n" + canonicalHeader;
            String signature = this.sha256Hex(signingKey, canonicalRequest);
            String authorizationHeader = authString + "/host/" + signature;

            HttpHeaders headers = new HttpHeaders();
            headers.add("accept-encoding", "gzip, deflate");
            headers.add("host", "referer.bj.baidubce.com");
            headers.add("content-type", "application/json; charset=utf-8");
            headers.add("x-bce-date", time);
            headers.add("authorization", authorizationHeader);
            headers.add("accept", "∗/∗");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<RefererApi> response = restTemplate.exchange(URI.create("http://8.8.8.8:88" + requestUrl), HttpMethod.GET, entity, new ParameterizedTypeReference<RefererApi>() {
            });
            if (response.getStatusCode() == HttpStatus.OK) {
                RefererApi body = response.getBody();
                if (body == null) {
                    log.error("referer api result is null");
                    return null;
                }
                if (null != body.getCode()) {
                    log.error("referer api result error error code {}, request id {} , message {}", body.getCode(), body.getRequestId(), body.getMessage());
                    return null;
                }
                String wd = body.getWd();
                return URLDecoder.decode(wd, StandardCharsets.UTF_8.toString());
            }
            log.error("request BaiDu referer api error status {}  message {}", response.getStatusCode(), response.toString());
        } catch (Exception e) {
            log.error("error has happens", e);
        }
        return null;
    }

}
