package io.piotrjastrzebski.bte;

import com.badlogic.gdx.ai.btree.Task;

/**
 * Created by PiotrJ on 26/02/16.
 */
public interface TaskInjector {
	void inject(Task task);
}
