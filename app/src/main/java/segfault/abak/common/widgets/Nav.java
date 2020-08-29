package segfault.abak.common.widgets;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import segfault.abak.MainActivity;
import segfault.abak.R;

/**
 * Navigation helpers
 */
public class Nav {
    /**
     * Obtain the navigation controller
     * @param fragment The fragment must be associated with an Activity.
     */
    @NonNull
    public static NavController get(@NonNull Fragment fragment) {
        return Navigation.findNavController(fragment.requireActivity(), R.id.nav_host_fragment);
    }

    @NonNull
    public static NavController get(@NonNull MainActivity mainActivity) {
        return Navigation.findNavController(mainActivity, R.id.nav_host_fragment);
    }

    /**
     * Determines whether the given <code>destId</code> matches the NavDestination. This handles
     * both the default case (the destination's id matches the given id) and the nested case where
     * the given id is a parent/grandparent/etc of the destination.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean matchDestination(@NonNull NavDestination destination,
                                            @IdRes int destId) {
        NavDestination currentDestination = destination;
        while (currentDestination.getId() != destId && currentDestination.getParent() != null) {
            currentDestination = currentDestination.getParent();
        }
        return currentDestination.getId() == destId;
    }

    /**
     * Finds the actual start destination of the graph, handling cases where the graph's starting
     * destination is itself a NavGraph.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    public static NavDestination findStartDestination(@NonNull NavGraph graph) {
        NavDestination startDestination = graph;
        while (startDestination instanceof NavGraph) {
            NavGraph parent = (NavGraph) startDestination;
            startDestination = parent.findNode(parent.getStartDestination());
        }
        return startDestination;
    }

}
