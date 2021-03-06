package com.haylion.haylionnetwork.http.api;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebSettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.haylion.haylionnetwork.http.converter.ConverterFactory;
import com.haylion.haylionnetwork.http.util.HttpsUtils;
import com.haylion.haylionnetwork.util.time.TimeCalibrationInterceptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

/**
 * Author:wangjianming
 * Time:2018/11/15 17:13
 * Description:ApiBox retrofit代理包装类
 */
public class ApiBox {
    private int CONNECT_TIME_OUT = 10 * 1000;//跟服务器连接超时时间
    public static int DELAY_TIME_SHOW_LOADING = 3000;//延时展示loading时间
    private int READ_TIME_OUT = 10 * 1000;    // 数据读取超时时间
    private int WRITE_TIME_OUT = 10 * 1000;   //数据写入超时时间
    private static final String CACHE_NAME = "cache";   //缓存目录名称
    /**
     * 异常
     */
    public static String FLAG_UNKNOWN = "1001";
    /**
     * 网络异常标志
     */
    public static String FLAG_NET_ERROR = "1002";
    /**
     * 网络异常标志
     */
    public static String FLAG_NET_TIME_OUT = "10021";
    /**
     * 解析异常
     */
    public static String FLAG_PARSE_ERROR = "1003";
    /**
     * 权限异常
     */
    public static String FLAG_PERMISSION_ERROR = "1004";
    /**
     * token过期，重新登录
     */
    public static String FLAG_TOKEN_EXPIRED = "406";
    /**
     * successCode
     */
    public static String FLAG_SUCCESS_CODE = "200";
    /**
     * Log 日志开关 发布版本设为false
     */
    private boolean DEBUG = false;

    public boolean isDEBUG() {
        return DEBUG;
    }

    /**
     * 单例 持有引用
     */
    private final Gson gson;
    private final OkHttpClient okHttpClient;

    public Application application;//应用上下文(需注入参数)
    private File cacheFile;//缓存路径
    private final InputStream[] inputStreams;

    private Map<String, Object> serviceMap;


    /**
     * 在访问时创建单例
     */
    private static class SingletonHolder {
        private static ApiBox INSTANCE;
    }

    /**
     * 获取单例
     */
    public static ApiBox getInstance() {
        return SingletonHolder.INSTANCE;
    }


    /**
     * 创建Api服务实例的方法。
     *
     * @param serviceClass 接口类
     * @param baseUrl      网络请求url
     * @param <T>
     * @return
     */
    public <T> T createService(Class<T> serviceClass, String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) {
            baseUrl = "";
        }
        Object serviceObj = serviceMap.get(serviceClass.getName() + baseUrl);
        if (serviceObj != null) {
            return (T) serviceObj;
        }
        Retrofit retrofit = new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(baseUrl)
                .addConverterFactory(ConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        T service = retrofit.create(serviceClass);
        serviceMap.put(serviceClass.getName() + baseUrl, service);
        return service;
    }

    /**
     * 构造方法
     *
     * @param builder
     */
    private ApiBox(Builder builder) {
        //1.设置应用上下文、debug参数
        DEBUG = builder.debug;
        this.application = builder.application;
        this.cacheFile = builder.cacheDir;
        this.inputStreams = builder.inputStreams;
        this.serviceMap = new HashMap<>();
        if (builder.connetTimeOut > 0) {
            this.CONNECT_TIME_OUT = builder.connetTimeOut;
        }
        if (builder.delayTimeShowLoading > 0) {
            this.DELAY_TIME_SHOW_LOADING = builder.delayTimeShowLoading;
        }
        if (builder.readTimeOut > 0) {
            this.READ_TIME_OUT = builder.readTimeOut;
        }
        if (builder.writeTimeOut > 0) {
            this.WRITE_TIME_OUT = builder.writeTimeOut;
        }
        if (builder.successCode > 0) {
            FLAG_SUCCESS_CODE = String.valueOf(builder.successCode);
        }
        if (builder.tokenExpiredCode > 0) {
            FLAG_TOKEN_EXPIRED = String.valueOf(builder.tokenExpiredCode);
        }
        if (builder.jsonParseExceptionCode > 0) {
            FLAG_PARSE_ERROR = String.valueOf(builder.jsonParseExceptionCode);
        }
        if (builder.netTimeOutExceptionCode > 0) {
            FLAG_NET_TIME_OUT = String.valueOf(builder.netTimeOutExceptionCode);
        }
        if (builder.permissionExceptionCode > 0) {
            FLAG_PERMISSION_ERROR = String.valueOf(builder.permissionExceptionCode);
        }
        if (builder.unknownExceptionCode > 0) {
            FLAG_UNKNOWN = String.valueOf(builder.unknownExceptionCode);
        }
        //2.gson
        gson = getReponseGson();

        //3.okhttp
        okHttpClient = getClient();
    }

    /**
     * 同步时间的拦截器
     *
     * @return
     */
    private Interceptor getTimeIntercepter() {
        return new TimeCalibrationInterceptor();
    }

