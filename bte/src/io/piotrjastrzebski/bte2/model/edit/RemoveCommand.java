package io.piotrjastrzebski.bte2.model.edit;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class RemoveCommand extends Command implements Pool.Poolable {
	private static Pool<RemoveCommand> pool = new Pool<RemoveCommand>() {
		@Override protected RemoveCommand newObject () {
			return new RemoveCommand();
		}
	};

	public static RemoveCommand obtain (TaskModel what) {
		return pool.obtain().init(what);
	}

	protected RemoveCommand() {
		super(Type.REMOVE);
	}

	protected TaskModel what;
	protected TaskModel parent;

	protected int idInParent;
	protected boolean removeGaurd;

	public RemoveCommand init (TaskModel what) {
		this.what = what;
		parent = what.getParent();
		// note top level guard doesn't have a parent, this could be set to guarded task
		if (parent == null && what.isGuard()) {
			removeGaurd = true;
			parent = what.getGuarded();
		}
		idInParent = parent.getChildId(what);
		return this;
	}

	@Override public void execute () {
		if (removeGaurd) {
			parent.removeGuard();
		} else {
			parent.removeChild(what);
		}
	}

	@Override public void undo () {
		if (removeGaurd) {
			parent.setGuard(what);
		} else {
			parent.insertChild(idInParent, what);
		}
	}

	@Override public void free () {
		pool.free(this);
	}

	@Override public void reset () {
		// free pools or whatever
		what = null;
		parent = null;
		idInParent = -1;
		removeGaurd = false;
	}
}
