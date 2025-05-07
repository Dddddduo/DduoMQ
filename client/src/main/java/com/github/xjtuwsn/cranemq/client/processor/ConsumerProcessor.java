package com.github.xjtuwsn.cranemq.client.processor;

import com.github.xjtuwsn.cranemq.client.remote.WrapperFutureCommand;
import com.github.xjtuwsn.cranemq.client.consumer.PullResult;
import com.github.xjtuwsn.cranemq.client.hook.InnerCallback;
import com.github.xjtuwsn.cranemq.client.hook.SendCallback;
import com.github.xjtuwsn.cranemq.client.remote.ClientInstance;
import com.github.xjtuwsn.cranemq.common.command.Header;
import com.github.xjtuwsn.cranemq.common.command.RemoteCommand;
import com.github.xjtuwsn.cranemq.common.command.payloads.resp.MQNotifyChangedResponse;
import com.github.xjtuwsn.cranemq.common.command.payloads.resp.MQPullMessageResponse;
import com.github.xjtuwsn.cranemq.common.command.payloads.resp.MQRebalanceQueryResponse;
import com.github.xjtuwsn.cranemq.common.entity.MessageQueue;
import com.github.xjtuwsn.cranemq.common.remote.RemoteHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @project:dduomq
 * @file:ConsumerProcessor
 * @author:dduo
 * @create:2023/10/07 - 19:45
 *
 * ConsumerProcessor 类继承自 AbstractClientProcessor，
 * 用于处理消费者相关的远程命令响应，包括简单拉取响应、通知变更响应、拉取响应、查询响应和锁定响应。
 */
public class ConsumerProcessor extends AbstractClientProcessor {
    // 静态日志记录器，用于记录该类的日志信息
    private static final Logger log = LoggerFactory.getLogger(ConsumerProcessor.class);

    /**
     * 构造函数，初始化客户端实例
     * @param clientInstance 客户端实例
     */
    public ConsumerProcessor(ClientInstance clientInstance) {
        super(clientInstance);
    }

    /**
     * 处理简单拉取响应的远程命令
     * @param remoteCommand 远程命令，包含简单拉取响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     * @param hook 远程钩子，用于在消息处理后执行额外操作
     */
    @Override
    public void processSimplePullResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService,
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
     * 处理通知变更响应的远程命令
     * @param remoteCommand 远程命令，包含通知变更响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     */
    @Override
    public void processNotifyChangedResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService) {
        // 从远程命令中获取通知变更响应的有效负载
        MQNotifyChangedResponse payLoad = (MQNotifyChangedResponse) remoteCommand.getPayLoad();
        // 启动一个新线程来更新消费者组
        new Thread(() -> {
            this.clientInstance.getRebalanceService().updateConsumerGroup(payLoad);
        }).start();
    }

    /**
     * 处理拉取响应的远程命令
     * @param remoteCommand 远程命令，包含拉取响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     */
    @Override
    public void processPullResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService) {
        // 从远程命令头中获取响应码
        int responseCode = remoteCommand.getHeader().getStatus();
        // 从远程命令头中获取关联 ID，用于标识请求
        String correlationID = remoteCommand.getHeader().getCorrelationId();
        // 从远程命令中获取拉取消息响应的有效负载
        MQPullMessageResponse mqPullMessageResponse = (MQPullMessageResponse) remoteCommand.getPayLoad();
        // 通过关联 ID 从客户端实例中获取包装的未来命令对象
        WrapperFutureCommand wrappered = this.clientInstance.getWrapperFuture(correlationID);
        // 创建拉取结果对象
        PullResult result = new PullResult();
        // 设置拉取结果对象的消息列表
        result.setMessages(mqPullMessageResponse.getMessages());
        // 设置拉取结果对象的获取结果类型
        result.setAcquireResultType(mqPullMessageResponse.getAcquireResultType());
        // 设置拉取结果对象的下一个偏移量
        result.setNextOffset(mqPullMessageResponse.getNextOffset());
        // 如果包装的未来命令对象存在拉取回调函数
        if (wrappered.getPullCallback() != null) {
            // 在异步钩子服务中执行拉取回调函数的 onSuccess 方法
            asyncHookService.execute(() -> {
                wrappered.getPullCallback().onSuccess(result);
            });
        }
    }

    /**
     * 处理查询响应的远程命令
     * @param remoteCommand 远程命令，包含查询响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     */
    @Override
    public void processQueryResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService) {
        // 从远程命令中获取头信息
        Header header = remoteCommand.getHeader();
        // 从远程命令中获取重新平衡查询响应的有效负载
        MQRebalanceQueryResponse mqRebalanceQueryResponse = (MQRebalanceQueryResponse) remoteCommand.getPayLoad();
        // 获取消费者组名称
        String group = mqRebalanceQueryResponse.getGroup();
        // 获取客户端集合
        Set<String> clients = mqRebalanceQueryResponse.getClients();
        // 获取所有偏移量的映射
        Map<MessageQueue, Long> allOffset = mqRebalanceQueryResponse.getOffsets();
        // 重置消费者组的消费者
        this.clientInstance.getRebalanceService().resetGroupConsumer(group, clients);
        // 重置本地偏移量
        this.clientInstance.getPushConsumerByGroup(group).getOffsetManager().resetLocalOffset(group, allOffset);
        // 将远程命令响应设置到包装的未来命令对象中
        this.clientInstance.getWrapperFuture(header.getCorrelationId()).setResponse(remoteCommand);
    }

    /**
     * 处理锁定响应的远程命令
     * @param remoteCommand 远程命令，包含锁定响应的信息
     * @param asyncHookService 异步钩子服务，用于异步执行回调函数
     */
    @Override
    public void processLockResponse(RemoteCommand remoteCommand, ExecutorService asyncHookService) {
        // 解析远程命令响应并根据结果决定是否重试
        WrapperFutureCommand wrappered = this.parseResponseWithRetry(remoteCommand, asyncHookService);
        // 获取包装的未来命令对象中的发送回调函数
        SendCallback callback = wrappered.getCallback();
        // 如果发送回调函数为 null（同步操作）
        if (callback == null) {
            // 此处可以添加同步操作的处理逻辑
        } else {
            // 将发送回调函数转换为内部回调函数
            InnerCallback innerCallback = (InnerCallback) callback;
            // 如果异步钩子服务不为 null
            if (asyncHookService != null) {
                // 在异步钩子服务中执行内部回调函数的 onResponse 方法
                asyncHookService.execute(() -> {
                    innerCallback.onResponse(remoteCommand);
                });
            } else {
                // 直接执行内部回调函数的 onResponse 方法
                innerCallback.onResponse(remoteCommand);
            }
        }
    }
}