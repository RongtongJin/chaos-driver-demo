package io.openmessaging.chaos.driver.demo.imp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.BaseEncoding;
import io.openmessaging.chaos.common.Message;
import io.openmessaging.chaos.driver.demo.api.MQChaosDriverAdapter;

import io.openmessaging.chaos.driver.demo.imp.config.RMQClientConfig;
import io.openmessaging.chaos.driver.mq.ConsumerCallback;
import io.openmessaging.chaos.driver.mq.MQChaosProducer;
import io.openmessaging.chaos.driver.mq.MQChaosPushConsumer;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMQChaosDriver extends MQChaosDriverAdapter {

    private static final Random RANDOM = new Random();
    private RMQClientConfig rmqClientConfig;
    private DefaultMQAdminExt rmqAdmin;
    private static final Logger log = LoggerFactory.getLogger(RMQChaosDriver.class);
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void createTopic(String topic, int partitions) {
        this.rmqAdmin = new DefaultMQAdminExt();
        this.rmqAdmin.setNamesrvAddr(rmqClientConfig.namesrvAddr);
        this.rmqAdmin.setInstanceName("AdminInstance-" + getRandomString());
        try {
            this.rmqAdmin.start();
        } catch (MQClientException e) {
            log.error("Start the RocketMQ admin tool failed.");
        }

        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setOrder(false);
        topicConfig.setPerm(6);
        topicConfig.setReadQueueNums(partitions);
        topicConfig.setWriteQueueNums(partitions);
        topicConfig.setTopicName(topic);

        try {
            Set<String> brokerList = CommandUtil.fetchMasterAddrByClusterName(this.rmqAdmin, this.rmqClientConfig.clusterName);
            topicConfig.setReadQueueNums(Math.max(1, partitions / brokerList.size()));
            topicConfig.setWriteQueueNums(Math.max(1, partitions / brokerList.size()));

            for (String brokerAddr : brokerList) {
                this.rmqAdmin.createAndUpdateTopicConfig(brokerAddr, topicConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create topic [%s] to cluster [%s]", topic, this.rmqClientConfig.clusterName), e);
        }
    }

    @Override
    public MQChaosProducer createProducer(String topic) {
        DefaultMQProducer defaultMQProducer = new DefaultMQProducer("ProducerGroup_Chaos");
        defaultMQProducer.setNamesrvAddr(rmqClientConfig.namesrvAddr);
        defaultMQProducer.setInstanceName("ProducerInstance" + getRandomString());

        return new RMQChaosProducer(defaultMQProducer, topic);
    }

    @Override
    public MQChaosPushConsumer createPushConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer(subscriptionName);
        defaultMQPushConsumer.setNamesrvAddr(rmqClientConfig.namesrvAddr);
        defaultMQPushConsumer.setInstanceName("ConsumerInstance" + getRandomString());
        try {
            defaultMQPushConsumer.subscribe(topic, "*");
            defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt message : msgs) {
                    consumerCallback.messageReceived(new Message(message.getKeys(), message.getBody()));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
        } catch (MQClientException e) {
            log.error("Failed to create consumer instance.", e);
        }
        return new RMQChaosPushConsumer(defaultMQPushConsumer);
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.rmqClientConfig = readConfigForClient(configurationFile);
    }

    @Override
    public void shutdown() {
        rmqAdmin.shutdown();
    }

    private static RMQClientConfig readConfigForClient(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RMQClientConfig.class);
    }

    private static String getRandomString() {
        byte[] buffer = new byte[5];
        RANDOM.nextBytes(buffer);
        return BaseEncoding.base64Url().omitPadding().encode(buffer);
    }
}
