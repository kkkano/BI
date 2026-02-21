//package com.yupi.springbootinit.manager;
//
//import com.yupi.springbootinit.common.ErrorCode;
//import com.yupi.springbootinit.exception.BusinessException;
//import com.yupi.yucongming.dev.client.YuCongMingClient;
//import com.yupi.yucongming.dev.common.BaseResponse;
//import com.yupi.yucongming.dev.model.DevChatRequest;
//import com.yupi.yucongming.dev.model.DevChatResponse;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//
///**
// * 用于对接 AI 平台
// */
//@Service
//public class AiManager {
//
//    @Resource
//    private YuCongMingClient yuCongMingClient;
//
//    /**
//     * AI 对话
//     *
//     * @param message
//     * @return
//     */
//    public String doChat(String message) {
//        // 第三步，构造请求参数
//        DevChatRequest devChatRequest = new DevChatRequest();
//        // 模型id，尾后加L，转成long类型
//        devChatRequest.setModelId(1716811466025619457L);
//        devChatRequest.setMessage(message);
//        // 第四步，获取响应结果
//        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
//        // 如果响应为null，就抛出系统异常，提示“AI 响应错误”
//        if (response == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误");
//        }
//        return response.getData().getContent();
//    }
//}
package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用于对接 AI 平台
 */
@Service
@Slf4j
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * 重试间隔（毫秒）
     */
    @Value("${bi.ai.retry-interval-ms:120000}")
    private long retryIntervalMs;

    /**
     * 最大重试次数（总尝试次数 = maxRetry）
     */
    @Value("${bi.ai.max-retry:10}")
    private int maxRetry;

    /**
     * AI 对话
     * 规则：120 秒一次重试，最多 10 次，全部失败才抛错
     *
     * @param modelId
     * @param message
     * @return
     */
    public String doChat(long modelId, String message) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                DevChatRequest devChatRequest = new DevChatRequest();
                devChatRequest.setModelId(modelId);
                devChatRequest.setMessage(message);
                BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);

                if (response != null && response.getData() != null && response.getData().getContent() != null) {
                    if (attempt > 1) {
                        log.info("AI 调用在第 {} 次重试后成功", attempt);
                    }
                    return response.getData().getContent();
                }

                lastException = new RuntimeException("AI 响应为空");
            } catch (Exception e) {
                lastException = e;
            }

            // 非最后一次，等待后重试
            if (attempt < maxRetry) {
                log.warn("AI 调用失败，第 {}/{} 次，{} 秒后重试", attempt, maxRetry, retryIntervalMs / 1000);
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 调用被中断");
                }
            }
        }

        log.error("AI 调用最终失败，已重试 {} 次", maxRetry, lastException);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 响应错误，重试后仍失败");
    }
}
