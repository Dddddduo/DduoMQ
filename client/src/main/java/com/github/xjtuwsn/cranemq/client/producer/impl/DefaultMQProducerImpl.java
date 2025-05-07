package com.github.xjtuwsn.cranemq.client.producer.impl;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.StrUtil;
import com.github.xjtuwsn.cranemq.client.remote.WrapperFutureCommand;
import com.github.xjtuwsn.cranemq.client.hook.SendCallback;
import com.github.xjtuwsn.cranemq.client.producer.MQSelector;
import com.github.xjtuwsn.cranemq.client.producer.balance.LoadBalanceStrategy;
import com.github.xjtuwsn.cranemq.client.producer.result.SendResult;
import com.github.xjtuwsn.cranemq.common.command.*;
import com.github.xjtuwsn.cranemq.common.command.payloads.req.MQBachProduceRequest;
import com.github.xjtuwsn.cranemq.common.remote.enums.RegistryType;
import com.github.xjtuwsn.cranemq.common.utils.TopicUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.xjtuwsn.cranemq.common.remote.RemoteHook;
import com.github.xjtuwsn.cranemq.client.producer.DefaultMQProducer;
import com.github.xjtuwsn.cranemq.client.producer.MQProducerInner;
import com.github.xjtuwsn.cranemq.client.remote.ClientFactory;
import com.github.xjtuwsn.cranemq.client.remote.ClientInstance;
import com.github.xjtuwsn.cranemq.common.command.payloads.req.MQProduceRequest;
import com.github.xjtuwsn.cranemq.common.command.types.RequestType;
import com.github.xjtuwsn.cranemq.common.command.types.RpcType;
import com.github.xjtuwsn.cranemq.common.entity.Message;
import com.github.xjtuwsn.cranemq.common.exception.CraneClientException;
import com.github.xjtuwsn.cranemq.common.remote.RemoteAddress;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @project:dduomq
 * @file:DefaultMQProducerImpl
 * @author:dduo
 * @create:2023/09/27-14:38
 * 消息生产者实现，实现了 MQProducerInner 接口，负责消息的发送和相关操作
 */
public class DefaultMQProducerImpl implements MQProducerInner {

    // 日志记录器，用于记录日志信息
    private static final Logger log = LoggerFactory.getLogger(DefaultMQProducer.class);

    // 关联的 DefaultMQProducer 对象
    private DefaultMQProducer defaultMQProducer;

    // 远程钩子，可用于在远程操作前后执行自定义逻辑
    private RemoteHook hook;

    // 客户端实例，用于与远程服务进行通信
    private ClientInstance clientInstance;

    // 远程地址，代表服务端地址
    private RemoteAddress address;

    // 注册中心地址
    private String registryAddress;

    // 客户端 ID，用于标识客户端
    private String clientID;

    // 生产者在客户端实例中的唯一标识
    private String id;

    // 负载均衡策略，用于选择合适的服务节点
    private LoadBalanceStrategy loadBalanceStrategy;

    // 注册中心类型，如 Default、Zookeeper、Nacos 等
    private RegistryType registryType;

    // 存储生产者发送消息所使用的主题集合，支持并发操作
    private ConcurrentHashSet<String> topicSet = new ConcurrentHashSet<>();

    /**
     * 生产者状态，使用原子整数保证线程安全
     * 0: created  已创建
     * 1: started  已启动
     * 2: failed   启动失败
     */
    private volatile AtomicInteger state;

    /**
     * 构造函数，初始化生产者实例
     * @param defaultMQProducer 关联的 DefaultMQProducer 对象
     * @param hook 远程钩子
     * @param registryAddress 注册中心地址
     */
    public DefaultMQProducerImpl(DefaultMQProducer defaultMQProducer,
                                 RemoteHook hook, String registryAddress) {
        this(defaultMQProducer, hook, registryAddress, null);
    }

