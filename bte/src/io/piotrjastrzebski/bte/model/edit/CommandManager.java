package io.piotrjastrzebski.bte.model.edit;

import com.badlogic.gdx.utils.Array;

/**
 * Menages commands, allows for undo/redo etc
 *
 * Created by EvilEntity on 05/02/2016.
 */
public class CommandManager {
	private static final String TAG = CommandManager.class.getSimpleName();
	private Array<Command> commands = new Array<>();
	private int current = 0;

	public boolean canRedo () {
		return current < commands.size -1 && current >= -1;
	}

	public boolean redo() {
		if (!canRedo()) {
			return false;
		}
		Command command = commands.get(++current);
		command.execute();
		return true;
	}

	public boolean canUndo () {
		return commands.size > 0 && current >= 0;
	}

	public boolean undo() {
		if (!canUndo()) {
			return false;
		}
		Command command = commands.get(current--);
		command.undo();
		return true;
	}

	public void execute(Command command) {
		if (current < commands.size -1 && commands.size > 0) {
			for (int i = current + 1; i < commands.size -1; i++) {
				commands.get(i).free();
			}
			commands.removeRange(current + 1, commands.size - 1);
		}
		commands.add(command);
		command.execute();
		current = commands.size - 1;
	}

	public void reset() {
		for (Command command : commands) {
			command.free();
		}
		commands.clear();
	}
}
