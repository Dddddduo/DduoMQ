package com.github.xjtuwsn.cranemq.client.processor;

import com.github.xjtuwsn.cranemq.client.remote.WrapperFutureCommand;
import com.github.xjtuwsn.cranemq.client.producer.result.SendResult;
import com.github.xjtuwsn.cranemq.client.producer.result.SendResultType;
import com.github.xjtuwsn.cranemq.client.remote.ClientInstance;
import com.github.xjtuwsn.cranemq.common.command.RemoteCommand;
import com.github.xjtuwsn.cranemq.common.command.types.ResponseCode;
import com.github.xjtuwsn.cranemq.common.command.types.RpcType;
import com.github.xjtuwsn.cranemq.common.exception.CraneClientException;
import com.github.xjtuwsn.cranemq.common.remote.processor.BaseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * @project:dduomq
 * @file:AbstractClientProcessor
 * @author:dduo
 * @create:2023/10/07 - 19:29
 *
 * 抽象类 AbstractClientProcessor 实现了 BaseProcessor 接口，
 * 用于处理客户端接收到的远程命令响应，并根据响应结果进行相应处理，包括重试机制和回调函数调用。
 */
public abstract class AbstractClientProcessor implements BaseProcessor {

    // 静态日志记录器，用于记录该类的日志信息
    private static final Logger log = LoggerFactory.getLogger(AbstractClientProcessor.class);

    // 客户端实例，代表当前客户端
    protected ClientInstance clientInstance;

    /**
     * 构造函数，初始化客户端实例
     * @param clientInstance 客户端实例
     */
    public AbstractClientProcessor(ClientInstance clientInstance) {
        this.clientInstance = clientInstance;
    }

    /**
     * 解析远程命令的响应，并根据响应结果决定是否进行重试
     * @param remoteCommand 远程命令，包含响应信息
     * @param asyncHookService 异步钩子服务，用于执行回调函数
     * @return 包装的未来命令对象
     */
    protected WrapperFutureCommand parseResponseWithRetry(RemoteCommand remoteCommand,
                                                          ExecutorService asyncHookService) {
        // 从远程命令头中获取 RPC 类型
        RpcType rpcType = remoteCommand.getHeader().getRpcType();
        // 从远程命令头中获取响应码
        int responseCode = remoteCommand.getHeader().getStatus();
        // 从远程命令头中获取关联 ID，用于标识请求
        String correlationID = remoteCommand.getHeader().getCorrelationId();
        // 通过关联 ID 从客户端实例中获取包装的未来命令对象
        WrapperFutureCommand wrappered = this.clientInstance.getWrapperFuture(correlationID);
        // 如果包装的未来命令对象为 null，说明请求在处理前已被移除
        if (wrappered == null) {
            log.warn("Request {} has been removed before", correlationID);
            return null;
        }
        // 如果响应码不是成功状态码
        if (responseCode != ResponseCode.SUCCESS) {
            // 记录请求出现错误的日志
            log.warn("Request {} has occured a mistake, reason is {}", correlationID, responseCode);
            // 如果包装的未来命令对象已经完成
            if (wrappered.isDone()) {
                // 从客户端实例中移除该请求
                this.clientInstance.removeWrapperFuture(correlationID);
                return wrappered;
            }
            // 如果不需要重试
            if (!wrappered.isNeedRetry()) {
                // 取消该请求
                wrappered.cancel();
                // 从客户端实例中移除该请求
                this.clientInstance.removeWrapperFuture(correlationID);
                // 如果存在回调函数
                if (wrappered.getCallback() != null) {
                    // 如果异步钩子服务不为 null
                    if (asyncHookService != null) {
                        // 在异步钩子服务中执行回调函数的 onFailure 方法
                        asyncHookService.execute(() -> {
                            wrappered.getCallback().onFailure(new CraneClientException("Retry time got max"));
                        });
                    } else {
                        // 直接执行回调函数的 onFailure 方法
                        wrappered.getCallback().onFailure(new CraneClientException("Retry time got max"));
                    }
                }
            } else { // 需要重试
                // 增加重试次数
                wrappered.increaseRetryTime();
                // 更新开始时间
                wrappered.setStartTime(System.currentTimeMillis());
                // 记录请求重试的日志
                log.info("Request {} retry because Failure response", correlationID);
                // 通过客户端实例异步发送该请求
                this.clientInstance.sendMessageAsync(wrappered);
            }

        } else { // 响应成功
            // 将响应设置到包装的未来命令对象中
            wrappered.setResponse(remoteCommand);
            // 从客户端实例中移除该请求
            this.clientInstance.removeWrapperFuture(correlationID);
            // 如果存在回调函数
            if (wrappered.getCallback() != null) {
                // 在异步钩子服务中执行回调函数的 onSuccess 方法
                asyncHookService.execute(() -> {
                    // 创建发送结果对象，状态为发送成功
                    SendResult result = new SendResult(SendResultType.SEND_OK, correlationID);
                    wrappered.getCallback().onSuccess(result);
                });
            }
            // 记录请求成功响应的日志
            log.info("Request {} get Success response", correlationID);
        }
        return wrappered;
    }

}