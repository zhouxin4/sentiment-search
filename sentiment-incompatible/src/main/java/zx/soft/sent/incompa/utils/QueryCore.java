package zx.soft.sent.incompa.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.utils.config.ConfigUtil;
import zx.soft.utils.log.LogbackUtil;

public class QueryCore implements SinaIO {

	private static Logger logger = LoggerFactory.getLogger(QueryCore.class);
	private final CloudSolrServer cloudServer;

	public QueryCore() throws MalformedURLException {

		Properties props = ConfigUtil.getProps("solr_params.properties");
		cloudServer = new CloudSolrServer(props.getProperty("zookeeper_cloud") + "/solr");
		cloudServer.setDefaultCollection(props.getProperty("cache_collection"));
		cloudServer.setZkConnectTimeout(Integer.parseInt(props.getProperty("zookeeper_connect_timeout")));
		cloudServer.setZkClientTimeout(Integer.parseInt(props.getProperty("zookeeper_client_timeout")));

		cloudServer.connect();
	}

	public static void main(String[] args) throws SolrServerException, IOException {
		//		SolrInputDocument doc = new SolrInputDocument();
		//		doc.setField("id", "36A7D04692076971DB4D8D9AEBB1C207");
		//		doc.setField("platform", 1);
		//		doc.setField("original_id", "0");
		//		doc.setField("original_uid", "0");
		//		doc.setField("url", "http://js.people.com.cn/n/2015/0316/c360313-24165215.html");
		//		doc.setField("title", "小龙女称压力大想轻生 吴绮莉否认精神虐待");
		//		doc.setField("isharmful", false);
		//		doc.setField("content", "据台湾媒体报道,昔日成龙外遇对象");
		//		doc.setField("timestamp", new Date(1426464000000l));
		//		doc.setField("source_id", 936);
		//		doc.setField("lasttime", new Date(1442419007000l));
		//		doc.setField("server_id", 3295);
		//		doc.setField("identify_id", 1);
		//		doc.setField("first_time", new Date(1442348249000l));
		//		doc.setField("ip", "221.228.65.10");
		//		doc.setField("location", "江苏省无锡市江阴市");
		//		doc.setField("source_name", "网易新闻");
		//		doc.setField("source_type", 1);
		//		doc.setField("country_code", 1);
		//		doc.setField("location_code", 320000);
		//		doc.setField("province_code", 32);
		//		doc.setField("cache_type", 1);
		//		doc.setField("cache_id", "25136C5BF3E9C324216E1E9E0886358C");
		//		doc.setField("cache_value", 611);
		//
		//		QueryCore test = new QueryCore();
		//		test.cloudServer.add(doc);
		//		test.cloudServer.commit();
		//		test.cloudServer.deleteById("12312312312qwfa");
		//		test.cloudServer.commit();

		HttpSolrServer server = new HttpSolrServer("http://bigdata1:8983/solr/");
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		System.out.println(server.query(query));

	}

	@Override
	public void close() throws IOException {
		cloudServer.shutdown();
	}

