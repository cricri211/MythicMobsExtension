package com.gmail.berndivader.mythicmobsext.jboolexpr;

final class BooleanNotOperation implements IBoolean {

	private IBoolean iBoolean;

	BooleanNotOperation(final IBoolean newIBoolean) {
		if (newIBoolean == null) {
			throw new IllegalArgumentException("newIBoolean is null");
		}
		this.iBoolean = newIBoolean;
	}

	@Override
	public boolean booleanValue() {
		return (!this.iBoolean.booleanValue());
	}

	@Override
	public String toString() {
		return "(!" +
				this.iBoolean +
				")";
	}
}
