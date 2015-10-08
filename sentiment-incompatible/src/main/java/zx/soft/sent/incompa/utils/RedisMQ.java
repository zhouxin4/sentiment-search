package zx.soft.sent.incompa.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import zx.soft.utils.config.ConfigUtil;
import zx.soft.utils.log.LogbackUtil;

public class RedisMQ {

	private static Logger logger = LoggerFactory.getLogger(RedisMQ.class);

	private static JedisPool pool;

	public RedisMQ() {
		init();
	}

	private void init() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(256);
		poolConfig.setMinIdle(64);
		poolConfig.setMaxWaitMillis(10_000);
		poolConfig.setMaxTotal(1024);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTimeBetweenEvictionRunsMillis(30000);
		Properties props = ConfigUtil.getProps("cache-config.properties");
		pool = new JedisPool(poolConfig, props.getProperty("redis.mq.server"), Integer.parseInt(props
				.getProperty("redis.mq.port")), 30_000, props.getProperty("redis.password"));
	}

	public synchronized static Jedis getJedis() {
		try {
			if (pool != null) {
				return pool.getResource();
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			return null;
		}
	}

	/**
	 * 添加数据，members不能为空
	 */
	public synchronized void addRecord(String cacheKey, String... members) {
		Jedis jedis = getJedis();
		if (jedis == null) {
			return;
		}
		try {
			jedis.sadd(cacheKey, members);
		} catch (Exception e) {
			logger.error("Exception:{},Records'size={}.", LogbackUtil.expection2Str(e), members.length);
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
				jedis = null;
			}
		} finally {
			// 这里很重要，一旦拿到的jedis实例使用完毕，必须要返还给池中
			if (jedis != null && jedis.isConnected()) {
				pool.returnResource(jedis);
			}
		}
	}

	public synchronized void addRecord(String cacheKey, String member) {
		Jedis jedis = getJedis();
		if (jedis == null) {
			return;
		}
		try {
			jedis.sadd(cacheKey, member);
		} catch (Exception e) {
			logger.error("Exception:{}.", LogbackUtil.expection2Str(e));
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
				jedis = null;
			}
		} finally {
			// 这里很重要，一旦拿到的jedis实例使用完毕，必须要返还给池中
			if (jedis != null && jedis.isConnected()) {
				pool.returnResource(jedis);
			}
		}
	}

	/**
	 * 获取集合大小
	 */
	public synchronized long getSetSize(String cacheKey) {
		long result = 0L;
		Jedis jedis = getJedis();
		if (jedis == null) {
			return result;
		}
		try {
			// 在事务和管道中不支持同步查询
			result = jedis.scard(cacheKey).longValue();
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
				jedis = null;
			}
		} finally {
			if (jedis != null && jedis.isConnected()) {
				pool.returnResource(jedis);
			}
		}
		return result;
	}

	/**
	 * 获取数据
	 */
	public synchronized List<String> getRecords(String cacheKey) {
		List<String> records = new ArrayList<>();
		Jedis jedis = getJedis();
		if (jedis == null) {
			return records;
		}
		try {
			String value = jedis.spop(cacheKey);
			for (int i = 0; (i < 100) && (value != null); i++) {
				records.add(value);
				value = jedis.spop(cacheKey);
			}
			logger.info("Records'size = {}", records.size());
		} catch (Exception e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
			if (jedis != null) {
				pool.returnBrokenResource(jedis);
				jedis = null;
			}
		} finally {
			if (jedis != null && jedis.isConnected()) {
				pool.returnResource(jedis);
			}
		}
		return records;
	}

	public void close() {
		// 程序关闭时，需要调用关闭方法
		pool.destroy();
	}
}
