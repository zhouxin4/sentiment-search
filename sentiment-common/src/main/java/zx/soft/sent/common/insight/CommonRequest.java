package zx.soft.sent.common.insight;

public class CommonRequest {
	private String type;
	private int operation;
	private KeyUnit keyUnit;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getOperation() {
		return operation;
	}

	public void setOperation(int operation) {
		this.operation = operation;
	}

	public KeyUnit getKeyUnit() {
		return keyUnit;
	}

	public void setKeyUnit(KeyUnit keyUnit) {
		this.keyUnit = keyUnit;
	}

	public static class KeyUnit {
		public final Unit unit;

		public KeyUnit(Unit unit) {
			this.unit = unit;
		}

	}

	public static class Unit {
		public final int systemAreaCode;

		public Unit(int sysCode) {
			this.systemAreaCode = sysCode;
		}

	}

}
