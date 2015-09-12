package zx.soft.sent.core.hdfs;

import java.io.Closeable;

/**
 * HDFS写数据接口
 * 
 * @author wanggang
 *
 */
public interface HdfsWriter extends Closeable {

	void write(String key, String value);

}
