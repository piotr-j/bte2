package io.piotrjastrzebski.bte.model.edit;

import com.badlogic.gdx.utils.Pool;

public abstract class Command implements Pool.Poolable {
	public enum Type {ADD, REMOVE, MOVE, COPY;}

	protected Type type;
	public Command (Type type) {
		this.type = type;
	}
	protected abstract void execute ();
	protected abstract void undo ();
	protected abstract void free ();
}
