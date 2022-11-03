/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.statistics;

import com.tangosol.internal.net.metrics.Meter;

import com.tangosol.internal.net.topic.impl.paged.management.PublishedMetrics;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.util.LongArray;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SimpleLongArray;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class holding statistics for a {@link com.tangosol.internal.net.topic.impl.paged.PagedTopic}.
 *
 * @author Jonathan Knight 2022.09.10
 * @since 23.03
 */
public class PagedTopicStatistics
        implements PublishedMetrics
    {
    /**
     * Create a {@link PagedTopicStatistics}.
     *
     * @param cChannel  the channel count.
     */
    public PagedTopicStatistics(int cChannel)
        {
        f_cChannel = cChannel;
        for (int i = 0; i < cChannel; i++)
            {
            m_aChannelStats.set(i, new PagedTopicChannelStatistics(i));
            }
        }

    /**
     * Return the number of channels in the topic.
     *
     * @return the number of channels in the topic
     */
    public int getChannelCount()
        {
        return f_cChannel;
        }

    /**
     * Return the {@link PagedTopicChannelStatistics statistics for a channel}.
     *
     * @param nChannel  the channel to obtain the statistics for
     *
     * @return the {@link PagedTopicChannelStatistics statistics for the channel}
     */
    public PagedTopicChannelStatistics getChannelStatistics(int nChannel)
        {
        PagedTopicChannelStatistics statistics = m_aChannelStats.get(nChannel);
        if (statistics == null)
            {
            f_lock.lock();
            try
                {
                statistics = new PagedTopicChannelStatistics(nChannel);
                m_aChannelStats.set(nChannel, statistics);
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return statistics;
        }

    /**
     * Update the message published metrics.
     *
     * @param nChannel  the number of messages published
     * @param cMessage  the channel the messages were published to
     * @param tail      the new tail position in the channel
     */
    public void onPublished(int nChannel, long cMessage, PagedPosition tail)
        {
        f_metricPublished.mark(cMessage);
        getChannelStatistics(nChannel).onPublished(cMessage, tail);
        }

    /**
     * Return the {@link SubscriberGroupStatistics statistics for a subscriber group}.
     *
     * @param sName  the name of the subscriber group
     *
     * @return the {@link SubscriberGroupStatistics statistics for a subscriber group}
     */
    public SubscriberGroupStatistics getSubscriberGroupStatistics(String sName)
        {
        SubscriberGroupStatistics statistics = f_mapSubscriberGroups.get(sName);
        if (statistics == null)
            {
            f_lock.lock();
            try
                {
                statistics = f_mapSubscriberGroups.computeIfAbsent(sName, s -> new SubscriberGroupStatistics(f_cChannel));
                }
            finally
                {
                f_lock.unlock();
                }
            }
        return statistics;
        }

    /**
     * Remove the {@link SubscriberGroupStatistics statistics for a subscriber group}.
     *
     * @param sName  the name of the subscriber group
     */
    public void removeSubscriberGroupStatistics(String sName)
        {
        f_lock.lock();
        try
            {
            f_mapSubscriberGroups.remove(sName);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Add a listener to be notified when subscriber group metrics are created or removed.
     *
     * @param listener  the listener to add
     */
    public void addSubscriberGroupListener(MapListener<String, SubscriberGroupStatistics> listener)
        {
        f_mapSubscriberGroups.addMapListener(listener);
        }

    /**
     * Add a subscriber group listener.
     *
     * @param listener  the listener to remove
     */
    public void removeSubscriberGroupListener(MapListener<String, SubscriberGroupStatistics> listener)
        {
        f_mapSubscriberGroups.removeMapListener(listener);
        }

    // ----- PublishedMetrics methods ---------------------------------------

    @Override
    public long getPublishedCount()
        {
        return f_metricPublished.getCount();
        }

    @Override
    public double getPublishedFifteenMinuteRate()
        {
        return f_metricPublished.getFifteenMinuteRate();
        }

    @Override
    public double getPublishedFiveMinuteRate()
        {
        return f_metricPublished.getFiveMinuteRate();
        }

    @Override
    public double getPublishedOneMinuteRate()
        {
        return f_metricPublished.getOneMinuteRate();
        }

    @Override
    public double getPublishedMeanRate()
        {
        return f_metricPublished.getMeanRate();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The channel count.
     */
    private final int f_cChannel;

    /**
     * The channel statistics.
     */
    @SuppressWarnings("unchecked")
    private final LongArray<PagedTopicChannelStatistics> m_aChannelStats = new SimpleLongArray();

    /**
     * The published messages metric.
     */
    private final Meter f_metricPublished = new Meter();

    /**
     * A map of {@link SubscriberGroupStatistics} keyed by subscriber group name.
     */
    private final ObservableMap<String, SubscriberGroupStatistics> f_mapSubscriberGroups = new ObservableHashMap<>();

    /**
     * The lock to use to synchronize access to internal state.
     */
    private final Lock f_lock = new ReentrantLock(true);
    }
