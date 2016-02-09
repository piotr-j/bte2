package io.piotrjastrzebski.bte2.model.edit;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.tasks.TaskModel;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class MoveCommand extends Command {
	private static Pool<MoveCommand> pool = new Pool<MoveCommand>() {
		@Override protected MoveCommand newObject () {
			return new MoveCommand();
		}
	};
	public static MoveCommand obtain (TaskModel what, TaskModel where) {
		return pool.obtain().init(what, where);
	}

	public static MoveCommand obtain (int at, TaskModel what, TaskModel where) {
		return pool.obtain().init(at, what, where);
	}

	private AddCommand add;
	private RemoveCommand remove;

	public MoveCommand () {
		super(Type.MOVE);
	}

	private MoveCommand init (TaskModel what, TaskModel where) {
		return init(-1, what, where);
	}

	private MoveCommand init (int at, TaskModel what, TaskModel where) {
		remove = RemoveCommand.obtain(what);
		add = AddCommand.obtain(at, what, where);
		return this;
	}

	@Override public void execute () {
		remove.execute();
		add.execute();
	}

	@Override public void undo () {
		add.undo();
		remove.undo();
	}

	@Override public void reset () {
		add.free();
		add = null;
		remove.free();
		remove = null;
	}

	@Override public void free () {
		pool.free(this);
	}
}
