package io.piotrjastrzebski.bte2.model.edit;

import com.badlogic.gdx.utils.Pool;
import io.piotrjastrzebski.bte2.model.tasks.ModelTask;

/**
 * Created by EvilEntity on 05/02/2016.
 */
public class MoveCommand extends Command {
	private static Pool<MoveCommand> pool = new Pool<MoveCommand>() {
		@Override protected MoveCommand newObject () {
			return new MoveCommand();
		}
	};
	public static MoveCommand obtain (ModelTask what, ModelTask where) {
		return pool.obtain().init(what, where);
	}

	public static MoveCommand obtain (int at, ModelTask what, ModelTask where) {
		return pool.obtain().init(at, what, where);
	}

	private AddCommand add;
	private RemoveCommand remove;

	public MoveCommand () {
		super(Type.MOVE);
	}

	private MoveCommand init (ModelTask what, ModelTask where) {
		return init(-1, what, where);
	}

	private MoveCommand init (int at, ModelTask what, ModelTask where) {
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
