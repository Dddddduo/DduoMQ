package com.github.xjtuwsn.cranemq.client.processor;

import com.github.xjtuwsn.cranemq.client.remote.ClientInstance;
import com.github.xjtuwsn.cranemq.common.remote.RemoteHook;
import com.github.xjtuwsn.cranemq.client.remote.WrapperFutureCommand;
import com.github.xjtuwsn.cranemq.common.command.RemoteCommand;
import com.github.xjtuwsn.cranemq.common.command.types.RpcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * @project:dduomq
 * @file:PruducerProcessor
 * @author:dduo
 * @create:2023/09/28-11:14
 *
 * PruducerProcessor类继承自AbstractClientProcessor，
 * 用于处理生产者相关的远程命令响应，如消息生产响应和创建主题响应。
 */
public class PruducerProcessor extends AbstractClientProcessor {
    // 静态日志记录器，用于记录该类的日志信息
    private static final Logger log = LoggerFactory.getLogger(PruducerProcessor.class);

    /**
     * 构造函数，初始化客户端实例
     * @param clientInstance 客户端实例
     */
    public PruducerProcessor(ClientInstance clientInstance) {
        super(clientInstance);
    }

    /**
     * 处理消息生产响应的远程命令
     * @param remoteCommand 远程命令，包含消息生产响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     * @param hook 远程钩子，用于在消息处理后执行额外操作
     */
    @Override
    public void processMessageProduceResopnse(RemoteCommand remoteCommand,
                                              ExecutorService asyncHookService,
                                              RemoteHook hook) {
        // 解析远程命令响应并根据结果决定是否重试
        this.parseResponseWithRetry(remoteCommand, asyncHookService);
        // 如果存在远程钩子
        if (hook != null) {
            // 如果异步钩子服务不为 null
            if (asyncHookService != null) {
                // 在异步钩子服务中执行远程钩子的 afterMessage 方法
                asyncHookService.execute(hook::afterMessage);
            } else {
                // 直接执行远程钩子的 afterMessage 方法
                hook.afterMessage();
            }
        }
    }

    /**
     * 处理创建主题响应的远程命令
     * @param remoteCommand 远程命令，包含创建主题响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     */
    @Override
    public void processCreateTopicResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService) {
        // 从远程命令头中获取 RPC 类型
        RpcType rpcType = remoteCommand.getHeader().getRpcType();
        // 从远程命令头中获取响应码
        int responseCode = remoteCommand.getHeader().getStatus();
        // 从远程命令头中获取关联 ID，用于标识请求
        String correlationID = remoteCommand.getHeader().getCorrelationId();
        // 通过关联 ID 从客户端实例中获取包装的未来命令对象
        WrapperFutureCommand wrappered = this.clientInstance.getWrapperFuture(correlationID);
        // 如果包装的未来命令对象为 null，说明创建主题请求被错误删除
        if (wrappered == null) {
            log.error("Create topic request has been deleted wrongly");
            return;
        }
        // 如果 RPC 类型为同步
        if (rpcType == RpcType.SYNC) {
            // 将远程命令响应设置到包装的未来命令对象中
            wrappered.setResponse(remoteCommand);
        }
    }
}