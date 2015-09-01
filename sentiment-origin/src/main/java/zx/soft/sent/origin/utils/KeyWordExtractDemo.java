package zx.soft.sent.origin.utils;

import java.util.Collection;

import org.ansj.app.keyword.KeyWordComputer;
import org.ansj.app.keyword.Keyword;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

public class KeyWordExtractDemo {

	private static KeyWordComputer kmc;
	static {
		kmc = new KeyWordComputer();
	}

	public static void main(String[] args) {
		String[] contents = {
				"随着以习近平同志为总书记的党中央领导全党深入开展党的群众路线教育实践活动以来,涌现出一大批执法为民、廉洁奉公的先进事迹。",
				"章便杀人灭口。 大量案件显示,这两起官员杀死情妇的悲剧,既不是空前,也注定不是绝后。远到山东省济南市人大常委会原主任段义和在闹市炸死情妇",
				"膀胱屡和妊娠压迫等刺激。 安徽宣城医院男科哪家好宣城九洲医院医生温馨提醒:男性尿急尿痛时应及时到医院进行检查,要确切知道引起男性尿急尿痛的病因后,再配合医生积",
				"有俄罗斯国会议员，9号在社交网站推特表示，美国中情局前雇员斯诺登，已经接受委内瑞拉的庇护，不过推文在发布几分钟后随即删除。俄罗斯当局拒绝发表评论，而一直协助斯诺登的维基解密否认他将投靠委内瑞拉。　　俄罗斯国会国际事务委员会主席普什科夫，在个人推特率先披露斯诺登已接受委内瑞拉的庇护建议，令外界以为斯诺登的动向终于有新进展。　　不过推文在几分钟内旋即被删除，普什科夫澄清他是看到俄罗斯国营电视台的新闻才这样说，而电视台已经作出否认，称普什科夫是误解了新闻内容。　　委内瑞拉驻莫斯科大使馆、俄罗斯总统府发言人、以及外交部都拒绝发表评论。而维基解密就否认斯诺登已正式接受委内瑞拉的庇护，说会在适当时间公布有关决定。　　斯诺登相信目前还在莫斯科谢列梅捷沃机场，已滞留两个多星期。他早前向约20个国家提交庇护申请，委内瑞拉、尼加拉瓜和玻利维亚，先后表示答应，不过斯诺登还没作出决定。　　而另一场外交风波，玻利维亚总统莫拉莱斯的专机上星期被欧洲多国以怀疑斯诺登在机上为由拒绝过境事件，涉事国家之一的西班牙突然转口风，外长马加略]号表示愿意就任何误解致歉，但强调当时当局没有关闭领空或不许专机降落。",
				"我和老婆是大叔恋，我已经四十多岁了，老婆还是如狼似虎的年纪。我平时的欲望比较低，体力也不行，心里挺内疚的。还好后来我在百度找到一款《百遗年核桃芝麻黑豆粉》，吃了6瓶后，白发减少了，头发乌黑有光泽了。感觉体力恢复了不少，老婆说我像个毛躁的小伙子",
				"你个傻狗，说联想拿来主义，好像小米魅族华为不是拿来主义一样，最讨厌你这种一本正经的胡说八道装专家的人。", "12306 胡说八道" };
		for (int i = 0; i < 100; i++) {
			int j = (int) (Math.random() * contents.length);
			new Thread(new MyRunnable(kmc, contents[j])).start();
		}

	}
}

class MyRunnable implements Runnable {

	private KeyWordComputer kmc;
	private String content;

	public MyRunnable(KeyWordComputer kmc, String content) {
		this.kmc = kmc;
		this.content = content;
	}

	@Override
	public void run() {
		Collection<Keyword> result = kmc.computeArticleTfidf("", content);
		Collection<String> key = Collections2.transform(result, new Function<Keyword, String>() {

			@Override
			public String apply(Keyword input) {
				// TODO Auto-generated method stub
				return input.getName();
			}
		});
		System.out.println(key);
	}

}
