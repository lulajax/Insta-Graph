package com.lulajax.instagraph.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class BaseApiResponse {

    private int code;
    @JsonProperty("request_id")
    private String requestId;
    private String message;
    @JsonProperty("message_zh")
    private String messageZh;
    private String support;
    private String time;
    @JsonProperty("time_stamp")
    private long timeStamp;
    @JsonProperty("time_zone")
    private String timeZone;
    private String docs;
    @JsonProperty("cache_message")
    private String cacheMessage;
    @JsonProperty("cache_message_zh")
    private String cacheMessageZh;
    @JsonProperty("cache_url")
    private String cacheUrl;
    private String router;
    private Map<String, Object> params;
}
