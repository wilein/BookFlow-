package com.book.bookflow.common.pay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.book.bookflow.entity.Book;
import com.book.bookflow.entity.BookOrder;
import com.book.bookflow.entity.User;
import com.book.bookflow.exception.CustomerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WechatPayClient {

    private static final String WECHAT_PREPAY_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";
    private static final String WECHAT_PREPAY_PATH = "/v3/pay/transactions/jsapi";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${pay.mode:disabled}")
    private String payMode;

    @Value("${weixin.appid:}")
    private String appid;

    @Value("${pay.wechat.mch-id:}")
    private String mchId;

    @Value("${pay.wechat.mch-serial-no:}")
    private String mchSerialNo;

    @Value("${pay.wechat.api-v3-key:}")
    private String apiV3Key;

    @Value("${pay.wechat.private-key-path:}")
    private String privateKeyPath;

    @Value("${pay.wechat.platform-cert-path:}")
    private String platformCertPath;

    @Value("${pay.wechat.notify-url:}")
    private String notifyUrl;

    public boolean isMockMode() {
        return "mock".equalsIgnoreCase(text(payMode));
    }

    public Map<String, Object> createPrepay(BookOrder order, User buyer, Book book) {
        if (isMockMode()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("mock", true);
            result.put("orderId", order.getId());
            result.put("orderNo", order.getOrderNo());
            result.put("payMode", "mock");
            return result;
        }

        ensureWechatConfigured();
        String openid = text(buyer == null ? "" : buyer.getOpenid());
        if (openid.isBlank()) {
            throw new CustomerException("400", "当前用户缺少微信 openid，无法发起支付");
        }

        String requestBody = buildPrepayBody(order, openid, book);
        JSONObject response = requestWechatPrepay(requestBody);
        String prepayId = response.getString("prepay_id");
        if (prepayId == null || prepayId.isBlank()) {
            throw new CustomerException("500", "微信预支付单创建失败");
        }

        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = nonce();
        String packageValue = "prepay_id=" + prepayId;
        String paySign = rsaSign(appid + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageValue + "\n");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mock", false);
        result.put("provider", "wxpay");
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("timeStamp", timeStamp);
        result.put("nonceStr", nonceStr);
        result.put("package", packageValue);
        result.put("signType", "RSA");
        result.put("paySign", paySign);
        return result;
    }

    public JSONObject parseNotify(String serial, String timestamp, String nonce, String signature, String body) {
        ensureWechatConfigured();
        verifyNotifySignature(serial, timestamp, nonce, signature, body);
        JSONObject payload = JSON.parseObject(body);
        JSONObject resource = payload.getJSONObject("resource");
        if (resource == null) {
            throw new CustomerException("400", "微信支付回调缺少资源数据");
        }
        String plainText = decryptResource(
            resource.getString("associated_data"),
            resource.getString("nonce"),
            resource.getString("ciphertext")
        );
        return JSON.parseObject(plainText);
    }

    private void ensureWechatConfigured() {
        if (!"wechat".equalsIgnoreCase(text(payMode))) {
            throw new CustomerException("400", "支付暂未配置");
        }
        if (appid.isBlank() || mchId.isBlank() || mchSerialNo.isBlank()
            || apiV3Key.isBlank() || privateKeyPath.isBlank() || notifyUrl.isBlank()) {
            throw new CustomerException("400", "支付暂未配置");
        }
    }

    private String buildPrepayBody(BookOrder order, String openid, Book book) {
        JSONObject body = new JSONObject(true);
        body.put("appid", appid);
        body.put("mchid", mchId);
        body.put("description", limit("BookFlow-" + text(book == null ? "" : book.getTitle()), 120));
        body.put("out_trade_no", order.getOrderNo());
        body.put("notify_url", notifyUrl);

        JSONObject amount = new JSONObject(true);
        amount.put("total", amountToFen(order.getTotalAmount()));
        amount.put("currency", "CNY");
        body.put("amount", amount);

        JSONObject payer = new JSONObject(true);
        payer.put("openid", openid);
        body.put("payer", payer);
        return body.toJSONString();
    }

    private JSONObject requestWechatPrepay(String requestBody) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = nonce();
        String message = "POST\n" + WECHAT_PREPAY_PATH + "\n" + timestamp + "\n" + nonceStr + "\n" + requestBody + "\n";
        String authorization = "WECHATPAY2-SHA256-RSA2048 "
            + "mchid=\"" + mchId + "\","
            + "nonce_str=\"" + nonceStr + "\","
            + "timestamp=\"" + timestamp + "\","
            + "serial_no=\"" + mchSerialNo + "\","
            + "signature=\"" + rsaSign(message) + "\"";

        HttpRequest request = HttpRequest.newBuilder(URI.create(WECHAT_PREPAY_URL))
            .header("Authorization", authorization)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CustomerException("500", "微信预支付单创建失败：" + response.body());
            }
            return JSON.parseObject(response.body());
        } catch (IOException exception) {
            throw new CustomerException("500", "微信支付网络请求失败");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomerException("500", "微信支付请求被中断");
        }
    }

    private void verifyNotifySignature(String serial, String timestamp, String nonce, String signature, String body) {
        if (text(serial).isBlank() || text(timestamp).isBlank() || text(nonce).isBlank() || text(signature).isBlank()) {
            throw new CustomerException("400", "微信支付回调验签参数缺失");
        }
        if (platformCertPath == null || platformCertPath.isBlank()) {
            throw new CustomerException("400", "支付回调验签证书未配置");
        }
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(Files.newInputStream(Path.of(platformCertPath)));
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(certificate.getPublicKey());
            verifier.update((timestamp + "\n" + nonce + "\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(Base64.getDecoder().decode(signature))) {
                throw new CustomerException("400", "微信支付回调验签失败");
            }
        } catch (CustomerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomerException("400", "微信支付回调验签失败");
        }
    }

    private String decryptResource(String associatedData, String nonce, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (associatedData != null && !associatedData.isBlank()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new CustomerException("400", "微信支付回调解密失败");
        }
    }

    private String rsaSign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey());
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (CustomerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomerException("500", "微信支付签名失败");
        }
    }

    private PrivateKey loadPrivateKey() {
        try {
            String pem = Files.readString(Path.of(privateKeyPath), StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception exception) {
            throw new CustomerException("500", "微信支付私钥读取失败");
        }
    }

    private int amountToFen(BigDecimal amount) {
        return amount == null ? 0 : amount.multiply(BigDecimal.valueOf(100)).intValue();
    }

    public int expectedAmountFen(BookOrder order) {
        return amountToFen(order == null ? BigDecimal.ZERO : order.getTotalAmount());
    }

    public String getMchId() {
        return mchId;
    }

    public String getAppid() {
        return appid;
    }

    private String nonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String limit(String value, int maxLength) {
        String text = text(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
