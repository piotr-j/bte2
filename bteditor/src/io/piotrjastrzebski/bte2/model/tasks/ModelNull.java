package io.piotrjastrzebski.bte2.model.tasks;


/**
 * Created by EvilEntity on 04/02/2016.
 */
public class ModelNull extends ModelTask {
	public final static ModelNull INSTANCE = new ModelNull();
	private ModelNull () {
		super(Type.NULL);
	}

	@Override public void free () {}

	@Override public ModelTask copy () {
		return INSTANCE;
	}

	@Override public String toString () {
		return "ModelNull{}";
	}
}
