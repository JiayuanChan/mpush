/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   ohun@live.cn (夜色)
 */

package com.mpush.cache.redis.mq;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mpush.api.spi.common.MQClient;
import com.mpush.api.spi.common.MQMessageReceiver;
import com.mpush.cache.redis.manager.RedisManager;
import com.mpush.tools.log.Logs;
import com.mpush.tools.thread.pool.ThreadPoolManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public final class ListenerDispatcher implements MQClient {

    private static ListenerDispatcher I;

    private final Map<String, List<MQMessageReceiver>> subscribes = Maps.newTreeMap();

    private final Executor executor = ThreadPoolManager.I.getRedisExecutor();

    private final Subscriber subscriber = new Subscriber();

    public static ListenerDispatcher I() {
        if (I == null) {
            synchronized (ListenerDispatcher.class) {
                if (I == null) {
                    I = new ListenerDispatcher();
                }
            }
        }
        return I;
    }

    private ListenerDispatcher() {
    }

    public void onMessage(final String channel, final String message) {
        List<MQMessageReceiver> listeners = subscribes.get(channel);
        if (listeners == null) {
            Logs.CACHE.info("cannot find listener:%s,%s", channel, message);
            return;
        }
        for (final MQMessageReceiver listener : listeners) {
            executor.execute(() -> listener.receive(channel, message));
        }
    }

    public void subscribe(String channel, MQMessageReceiver listener) {
        subscribes.computeIfAbsent(channel, k -> Lists.newArrayList()).add(listener);
        RedisManager.I.subscribe(subscriber, channel);
    }

    @Override
    public void publish(String topic, Object message) {
        RedisManager.I.publish(topic, message);
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }
}
