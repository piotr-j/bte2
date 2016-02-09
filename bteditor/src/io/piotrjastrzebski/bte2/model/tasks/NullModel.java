package io.piotrjastrzebski.bte2.model.tasks;


/**
 * Created by EvilEntity on 04/02/2016.
 */
public class NullModel extends TaskModel {
	public final static NullModel INSTANCE = new NullModel();
	private NullModel () {
		super(Type.NULL);
	}

	@Override public void free () {}

	@Override public TaskModel copy () {
		return INSTANCE;
	}

	@Override public String toString () {
		return "ModelNull{}";
	}
}
