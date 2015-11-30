package zx.soft.sent.solr.driver;

import zx.soft.sent.solr.allinternet.TaskUpdate;
import zx.soft.sent.solr.firstpage.FirstPageRun;
import zx.soft.sent.solr.firstpage.NegativeRecordsRun;
import zx.soft.sent.solr.index.ImportRedisToSC;
import zx.soft.sent.solr.insight.ImpalaUpdate;
import zx.soft.sent.solr.insight.InsightHotKey;
import zx.soft.sent.solr.insight.RelationCache;
import zx.soft.sent.solr.insight.RelationCacheV2;
import zx.soft.sent.solr.insight.UserActivity;
import zx.soft.sent.solr.origin.OriginUpdate;
import zx.soft.sent.solr.query.OracleToRedis;
import zx.soft.sent.solr.query.RemoveRedisReplicationData;
import zx.soft.sent.solr.query.RemoveSentiData;
import zx.soft.sent.solr.query.RemoveWeiboData;
import zx.soft.sent.solr.special.SpecialTopicRun;
import zx.soft.sent.solr.tmp.ImportSolrToGuangxi;
import zx.soft.utils.driver.ProgramDriver;

/**
 * 驱动类
 *
 * @author wanggang
 *
 */
public class SentSolrDriver {

	/**
	 * 主函数
	 */
	public static void main(String[] args) {

		int exitCode = -1;
		ProgramDriver pgd = new ProgramDriver();
		try {
			// 在hefei09机器上运行
			pgd.addClass("oracleToRedis", OracleToRedis.class, "将站点数据定时导入Redis中（默认是每小时）");
			// 在hefei07机器上运行
			pgd.addClass("specialTopicRun", SpecialTopicRun.class, "OA专题数据统计——临时分析");
			// 在hefei07机器上运行
			pgd.addClass("firstPageRun", FirstPageRun.class, "OA首页数据统计——临时分析");
			// 在hefei09,hefei10机器上运行
			pgd.addClass("importRedisToSC", ImportRedisToSC.class, "将Redis中的数据所引到SolrCloud");
			// 暂时不用
			pgd.addClass("removeSentiData", RemoveSentiData.class, "定时删除过期舆情数据");
			// 在hefei08机器上运行
			pgd.addClass("removeWeiboData", RemoveWeiboData.class, "定时删除过期微博数据");
			// 暂时不用
			pgd.addClass("removeRedisReplicationData", RemoveRedisReplicationData.class, "定时清理Redis去重数据");
			// 在hefei10机器上运行
			pgd.addClass("taskUpdate", TaskUpdate.class, "全网任务信息查询结果存储缓存信息");
			// 在hefei06机器上运行
			pgd.addClass("insightHotKey", InsightHotKey.class, "分时段对所有重点人员计算热门关键词");
			// 在hefei06机器上运行
			pgd.addClass("userActivity", UserActivity.class, "定时更新真实用户的活跃度");
			// 在hefei06机器上运行
			pgd.addClass("originUpdate", OriginUpdate.class, "定时更新溯源任务的缓存");
			
			pgd.addClass("relationCache", RelationCache.class, "定时更新重点人员关系");

			pgd.addClass("relationCacheV2", RelationCacheV2.class, "定时更新新浪博文关系");
			// 在hefei06机器上运行
			pgd.addClass("negativeRecordsRun", NegativeRecordsRun.class, "定时缓存负面信息关系");

			pgd.addClass("importSolrToGuangxi", ImportSolrToGuangxi.class, "临时任务，将省厅数据写入广西区厅");
			pgd.addClass("impalaUpdate", ImpalaUpdate.class, "定时更新impala表");
			pgd.driver(args);
			// Success
			exitCode = 0;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		System.exit(exitCode);

	}

}
