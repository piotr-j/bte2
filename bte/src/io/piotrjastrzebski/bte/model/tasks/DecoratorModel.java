package io.piotrjastrzebski.bte.model.tasks;

import com.badlogic.gdx.ai.btree.Decorator;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte.model.BehaviorTreeModel;

/**
 * TODO this would be nice, but is a bit of pain to implement
 * Possible situations, if we allow more than 1 task:
 * - 0 children, invalid state
 * 	- add - 1 child, valid state
 * - 1 child, valid state
 * 	- add - 2 children, invalid state
 *    - remove - 0 children, invalid state
 * - 2 children, invalid state
 *    - remove - 1 child, valid state
 *
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class DecoratorModel extends TaskModel implements Pool.Poolable {
	private final static Pool<DecoratorModel> pool = new Pool<DecoratorModel>() {
		@Override protected DecoratorModel newObject () {
			return new DecoratorModel();
		}
	};

	public static DecoratorModel obtain (Decorator task, BehaviorTreeModel model) {
		return pool.obtain().init(task, model);
	}

	public static void free (DecoratorModel leaf) {
		pool.free(leaf);
	}

	private DecoratorModel () {
		super(Type.DECORATOR);
	}

	public DecoratorModel init (Decorator task, BehaviorTreeModel model) {
		super.init(task, model);
		return this;
	}

	protected DecoratorModel init (DecoratorModel other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public DecoratorModel copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}


	@Override public String toString () {
		return "DecoratorModel{" +
			"name='" + name + '\'' +
			(valid?", valid":", invalid") +
			'}';
	}
}
