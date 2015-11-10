package zx.soft.sent.core.impala;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import zx.soft.utils.log.LogbackUtil;
import zx.soft.utils.threads.ObjectFactory;

/**
 * ImpalaJDBC连接池
 * @author donglei
 *
 */
public class ImpalaConnPool extends ObjectFactory<ImpalaJdbc> {

	public static final String VALID = "select 1";

	private static Logger logger = LoggerFactory.getLogger(ImpalaConnPool.class);
	private static ImpalaConnPool pool = null;

	private ImpalaConnPool(int num, int tryNum) {
		super(num, tryNum, 60 * 1000);
	}

	public synchronized static ImpalaConnPool getPool(int num, int tryNum) {
		if (pool == null) {
			pool = new ImpalaConnPool(num, tryNum);
		}
		return pool;

	}

	@Override
	protected ImpalaJdbc create() {
		return new ImpalaJdbc();
	}

	@Override
	public boolean validate(ImpalaJdbc o) {
		try {
			ResultSet result = o.Query(VALID);
			while (result.next()) {
				logger.info("SELECT 1 结果为：" + result.getString(1));
			}
			result.close();
			return true;
		} catch (SQLException e) {
			logger.error(LogbackUtil.expection2Str(e));
		}
		return false;
	}

	@Override
	public void expire(ImpalaJdbc o) {
		o.closeConnection();
	}

	@Override
	public synchronized void clear() {
		ImpalaJdbc jdbc = null;
		try {
			while ((jdbc = pool.checkOut()) != null) {
				expire(jdbc);
			}
		} catch (InterruptedException e) {
			logger.info(LogbackUtil.expection2Str(e));
		}
		super.clear();
	}

}
