package zx.soft.sent.core.hbase;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class HbaseDao {
	private List<Put> puts;
	private int num;
	private String tableName;

	public HbaseDao(String tableName, int num) {
		this.num = num;
		this.tableName = tableName;
		this.puts = new ArrayList<Put>();
	}

	public void addSingleColumn(String rowKey, String family, String qualifier, String value) {
		Put put = new Put(Bytes.toBytes(rowKey));
		put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
		puts.add(put);
		if (puts.size() >= num) {
			flushPuts();
		}
	}

	public void addSingleColumn(byte[] rowKey, String family, String qualifier, String value) {
		Put put = new Put(rowKey);
		put.add(Bytes.toBytes(family), Bytes.toBytes(qualifier), Bytes.toBytes(value));
		puts.add(put);
		if (puts.size() >= num) {
			flushPuts();
		}
	}

	public void flushPuts() {
		HBaseUtils.put(tableName, puts);
		puts.clear();
	}
}
