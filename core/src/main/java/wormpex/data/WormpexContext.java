package wormpex.data;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wormpex.data.util.Pair;
import wormpex.data.util.ProxyUser;
import wormpex.data.util.ProxyUserQueue;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WormpexContext {
    private static long SYNC_TIME_OUT = 3000;
    private static TimeUnit SYNC_TIME_OUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static ReentrantReadWriteLock SYNC_BIZ_CODES_LOCK = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock SYNC_PROXY_USER_LOCK = new ReentrantReadWriteLock();

    private static Logger LOGGER = LoggerFactory.getLogger(WormpexContext.class);
    private static Cache<String, ProxyUserQueue> userMappingCache = CacheBuilder.newBuilder()
            .maximumSize(5000).build(new CacheLoader<String, ProxyUserQueue>() {
                @Override
                public ProxyUserQueue load(String key) throws Exception {
                    return new ProxyUserQueue();
                }
            });
    private static Cache<String, Set<Pair>> BIZ_CODES_CACHE = CacheBuilder.newBuilder()
            .maximumSize(99999).build(new CacheLoader<String, Set<Pair>>() {
                @Override
                public Set<Pair> load(String key) throws Exception {
                    return new HashSet<Pair>();
                }
            });
    private static final String DEFAULT = "default";
    protected static List<Pair> DEFAULT_BIZ_CODES = Collections.singletonList(new Pair("default", "default"));
    public static final String DEFAULT_PROXY_USER = "wstats";

    public final static String getProxyUserforBizCode(String bizCode) {
        ReentrantReadWriteLock.ReadLock readLock = SYNC_PROXY_USER_LOCK.readLock();
        try {
            readLock.lock();
            if (StringUtils.isBlank(bizCode)) {
                return DEFAULT_PROXY_USER;
            }
            ProxyUserQueue proxyUserQueue = userMappingCache.get(bizCode);
            if (proxyUserQueue.isEmpty()) {
                return DEFAULT_PROXY_USER;
            } else {
                return proxyUserQueue.getLastUserName();
            }
        } catch (ExecutionException e) {
            LOGGER.warn("getProxyUserforBizCodeFail");
        } finally {
            readLock.unlock();
        }
        return DEFAULT_PROXY_USER;
    }

    public final static List<Pair> getBizCodes() {
        List<Pair> ret;
        ReentrantReadWriteLock.ReadLock readLock = SYNC_BIZ_CODES_LOCK.readLock();
        try {
            readLock.lock();
            Set<Pair> pairs = BIZ_CODES_CACHE.get(DEFAULT);
            if (CollectionUtils.isEmpty(pairs)) {
                ret = DEFAULT_BIZ_CODES;
            } else {
                ret = Lists.newArrayList(pairs);
            }
        } catch (Exception e) {
            LOGGER.warn("getBizCodes fail", e);
            ret = DEFAULT_BIZ_CODES;
        } finally {
            readLock.unlock();
        }
        return ret;
    }

    private final static void setUserMappingCache(String biz, ProxyUser proxyUser) throws ExecutionException {
        ProxyUserQueue proxyUserQueue = userMappingCache.get(biz);
        if (proxyUserQueue == null) {
            proxyUserQueue = new ProxyUserQueue();
            userMappingCache.put(biz, proxyUserQueue);
        }
        if (proxyUser != null) {
            proxyUserQueue.add(proxyUser);
        }
    }

    public final static void syncBizCodes(Set<Pair> pairs) {
        ReentrantReadWriteLock.WriteLock writeLock = SYNC_BIZ_CODES_LOCK.writeLock();
        try {
            writeLock.lock();
            final Set<Pair> old = BIZ_CODES_CACHE.get(DEFAULT);
            Sets.SetView<Pair> needClean = Sets.difference(old, pairs);
            old.removeAll(needClean);
            old.addAll(pairs);
            BIZ_CODES_CACHE.put(DEFAULT, old);
        } catch (Exception e) {
            LOGGER.warn("syncBizCodes fail data:{} e:", JSON.toJSON(pairs), e);
        } finally {
            writeLock.unlock();
        }
    }

    public final static void syncProxyUsers(Map<String, ProxyUser> proxyUserInfo) {
        ReentrantReadWriteLock.WriteLock writeLock = SYNC_PROXY_USER_LOCK.writeLock();
        try {
            writeLock.lock();
            for (Map.Entry<String, ProxyUser> entry : proxyUserInfo.entrySet()) {
                if (StringUtils.isNotBlank(entry.getKey())) {
                    setUserMappingCache(entry.getKey(), entry.getValue());
                } else {
                    LOGGER.warn("syncProxyUsers for:{} :{}", entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("syncBizCodes fail data:{} e:", JSON.toJSON(proxyUserInfo), e);
        } finally {
            writeLock.unlock();
        }
    }
}
package wormpex.data;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wormpex.data.util.Pair;
import wormpex.data.util.ProxyUser;
import wormpex.data.util.ProxyUserQueue;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WormpexContext {
    private static long SYNC_TIME_OUT = 3000;
    private static TimeUnit SYNC_TIME_OUT_TIME_UNIT = TimeUnit.MILLISECONDS;
    private static ReentrantReadWriteLock SYNC_BIZ_CODES_LOCK = new ReentrantReadWriteLock();
    private static ReentrantReadWriteLock SYNC_PROXY_USER_LOCK = new ReentrantReadWriteLock();

    private static Logger LOGGER = LoggerFactory.getLogger(WormpexContext.class);
    private static Cache<String, ProxyUserQueue> userMappingCache = CacheBuilder.newBuilder()
            .maximumSize(5000).build(new CacheLoader<String, ProxyUserQueue>() {
                @Override
                public ProxyUserQueue load(String key) throws Exception {
                    return new ProxyUserQueue();
                }
            });
    private static Cache<String, Set<Pair>> BIZ_CODES_CACHE = CacheBuilder.newBuilder()
            .maximumSize(99999).build(new CacheLoader<String, Set<Pair>>() {
                @Override
                public Set<Pair> load(String key) throws Exception {
                    return new LinkedHashSet<Pair>();
                }
            });
    private static final String DEFAULT = "default";
    protected static List<Pair> DEFAULT_BIZ_CODES = Collections.singletonList(new Pair("default", "default"));
    public static final String DEFAULT_PROXY_USER = "wstats";

    public final static String getProxyUserforBizCode(String bizCode) {
        ReentrantReadWriteLock.ReadLock readLock = SYNC_PROXY_USER_LOCK.readLock();
        try {
            //同步用户读锁
            readLock.lock();
            if (StringUtils.isBlank(bizCode)) {
                return DEFAULT_PROXY_USER;
            }
            ProxyUserQueue proxyUserQueue = userMappingCache.get(bizCode);
            if (proxyUserQueue.isEmpty()) {
                return DEFAULT_PROXY_USER;
            } else {
                return proxyUserQueue.getLastUserName();
            }
        } catch (ExecutionException e) {
            LOGGER.warn("getProxyUserforBizCodeFail");
        } finally {
            readLock.unlock();
        }
        return DEFAULT_PROXY_USER;
    }

    public final static List<Pair> getBizCodes() {

        List<Pair> ret = null;
        ReentrantReadWriteLock.ReadLock readLock = SYNC_BIZ_CODES_LOCK.readLock();
        try {
            //同步业务线读锁
            readLock.lock();
            //System.out.println("----Thread:"+Thread.currentThread().getName()+"----read first value:"+ null);

            Set<Pair> pairs = BIZ_CODES_CACHE.get(DEFAULT);
            if (CollectionUtils.isEmpty(pairs)) {
                ret = DEFAULT_BIZ_CODES;
            } else {
                ret = Lists.newArrayList(pairs);
            }
            //System.out.println("----Thread:"+Thread.currentThread().getName()+"----read second value:"+ ret);
        } catch (Exception e) {
            LOGGER.warn("getBizCodes fail", e);
            ret = DEFAULT_BIZ_CODES;
        } finally {
            readLock.unlock();
        }
        return ret;
    }

    private final static void setUserMappingCache(String biz, ProxyUser proxyUser) throws ExecutionException {

        ProxyUserQueue proxyUserQueue = userMappingCache.get(biz);
        if (proxyUserQueue == null) {
            proxyUserQueue = new ProxyUserQueue();
            userMappingCache.put(biz, proxyUserQueue);
        }
        if (proxyUser != null) {
            proxyUserQueue.add(proxyUser);
        }
    }

    public final static void syncBizCodes(Set<Pair> pairs) {
        ReentrantReadWriteLock.WriteLock writeLock = SYNC_BIZ_CODES_LOCK.writeLock();
        try {
            //同步业务线写锁
            writeLock.lock();
            final Set<Pair> old = BIZ_CODES_CACHE.get(DEFAULT);
            Sets.SetView<Pair> needClean = Sets.difference(old, pairs);
            old.removeAll(needClean);
            old.addAll(pairs);
            BIZ_CODES_CACHE.put(DEFAULT, old);
        } catch (Exception e) {
            LOGGER.warn("syncBizCodes fail data:{} e:", JSON.toJSON(pairs), e);
        } finally {
            writeLock.unlock();
        }
    }

    public final static void syncProxyUsers(Map<String, ProxyUser> proxyUserInfo) {
        ReentrantReadWriteLock.WriteLock writeLock = SYNC_PROXY_USER_LOCK.writeLock();
        try {
            writeLock.lock();
            Set<Map.Entry<String, ProxyUser>> entries = proxyUserInfo.entrySet();
            for (Map.Entry<String, ProxyUser> entry : entries) {
                if (StringUtils.isNotBlank(entry.getKey())) {
                    setUserMappingCache(entry.getKey(), entry.getValue());
                } else {
                    LOGGER.warn("syncProxyUsers for:{} :{}", entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("syncBizCodes fail data:{} e:", JSON.toJSON(proxyUserInfo), e);
        } finally {
            writeLock.unlock();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 1; i++) {
            Thread t1 = new Thread() {
                /**
                 * If this thread was constructed using a separate
                 * <code>Runnable</code> run object, then that
                 * <code>Runnable</code> object's <code>run</code> method is called;
                 * otherwise, this method does nothing and returns.
                 * <p>
                 * Subclasses of <code>Thread</code> should override this method.
                 *
                 * @see #start()
                 * @see #stop()
                 * @see #Thread(ThreadGroup, Runnable, String)
                 */
                @Override
                public void run() {
                    while (true) {

                        try {
                            long currentTimeMillis = System.currentTimeMillis();
                            ProxyUser proxyUser = new ProxyUser(Thread.currentThread().getName() + " " + currentTimeMillis + "");
                            HashSet<Pair> pairs = null;
                            if (currentTimeMillis % 7 == 0) {
                                pairs = Sets.newHashSet(new Pair(proxyUser.getName() + "a", proxyUser.getName()), new Pair(proxyUser.getName() + "b", proxyUser.getName()));
                            } else if (currentTimeMillis % 7 == 1) {
                                pairs = Sets.newHashSet(new Pair(proxyUser.getName() + "d", proxyUser.getName()), new Pair(proxyUser.getName() + "c", proxyUser.getName()));
                            } else if (currentTimeMillis % 7 == 2) {
                                pairs = Sets.newHashSet(new Pair(proxyUser.getName() + "e", proxyUser.getName()), new Pair(proxyUser.getName() + "b", proxyUser.getName()));
                            } else if (currentTimeMillis % 7 == 3) {
                                pairs = Sets.newHashSet(new Pair(proxyUser.getName() + "b", proxyUser.getName()), new Pair(proxyUser.getName() + "a", proxyUser.getName()));
                            } else if (currentTimeMillis % 7 == 4) {
                                pairs = Sets.newHashSet(new Pair(proxyUser.getName() + "b", proxyUser.getName()));
                            } else {
                                pairs = Sets.newHashSet(new Pair(proxyUser.getName() + "OOOO", proxyUser.getName()), new Pair(proxyUser.getName() + "AAA", proxyUser.getName()));
                            }
                            HashMap map = new HashMap();
                            map.put("user", proxyUser);
                            WormpexContext.syncProxyUsers(map);
                            //System.out.println("set " + proxyUser);
                            WormpexContext.syncBizCodes(pairs);
                            TimeUnit.SECONDS.sleep(60);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                {

                }
            };
            t1.start();
        }
        for (int i = 0; i < 2000; i++) {
            Thread t2 = new Thread() {
                /**
                 * If this thread was constructed using a separate
                 * <code>Runnable</code> run object, then that
                 * <code>Runnable</code> object's <code>run</code> method is called;
                 * otherwise, this method does nothing and returns.
                 * <p>
                 * Subclasses of <code>Thread</code> should override this method.
                 *
                 * @see #start()
                 * @see #stop()
                 * @see #Thread(ThreadGroup, Runnable, String)
                 */
                @Override
                public void run() {
                    while (true) {
                        try {
                            String proxyUserforBizCode = WormpexContext.getProxyUserforBizCode("user");
                            List<Pair> bizCodes = WormpexContext.getBizCodes();
                            for (Pair pair : bizCodes) {
                                if (pair.getLeft().equals("default")){
                                    pair.getRight();
                                }
                            }
                            List<Pair> codes = WormpexContext.getBizCodes();
                            for (Pair pair : codes) {
                                //System.out.println(pair);
                            }
                            //System.out.println(CollectionUtils.isEmpty(codes));
                            //System.out.println("get " + bizCodes);
                            //System.out.println("SIZE: " +  WormpexContext.getBizCodes() + "  "+  WormpexContext.getBizCodes().size());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                {

                }
            };
            t2.start();
        }
    }
}
