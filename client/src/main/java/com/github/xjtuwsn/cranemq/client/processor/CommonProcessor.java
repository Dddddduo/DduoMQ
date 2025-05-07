package com.github.xjtuwsn.cranemq.client.processor;

import com.github.xjtuwsn.cranemq.client.hook.InnerCallback;
import com.github.xjtuwsn.cranemq.client.remote.WrapperFutureCommand;
import com.github.xjtuwsn.cranemq.client.remote.ClientInstance;
import com.github.xjtuwsn.cranemq.common.command.RemoteCommand;
import com.github.xjtuwsn.cranemq.common.command.types.RpcType;
import com.github.xjtuwsn.cranemq.common.remote.processor.BaseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * @project:dduomq
 * @file:CommonProcessor
 * @author:dduo
 * @create:2023/10/07 - 16:37
 *
 * CommonProcessor 类实现了 BaseProcessor 接口，
 * 用于处理更新主题响应的远程命令，根据不同的 RPC 类型采取不同的处理方式。
 */
public class CommonProcessor implements BaseProcessor {
    // 静态日志记录器，用于记录该类的日志信息
    private static final Logger log = LoggerFactory.getLogger(CommonProcessor.class);

    // 客户端实例，代表当前客户端
    private ClientInstance clientInstance;

    /**
     * 构造函数，初始化客户端实例
     * @param clientInstance 客户端实例
     */
    public CommonProcessor(ClientInstance clientInstance) {
        this.clientInstance = clientInstance;
    }

    /**
     * 处理更新主题响应的远程命令
     * @param remoteCommand 远程命令，包含更新主题响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     */
    @Override
    public void processUpdateTopicResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService) {
        // 从远程命令头中获取 RPC 类型
        RpcType rpcType = remoteCommand.getHeader().getRpcType();
        // 从远程命令头中获取响应码
        int responseCode = remoteCommand.getHeader().getStatus();
        // 从远程命令头中获取关联 ID，用于标识请求
        String correlationID = remoteCommand.getHeader().getCorrelationId();
        // 通过关联 ID 从客户端实例中获取包装的未来命令对象
        WrapperFutureCommand wrappered = this.clientInstance.getWrapperFuture(correlationID);
        // 如果包装的未来命令对象为 null，说明更新主题请求被错误删除
        if (wrappered == null) {
            log.error("Update topic request has been deleted wrongly");
            return;
        }
        // 如果 RPC 类型为同步
        if (rpcType == RpcType.SYNC) {
            // 将远程命令响应设置到包装的未来命令对象中
            wrappered.setResponse(remoteCommand);
        } else {
            // 获取包装的未来命令对象中的内部回调函数
            InnerCallback innerCallback = (InnerCallback) wrappered.getCallback();
            // 如果异步钩子服务不为 null
            if (asyncHookService != null) {
                // 在异步钩子服务中执行内部回调函数的 onResponse 方法
                asyncHookService.execute(() -> {
                    log.info("Async execute update topic callback");
                    innerCallback.onResponse(remoteCommand);
                });
            } else {
                // 直接执行内部回调函数的 onResponse 方法
                innerCallback.onResponse(remoteCommand);
            }
        }
    }
}