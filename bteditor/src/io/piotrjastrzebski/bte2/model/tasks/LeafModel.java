package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.LeafTask;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class LeafModel extends TaskModel implements Pool.Poolable {
	private final static Pool<LeafModel> pool = new Pool<LeafModel>() {
		@Override protected LeafModel newObject () {
			return new LeafModel();
		}
	};

	public static LeafModel obtain (LeafTask task, BehaviorTreeModel model) {
		return pool.obtain().init(task, model);
	}

	public static void free (LeafModel leaf) {
		pool.free(leaf);
	}

	private LeafModel () {
		super(Type.LEAF);
	}

	public LeafModel init (LeafTask task, BehaviorTreeModel model) {
		super.init(task, model);
		return this;
	}

	protected LeafModel init (LeafModel other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public LeafModel copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "LeafModel{"+wrapped+"}";
	}
}
