package io.piotrjastrzebski.bte2.model.tasks;

import com.badlogic.gdx.ai.btree.BranchTask;
import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.BehaviorTreeModel;

/**
 * Created by EvilEntity on 04/02/2016.
 */
@SuppressWarnings("rawtypes")
public class BranchModel extends TaskModel implements Pool.Poolable {
	private final static Pool<BranchModel> pool = new Pool<BranchModel>() {
		@Override protected BranchModel newObject () {
			return new BranchModel();
		}
	};

	public static BranchModel obtain (BranchTask task, BehaviorTreeModel model) {
		return pool.obtain().init(task, model);
	}

	public static void free (BranchModel leaf) {
		pool.free(leaf);
	}

	private BranchModel () {
		super(Type.BRANCH);
	}

	public BranchModel init (BranchTask task, BehaviorTreeModel model) {
		super.init(task, model);
		return this;
	}

	protected BranchModel init (BranchModel other) {
		super.init(other.wrapped.cloneTask(), other.model);
		return this;
	}

	@Override public BranchModel copy () {
		return pool.obtain().init(this);
	}

	public void free () {
		pool.free(this);
	}

	@Override public String toString () {
		return "BranchModel{"+wrapped+"}";
	}
}