	private <T> SolrInputDocument trans(T value) {
		SolrInputDocument doc = new SolrInputDocument();
		Field[] fields = value.getClass().getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			try {
				Object obj = field.get(value);
				doc.addField(field.getName(), obj);
				logger.info("add field　" + field.getName());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return doc;
	}

	public static SolrInputDocument transSolrDocumentToInputDocument(SolrDocument doc) {
		SolrInputDocument input = new SolrInputDocument();

		input.addField("id", doc.getFieldValue("id").toString());
		if (doc.getFieldValue("platform") != null) {
			input.addField("platform", Integer.parseInt(doc.getFieldValue("platform").toString()));
		}
		if (doc.getFieldValue("mid") != null) {
			input.addField("mid", doc.getFieldValue("platform").toString());
		}
		if (doc.getFieldValue("username") != null) {
			input.addField("username", doc.getFieldValue("username").toString());
		}
		if (doc.getFieldValue("nickname") != null) {
			input.addField("nickname", doc.getFieldValue("nickname").toString());
		}
		if (doc.getFieldValue("original_id") != null) {
			input.addField("original_id", doc.getFieldValue("original_id").toString());
		}
		if (doc.getFieldValue("original_uid") != null) {
			input.addField("original_uid", doc.getFieldValue("original_uid").toString());
		}
		if (doc.getFieldValue("original_name") != null) {
			input.addField("original_name", doc.getFieldValue("original_name").toString());
		}
		if (doc.getFieldValue("original_title") != null) {
			input.addField("original_title", doc.getFieldValue("original_title").toString());
		}
		if (doc.getFieldValue("original_url") != null) {
			input.addField("original_url", doc.getFieldValue("original_url").toString());
		}
		if (doc.getFieldValue("url") != null) {
			input.addField("url", doc.getFieldValue("url").toString());
		}
		if (doc.getFieldValue("home_url") != null) {
			input.addField("home_url", doc.getFieldValue("home_url").toString());
		}
		if (doc.getFieldValue("title") != null) {
			input.addField("title", doc.getFieldValue("title").toString());
		}
		if (doc.getFieldValue("type") != null) {
			input.addField("type", doc.getFieldValue("type").toString());
		}
		if (doc.getFieldValue("isharmful") != null) {
			input.addField("isharmful", Boolean.parseBoolean(doc.getFieldValue("isharmful").toString()));
		}
		if (doc.getFieldValue("content") != null) {
			input.addField("content", doc.getFieldValue("content").toString());
		}
		if (doc.getFieldValue("comment_count") != null) {
			input.addField("comment_count", Integer.parseInt(doc.getFieldValue("comment_count").toString()));
		}
		if (doc.getFieldValue("read_count") != null) {
			input.addField("read_count", Integer.parseInt(doc.getFieldValue("read_count").toString()));
		}
		if (doc.getFieldValue("favorite_count") != null) {
			input.addField("favorite_count", Integer.parseInt(doc.getFieldValue("favorite_count").toString()));
		}
		if (doc.getFieldValue("attitude_count") != null) {
			input.addField("attitude_count", Integer.parseInt(doc.getFieldValue("attitude_count").toString()));
		}
		if (doc.getFieldValue("repost_count") != null) {
			input.addField("repost_count", Integer.parseInt(doc.getFieldValue("repost_count").toString()));
		}
		if (doc.getFieldValue("video_url") != null) {
			input.addField("video_url", doc.getFieldValue("video_url").toString());
		}
		if (doc.getFieldValue("pic_url") != null) {
			input.addField("pic_url", doc.getFieldValue("pic_url").toString());
		}
		if (doc.getFieldValue("voice_url") != null) {
			input.addField("voice_url", doc.getFieldValue("voice_url").toString());
		}
		if (doc.getFieldValue("timestamp") != null) {
			input.addField("timestamp", new Date(Long.parseLong(doc.getFieldValue("timestamp").toString())));
		}
		if (doc.getFieldValue("source_id") != null) {
			input.addField("source_id", Integer.parseInt(doc.getFieldValue("source_id").toString()));
		}
		if (doc.getFieldValue("lasttime") != null) {
			input.addField("lasttime", new Date(Long.parseLong(doc.getFieldValue("lasttime").toString())));
		}
		if (doc.getFieldValue("server_id") != null) {
			input.addField("server_id", Integer.parseInt(doc.getFieldValue("server_id").toString()));
		}
		if (doc.getFieldValue("identify_id") != null) {
			input.addField("identify_id", Long.parseLong(doc.getFieldValue("identify_id").toString()));
		}
		if (doc.getFieldValue("identify_md5") != null) {
			input.addField("identify_md5", doc.getFieldValue("identify_md5").toString());
		}
		if (doc.getFieldValue("keyword") != null) {
			input.addField("keyword", doc.getFieldValue("keyword").toString());
		}
		if (doc.getFieldValue("first_time") != null) {
			input.addField("first_time", new Date(Long.parseLong(doc.getFieldValue("first_time").toString())));
		}
		if (doc.getFieldValue("update_time") != null) {
			input.addField("update_time", new Date(Long.parseLong(doc.getFieldValue("update_time").toString())));
		}
		if (doc.getFieldValue("ip") != null) {
			input.addField("ip", doc.getFieldValue("ip").toString());
		}
		if (doc.getFieldValue("location") != null) {
			input.addField("location", doc.getFieldValue("location").toString());
		}
		if (doc.getFieldValue("geo") != null) {
			input.addField("geo", doc.getFieldValue("geo").toString());
		}
		if (doc.getFieldValue("receive_addr") != null) {
			input.addField("receive_addr", doc.getFieldValue("receive_addr").toString());
		}
		if (doc.getFieldValue("append_addr") != null) {
			input.addField("append_addr", doc.getFieldValue("append_addr").toString());
		}
		if (doc.getFieldValue("send_addr") != null) {
			input.addField("send_addr", doc.getFieldValue("send_addr").toString());
		}
		if (doc.getFieldValue("source_name") != null) {
			input.addField("source_name", doc.getFieldValue("source_name").toString());
		}
		if (doc.getFieldValue("source_type") != null) {
			input.addField("source_type", Integer.parseInt(doc.getFieldValue("source_type").toString()));
		}
		if (doc.getFieldValue("country_code") != null) {
			input.addField("country_code", Integer.parseInt(doc.getFieldValue("country_code").toString()));
		}
		if (doc.getFieldValue("location_code") != null) {
			input.addField("location_code", Integer.parseInt(doc.getFieldValue("location_code").toString()));
		}
		if (doc.getFieldValue("province_code") != null) {
			input.addField("province_code", Integer.parseInt(doc.getFieldValue("province_code").toString()));
		}
		if (doc.getFieldValue("city_code") != null) {
			input.addField("city_code", Integer.parseInt(doc.getFieldValue("city_code").toString()));
		}
		if (doc.getFieldValue("latitude") != null) {
			input.addField("latitude", Double.parseDouble((doc.getFieldValue("latitude").toString())));
		}
		if (doc.getFieldValue("longitude") != null) {
			input.addField("longitude", Double.parseDouble((doc.getFieldValue("longitude").toString())));
		}
		if (doc.getFieldValue("sent_index") != null) {
			input.addField("sent_index", Integer.parseInt(doc.getFieldValue("sent_index").toString()));
		}
		if (doc.getFieldValue("cache_type") != null) {
			input.addField("cache_type", Integer.parseInt(doc.getFieldValue("cache_type").toString()));
		}
		if (doc.getFieldValue("cache_id") != null) {
			input.addField("cache_id", doc.getFieldValue("cache_id").toString());
		}
		if (doc.getFieldValue("cache_value") != null) {
			input.addField("cache_value", Integer.parseInt(doc.getFieldValue("cache_value").toString()));
		}

		return input;
	}

	public void addDocToSolr(List<SolrInputDocument> docs) {
		try {
			cloudServer.add(docs);
			cloudServer.commit();
		} catch (RemoteSolrException | SolrServerException | IOException e) {
			logger.error("Exception:{}", LogbackUtil.expection2Str(e));
		}
	}

	private <T> SolrInputDocument transWeibo(T value) {
		SolrInputDocument doc = new SolrInputDocument();
		Field[] fields = value.getClass().getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			try {
				if (field.getName().equals("user") || field.getName().equals("pic_urls")
						|| field.getName().equals("visible") || field.getName().equals("darwin_tags")) {
					if (field.get(value) != null) {
						doc.addField(field.getName(), field.get(value).toString());
						logger.info("add field " + field.getName());
					}

				} else {
					doc.addField(field.getName(), field.get(value));
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return doc;
	}

	@Override
	public <T> void write(String key, T value) {
		// TODO Auto-generated method stub

	}

}
