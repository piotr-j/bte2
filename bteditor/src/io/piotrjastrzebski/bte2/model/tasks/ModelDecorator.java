package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.Decorator;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BTModel;

/**
 * TODO this would be nice, but is a bit of pain to implement
 * Possible situations, if we allow more than 1 task:
 * - 0 children, invalid state
 * 	- add -> 1 child, valid state
 * - 1 child, valid state
 * 	- add -> 2 children, invalid state
 *    - remove -> 0 children, invalid state
 * - 2 children, invalid state
 *    - remove -> 1 child, valid state
 *
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class ModelDecorator extends ModelTask implements Pool.Poolable {
	private final static Pool<ModelDecorator> pool = new Pool<ModelDecorator>() {
		@Override protected ModelDecorator newObject () {
			return new ModelDecorator();
		}
	};

	public static ModelDecorator obtain (Decorator task, BTModel model) {
		return pool.obtain().init(task, model);
	}

	public static void free (ModelDecorator leaf) {
		pool.free(leaf);
	}

	private ModelDecorator () {
		super(Type.DECORATOR);
	}

	public ModelDecorator init (Decorator task, BTModel model) {
		super.init(task, model);
		return this;
	}

	protected ModelDecorator init (ModelDecorator other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public ModelDecorator copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "ModelDecorator{"+wrapped+"}";
	}
}
