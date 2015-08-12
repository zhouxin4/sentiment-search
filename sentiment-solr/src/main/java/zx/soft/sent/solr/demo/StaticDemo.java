package zx.soft.sent.solr.demo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField.Count;

import zx.soft.sent.solr.query.QueryCore;
import zx.soft.utils.time.TimeUtils;

public class StaticDemo {
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		startTime = TimeUtils.transTimeLong("2014-05-24 00:00:00");
		long threeOld = startTime - 3 * 24 * 60 * 60 * 1000;
		String fq = "timestamp:[" + TimeUtils.transToSolrDateStr(threeOld) + " TO "
				+ TimeUtils.transToSolrDateStr(startTime) + "]";
		QueryCore core = QueryCore.getInstance();
		SolrQuery query = new SolrQuery();
		query.set("q", "*:*");
		query.addFilterQuery(fq);
		query.addFilterQuery("-platform:(7 OR 8)");
		query.setFacet(true);
		query.set("facet.field", "source_id");
		query.set("f.source_id.facet.limit", -1);
		query.setRows(0);
		List<Count> counts = core.queryData(query);
		Map<String, Long> maps = new HashMap<String, Long>();
		for (Count count : counts) {
			maps.put(count.getName(), count.getCount());
		}
		List<String> keys = readFile("source_id0626.csv");
		PrintWriter writer = new PrintWriter(new FileWriter("source_id0629.csv"));
		boolean firstLine = true;
		for (String key : keys) {
			String source_id = key.split(",")[1];
			if (maps.containsKey(source_id)) {
				writer.println(key + "," + maps.get(source_id));
			} else {
				if (firstLine) {
					writer.println(key + "," + "2015-06-29");
					firstLine = false;
				} else {
					writer.println(key + "," + "0");
				}
			}
		}
		writer.close();

	}

	public static List<String> readFile(String file) {
		List<String> lists = new LinkedList<String>();
		try {
			BufferedReader read = null;
			try {
				read = new BufferedReader(new FileReader(file));
				String str = null;
				while ((str = read.readLine()) != null) {
					lists.add(str);
				}
			} finally {
				read.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lists;
	}

}
