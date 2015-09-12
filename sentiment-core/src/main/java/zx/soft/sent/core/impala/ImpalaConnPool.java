package zx.soft.sent.core.impala;

import zx.soft.utils.threads.ObjectFactory;

/**
 * ImpalaJDBC连接池
 * @author donglei
 *
 */
public class ImpalaConnPool extends ObjectFactory<ImpalaJdbc> {
	private static ImpalaConnPool pool = null;

	private ImpalaConnPool(int num, int tryNum) {
		super(num, tryNum);
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

}