    /**
     * 设置打印 log
     * 开发模式记录整个body，否则只记录基本信息如返回200，http协议版本等
     *
     * @return
     */
    private HttpLoggingInterceptor getLogInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        if (DEBUG) {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            interceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        }
        return interceptor;
    }


    /**
     * 创建okhttp客户端
     *
     * @return
     */
    private OkHttpClient getClient() {
        HostnameVerifier hostnameVerifier = HttpsUtils.getHostnameVerifier();
        // 如果使用到HTTPS，我们需要创建SSLSocketFactory，并设置到client
        SSLSocketFactory sslSocketFactory = HttpsUtils.getSslFactory();
        if (inputStreams != null) {
            sslSocketFactory = HttpsUtils.setCertificates(inputStreams);
        }
        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .addInterceptor(getLogInterceptor())
                .addInterceptor(getTimeIntercepter())
                .addInterceptor(getUserAgentInrercept())
                .connectTimeout(CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIME_OUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIME_OUT, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .sslSocketFactory(sslSocketFactory)
                .hostnameVerifier(hostnameVerifier);
        return okHttpClientBuilder.build();
    }

    /**
     * 添加user-Agent
     *
     * @return
     */
    private Interceptor getUserAgentInrercept() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request()
                        .newBuilder()
                        .removeHeader("User-Agent")//移除旧的
                        .addHeader("User-Agent", getUserAgent(application))//添加真正的头部
                        .build();
                return chain.proceed(request);
            }
        };
    }

    /**
     * 获取user-agent
     *
     * @param context
     * @return
     */
    private static String getUserAgent(Context context) {
        String userAgent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                userAgent = WebSettings.getDefaultUserAgent(context);
            } catch (Exception e) {
                userAgent = System.getProperty("http.agent");
            }
        } else {
            userAgent = System.getProperty("http.agent");
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0, length = userAgent.length(); i < length; i++) {
            char c = userAgent.charAt(i);
            if (c <= '\u001f' || c >= '\u007f') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 创建配置Gson对象
     *
     * @return
     */
    private Gson getReponseGson() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson;
    }


    public static final class Builder {
        private Application application;//应用上下文，需要注入参数
        private File cacheDir;//缓存路径
        private boolean debug;
        private int connetTimeOut;
        private int delayTimeShowLoading;
        private int readTimeOut;
        private int writeTimeOut;
        private InputStream[] inputStreams;
        /**
         * token过期代码，需要重新标识
         */
        private int tokenExpiredCode;
        /**
         * josn解析异常标识
         */
        private int jsonParseExceptionCode;
        /**
         * 网络连接超时异常标识
         */
        private int netTimeOutExceptionCode;
        /**
         * 权限异常标识
         */
        private int permissionExceptionCode;
        /**
         * 未标记异常标识
         */
        private int unknownExceptionCode;
        /**
         * 访问成功标识
         */
        private int successCode;

        public Builder application(Application application) {
            this.application = application;
            this.cacheDir = new File(application.getCacheDir(), CACHE_NAME);
            return this;
        }

        /**
         * 是否是debug模式
         *
         * @param debug
         * @return
         */
        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        /**
         * 连接超时时间
         *
         * @param connetTime
         * @return
         */
        public Builder connetTimeOut(int connetTime) {
            this.connetTimeOut = connetTime;
            return this;
        }

        /**
         * 设置延迟多久显示loading动画
         *
         * @param delayTimeShowLoading
         * @return
         */
        public Builder delayTimeShowLoading(int delayTimeShowLoading) {
            this.delayTimeShowLoading = delayTimeShowLoading;
            return this;
        }

        /**
         * 读取超时时间
         *
         * @param readTimeOut
         * @return
         */
        public Builder readTimeOut(int readTimeOut) {
            this.readTimeOut = readTimeOut;
            return this;
        }

        /**
         * 写入超时时间
         *
         * @param writeTimeOut
         * @return
         */
        public Builder writeTimeOut(int writeTimeOut) {
            this.writeTimeOut = writeTimeOut;
            return this;
        }

        public Builder inputStreams(InputStream[] inputStreams) {
            this.inputStreams = inputStreams;
            return this;
        }

        /**
         * token错误码
         *
         * @param tokenExpiredCode
         * @return
         */
        public Builder tokenExpiredCode(int tokenExpiredCode) {
            this.tokenExpiredCode = tokenExpiredCode;
            return this;
        }

        /**
         * 解析gson异常码
         *
         * @param jsonParseExceptionCode
         * @return
         */
        public Builder jsonParseExceptionCode(int jsonParseExceptionCode) {
            this.jsonParseExceptionCode = jsonParseExceptionCode;
            return this;
        }

        /**
         * 网络请求超时码
         *
         * @param netTimeOutExceptionCode
         * @return
         */
        public Builder netTimeOutExceptionCode(int netTimeOutExceptionCode) {
            this.netTimeOutExceptionCode = netTimeOutExceptionCode;
            return this;
        }

        /**
         * 权限异常码
         *
         * @param permissionExceptionCode
         * @return
         */
        public Builder permissionExceptionCode(int permissionExceptionCode) {
            this.permissionExceptionCode = permissionExceptionCode;
            return this;
        }

        /**
         * 位置异常码
         *
         * @param unknownExceptionCode
         * @return
         */
        public Builder unknownExceptionCode(int unknownExceptionCode) {
            this.unknownExceptionCode = unknownExceptionCode;
            return this;
        }

        /**
         * 网络请求成功码
         *
         * @param successCode
         * @return
         */
        public Builder successCode(int successCode) {
            this.successCode = successCode;
            return this;
        }


        public ApiBox build() {
            if (SingletonHolder.INSTANCE == null) {
                ApiBox apiBox = new ApiBox(this);
                SingletonHolder.INSTANCE = apiBox;
            } else {
                SingletonHolder.INSTANCE.application = this.application;
                SingletonHolder.INSTANCE.DEBUG = this.debug;
            }
            return SingletonHolder.INSTANCE;
        }

    }

}
