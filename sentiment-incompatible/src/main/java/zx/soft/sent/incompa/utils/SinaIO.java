package zx.soft.sent.incompa.utils;

import java.io.Closeable;

public interface SinaIO extends Closeable {

	public <T> void write(String key, T value);

}