    /**
     * 构造函数，初始化生产者实例，并指定负载均衡策略
     * @param defaultMQProducer 关联的 DefaultMQProducer 对象
     * @param hook 远程钩子
     * @param registryAddress 注册中心地址
     * @param loadBalanceStrategy 负载均衡策略
     */
    public DefaultMQProducerImpl(DefaultMQProducer defaultMQProducer,
                                 RemoteHook hook, String registryAddress,
                                 LoadBalanceStrategy loadBalanceStrategy) {
        this.defaultMQProducer = defaultMQProducer;
        this.hook = hook;
        this.registryAddress = registryAddress;

        this.state = new AtomicInteger(0);
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    /**
     * 启动生产者
     * @throws CraneClientException 当生产者已经启动或启动失败时抛出异常
     */
    public void start() throws CraneClientException {
        if (this.state.get() != 0) {
            log.error("DefaultMQProducerImpl has already been started or faied!");
            throw new CraneClientException("DefaultMQProducerImpl has already been started or faied!");
        }
        // 检查配置信息是否合法
        this.checkConfig();
        // 获取代理地址
        this.address = this.defaultMQProducer.getBrokerAddress();
        // 构建客户端 ID
        this.clientID = TopicUtil.buildClientID("producer");
        // 获取或创建客户端实例
        this.clientInstance = ClientFactory.newInstance().getOrCreate(this.clientID, this.hook);
        if (this.loadBalanceStrategy != null) {
            // 设置负载均衡策略
            this.clientInstance.setLoadBalanceStrategy(this.loadBalanceStrategy);
        }
        // 设置注册中心类型
        this.clientInstance.setRegistryType(registryType);
        // 注册远程钩子
        this.clientInstance.registerHook(hook);
        // 注册生产者并获取唯一标识
        id = this.clientInstance.registerProducer(this);
        // 更新生产者状态为已启动
        this.state.set(1);
        // 启动客户端实例
        this.clientInstance.start();
    }

    /**
     * 同步发送消息
     * @param timeout 超时时间
     * @param isOneWay 是否为单向发送
     * @param selector 消息选择器，用于选择消息发送的目标
     * @param arg 选择器使用的参数
     * @param delay 延迟发送时间
     * @param messages 要发送的消息数组
     * @return 消息发送结果
     * @throws CraneClientException 当消息为空或创建请求失败时抛出异常
     */
    public SendResult sendSync(long timeout, boolean isOneWay, MQSelector selector, Object arg, long delay, Message... messages)
            throws CraneClientException {
        if (messages == null || messages.length == 0) {
            throw new CraneClientException("Message cannot be empty!");
        }
        // 构建请求
        WrapperFutureCommand wrappered = buildRequest(RpcType.SYNC, null, timeout, delay, messages);
        if (selector != null) {
            // 设置消息选择器和参数
            wrappered.setSelector(selector);
            wrappered.setArg(arg);
        }
        // 获取请求的关联 ID
        String correlationID = wrappered.getFutureCommand().getRequest().getHeader().getCorrelationId();
        if (isOneWay) {
            // 如果是单向发送，设置请求的 RPC 类型为单向
            wrappered.getFutureCommand().getRequest().getHeader().setRpcType(RpcType.ONE_WAY);
        }
        if (wrappered == null) {
            throw new CraneClientException("Create Request error!");
        }
        // 获取 FutureCommand 对象
        FutureCommand futureCommand = wrappered.getFutureCommand();

        // 同步发送消息并返回结果
        return this.clientInstance.sendMessageSync(wrappered, isOneWay);
    }

    /**
     * 同步发送消息的重载方法，不使用选择器和延迟发送
     * @param timeout 超时时间
     * @param isOneWay 是否为单向发送
     * @param messages 要发送的消息数组
     * @return 消息发送结果
     * @throws CraneClientException 当消息为空或创建请求失败时抛出异常
     */
    public SendResult sendSync(long timeout, boolean isOneWay, Message... messages)
            throws CraneClientException {
        return this.sendSync(timeout, isOneWay, null, null, 0, messages);
    }

    /**
     * 异步发送消息
     * @param callback 发送回调，用于处理消息发送结果
     * @param timeout 超时时间
     * @param messages 要发送的消息数组
     */
    public void sendAsync(SendCallback callback, long timeout, Message... messages) {
        if (messages == null || messages.length == 0) {
            throw new CraneClientException("Message cannot be empty!");
        }
        // 构建请求
        WrapperFutureCommand remoteCommand = buildRequest(RpcType.ASYNC, callback, timeout, 0, messages);
        if (remoteCommand == null) {
            throw new CraneClientException("Create Request error!");
        }

        // 异步发送消息
        this.clientInstance.sendMessageAsync(remoteCommand);
    }

    /**
     * 构建请求
     * @param rpcType RPC 类型，如同步、异步、单向等
     * @param callback 发送回调
     * @param timeout 超时时间
     * @param delay 延迟发送时间
     * @param messages 要发送的消息数组
     * @return 包装后的 FutureCommand 对象
     * @throws CraneClientException 当主题为空或主题名称无效时抛出异常
     */
    private WrapperFutureCommand buildRequest(RpcType rpcType, SendCallback callback, long timeout, long delay, Message... messages) {
        // 获取第一个消息的主题
        String topic = messages[0].getTopic();
        if (StrUtil.isEmpty(topic)) {
            throw new CraneClientException("Topic cannot be null");
        }
        if (!TopicUtil.checkTopic(topic)) {
            throw new CraneClientException("Topic name is invalid!");
        }
        // 将主题添加到主题集合中
        this.topicSet.add(topic);
        // 生成唯一的关联 ID
        String correlationID = TopicUtil.generateUniqueID();
        // 创建请求头
        Header header = new Header(RequestType.MESSAGE_PRODUCE_REQUEST, rpcType, correlationID);
        if (messages.length > 1) {
            // 如果消息数量大于 1，设置请求类型为批量消息生产请求
            header.setCommandType(RequestType.MESSAGE_BATCH_PRODUCE_REAUEST);
        }
        PayLoad payLoad = null;
        if (messages.length == 1) {
            if (delay > 0) {
                // 如果有延迟发送时间，设置请求类型为延迟消息生产请求
                header.setCommandType(RequestType.DELAY_MESSAGE_PRODUCE_REQUEST);
                payLoad = new MQProduceRequest(messages[0], delay);
            } else {
                payLoad = new MQProduceRequest(messages[0]);
            }
        } else {
            payLoad = new MQBachProduceRequest(Arrays.asList(messages));
        }
        // 创建远程命令
        RemoteCommand remoteCommand = new RemoteCommand(header, payLoad);
        // 创建 FutureCommand 对象
        FutureCommand futureCommand = new FutureCommand();
        futureCommand.setRequest(remoteCommand);
        // 创建包装后的 FutureCommand 对象
        WrapperFutureCommand wrappered = new WrapperFutureCommand(futureCommand,
                this.defaultMQProducer.getMaxRetryTime(),
                timeout, callback, messages[0].getTopic());
        return wrappered;
    }

    /**
     * 关闭生产者
     * @throws CraneClientException 当注销生产者失败时抛出异常
     */
    public void close() throws CraneClientException {
        // 注销生产者
        this.clientInstance.unregisterProducer(id);
    }

    /**
     * 检查配置信息是否合法
     * @throws CraneClientException 当主题、组名、注册地址为空或最大重试次数为负数时抛出异常
     */
    private void checkConfig() throws CraneClientException {
        if (StrUtil.isEmpty(this.defaultMQProducer.getTopic())) {
            throw new CraneClientException("Topic name cannot be null");
        }
        if (StrUtil.isEmpty(this.defaultMQProducer.getGroup())) {
            throw new CraneClientException("Group name cannot be null");
        }
        if (StrUtil.isEmpty(this.defaultMQProducer.getRegistryAddr())) {
            throw new CraneClientException("Brokder address cannot be null");
        }
        if (this.defaultMQProducer.getMaxRetryTime() < 0) {
            throw new CraneClientException("Max Retry Time cannot be negtive");
        }
    }

    /**
     * 根据关联 ID 获取包装后的 FutureCommand 对象
     * @param correlationID 关联 ID
     * @return 包装后的 FutureCommand 对象
     */
    public WrapperFutureCommand getWrapperFuture(String correlationID) {
        return this.clientInstance.getWrapperFuture(correlationID);
    }

    /**
     * 根据关联 ID 移除包装后的 FutureCommand 对象
     * @param correlationID 关联 ID
     */
    public void removeWrapperFuture(String correlationID) {
        this.clientInstance.removeWrapperFuture(correlationID);
    }

    /**
     * 异步发送包装后的 FutureCommand 对象
     * @param wrapperFutureCommand 包装后的 FutureCommand 对象
     */
    public void asyncSend(WrapperFutureCommand wrapperFutureCommand) {
        this.clientInstance.sendMessageAsync(wrapperFutureCommand);
    }

    /**
     * 获取远程地址
     * @return 远程地址
     */
    public RemoteAddress getAddress() {
        return address;
    }

    /**
     * 设置远程地址
     * @param address 远程地址
     */
    public void setAddress(RemoteAddress address) {
        this.address = address;
    }

    /**
     * 获取代理地址列表，当前实现返回 null，需后续完善
     * @return 代理地址列表
     */
    @Override
    public List<String> getBrokerAddress() {
        return null;
    }

    /**
     * 获取或创建主题，当前实现返回 null，需后续完善
     * @param queueNumber 队列数量
     * @return 主题名称
     */
    @Override
    public String fetechOrCreateTopic(int queueNumber) {
        return null;
    }

    /**
     * 获取生产者使用的主题集合
     * @return 主题集合
     */
    public Set<String> getTopics() {
        return this.topicSet;
    }

    /**
     * 获取分割后的注册中心地址数组
     * @return 注册中心地址数组
     */
    public String[] getRegisteryAddress() {
        return registryAddress.split(";");
    }

    /**
     * 获取原始的注册中心地址
     * @return 注册中心地址
     */
    public String getOriginRegisteryAddress() {
        return registryAddress;
    }

    /**
     * 设置注册中心地址
     * @param registryAddress 注册中心地址
     */
    public void setRegistryAddress(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    /**
     * 设置负载均衡策略
     * @param loadBalanceStrategy 负载均衡策略
     */
    public void setLoadBalanceStrategy(LoadBalanceStrategy loadBalanceStrategy) {
        this.loadBalanceStrategy = loadBalanceStrategy;
    }

    /**
     * 获取关联的 DefaultMQProducer 对象
     * @return DefaultMQProducer 对象
     */
    public DefaultMQProducer getDefaultMQProducer() {
        return defaultMQProducer;
    }

    /**
     * 获取注册中心类型
     * @return 注册中心类型
     */
    public RegistryType getRegistryType() {
        return registryType;
    }

    /**
     * 设置注册中心类型
     * @param registryType 注册中心类型
     */
    public void setRegistryType(RegistryType registryType) {
        this.registryType = registryType;
    }
}