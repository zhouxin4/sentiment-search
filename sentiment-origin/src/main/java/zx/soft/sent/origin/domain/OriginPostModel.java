package zx.soft.sent.origin.domain;

import java.util.LinkedList;
import java.util.List;

import org.apache.solr.common.SolrDocument;

import com.google.common.base.MoreObjects;

/**
 *
 * @author donglei
 *
 */
public class OriginPostModel {
	// 总数
	private int count;
	// 溯源结果
	private int origin;
	//更新时间
	private String updateTime;
	// 页码
	private int page;
	// 当前页博文
	private List<SolrDocument> docs = new LinkedList<>();

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(OriginPostModel.class).add("count", count).add("page", page)
				.add("docs", docs).toString();
	}

	public void addDoc(SolrDocument doc) {
		docs.add(doc);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public String getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public List<SolrDocument> getDocs() {
		return docs;
	}

	public void setDocs(List<SolrDocument> docs) {
		this.docs = docs;
	}

}
