package io.openmessaging.chaos.driver.demo.imp;

import io.openmessaging.chaos.common.InvokeResult;
import io.openmessaging.chaos.driver.demo.api.MQChaosProducerAdapter;
import java.util.List;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMQChaosProducer extends MQChaosProducerAdapter {

    private static final Logger log = LoggerFactory.getLogger(RMQChaosProducer.class);
    private final DefaultMQProducer defaultMQProducer;
    private String chaosTopic;

    public RMQChaosProducer(final DefaultMQProducer defaultMQProducer, String chaosTopic) {
        this.defaultMQProducer = defaultMQProducer;
        this.chaosTopic = chaosTopic;
    }

    @Override public InvokeResult enqueue(byte[] payload) {
        Message message = new Message(chaosTopic, payload);
        try {
            defaultMQProducer.send(message);
        } catch (RemotingException e) {
            if (e instanceof RemotingConnectException || e instanceof RemotingSendRequestException) {
                log.warn("Enqueue fail", e);
                return InvokeResult.FAILURE;
            } else {
                log.warn("Enqueue unknown", e);
                return InvokeResult.UNKNOWN;
            }
        } catch (IllegalStateException | MQClientException | InterruptedException | MQBrokerException e) {
            log.warn("Enqueue fail", e);
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            log.warn("Enqueue unknown", e);
            return InvokeResult.UNKNOWN;
        }
        return InvokeResult.SUCCESS;
    }

    @Override public InvokeResult enqueue(String shardingKey, byte[] payload) {
        Message message = new Message(chaosTopic, payload);
        message.setKeys(shardingKey);
        try {
            defaultMQProducer.send(message, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    String key = (String) arg;
                    int index = Math.abs(key.hashCode()) % mqs.size();
                    return mqs.get(index);
                }
            }, shardingKey);
        } catch (RemotingException e) {
            if (e instanceof RemotingConnectException || e instanceof RemotingSendRequestException) {
                log.warn("Enqueue fail", e);
                return InvokeResult.FAILURE;
            } else {
                log.warn("Enqueue unknown", e);
                return InvokeResult.UNKNOWN;
            }
        } catch (IllegalStateException | MQClientException | InterruptedException | MQBrokerException e) {
            log.warn("Enqueue fail", e);
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            log.warn("Enqueue unknown", e);
            return InvokeResult.UNKNOWN;
        }
        return InvokeResult.SUCCESS;
    }

    @Override public void start() {
        try {
            if (defaultMQProducer != null) {
                defaultMQProducer.start();
            }
        } catch (MQClientException e) {
            log.error("Failed to start the created producer instance.", e);
        }
    }

    @Override public void close() {
        if (defaultMQProducer != null) {
            defaultMQProducer.shutdown();
        }
    }
}
