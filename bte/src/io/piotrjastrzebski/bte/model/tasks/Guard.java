package io.piotrjastrzebski.bte.model.tasks;

import com.badlogic.gdx.ai.btree.annotation.TaskConstraint;
import com.badlogic.gdx.ai.btree.branch.Selector;

/**
 * Fake Task used to fake guards in the tree
 *
 * Created by PiotrJ on 10/02/16.
 */
@TaskConstraint(minChildren = 2, maxChildren = 2)
public class Guard extends Selector {
}
