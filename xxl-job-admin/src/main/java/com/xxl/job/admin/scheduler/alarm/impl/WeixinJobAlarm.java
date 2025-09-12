package com.xxl.job.admin.scheduler.alarm.impl;

import com.xxl.job.admin.model.XxlJobInfo;
import com.xxl.job.admin.model.XxlJobLog;
import com.xxl.job.admin.scheduler.alarm.JobAlarm;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

/**
 * 基于企业微信WebHook的Job 告警
 */
@Slf4j
@Component
@ConfigurationProperties("job.alarm.qwx.robot")
public class WeixinJobAlarm implements JobAlarm {
    private static final Logger logger = LoggerFactory.getLogger(WeixinJobAlarm.class);
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        if (info != null && info.getAlarmEmail() != null && !info.getAlarmEmail().trim().isEmpty()) {
            final String receiver = info.getAlarmEmail();
            final String[] items = StringUtils.tokenizeToStringArray(receiver, ",");
            final String webhookUrl = Arrays.stream(items).filter(str -> str.startsWith("https://qyapi.weixin.qq.com")).findFirst().orElse(null);
            if (webhookUrl == null) {
                return true;
            }
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            final String content = """
                    Scheduler Alarm : Executor Handler =  %s
                    
                    JobId : %s
                    JobDesc : %s
                    JobLogId: %s
                    TriggerTime : %s
                    HandleCode : %s
                    
                    """.formatted(info.getExecutorHandler(),
                    info.getId(), info.getJobDesc(), jobLog.getId(),
                    jobLog.getTriggerTime() == null ? null : format.format(jobLog.getTriggerTime()),
                    jobLog.getHandleMsg());
            final String body = """
                      {
                        "msgtype":"text",
                        "text": {
                            "content": "%s",
                            "mentioned_list":["%s"]
                        },
                      }
                    """.formatted(content, info.getAuthor() == null ? "@all" : info.getAuthor());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            try {
                HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("QWX job alarm send result:  resp status = {} , body = {}", resp.statusCode(), resp.body());
            } catch (Exception e) {
                logger.error("QWX job alarm send failed , exception:", e);
                return false;
            }
        }
        return true;
    }

//    public static void main(String [] args){
//        WeixinJobAlarm weixinJobAlarm= new WeixinJobAlarm();
//        XxlJobInfo info = new XxlJobInfo();
//        info.setAuthor("wuming");
//        XxlJobLog jobLog = new XxlJobLog();
//        jobLog.setTriggerTime(new Date());
//        info.setAlarmEmail("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=");
//        weixinJobAlarm.doAlarm(info, jobLog);
//    }

}


