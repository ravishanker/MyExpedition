package com.raywenderlich.android.myexpedition.rendering

import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.vector.Vector3


/**
 * Convenience class to encapsulate a position and orientation combination using Rajawali classes.
 */
class Pose(val position: Vector3, val orientation: Quaternion) {

    override fun toString(): String {
        return "p:$position,q:$orientation"
    }
}

