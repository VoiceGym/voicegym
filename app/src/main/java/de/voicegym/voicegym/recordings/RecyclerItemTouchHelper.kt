package de.voicegym.voicegym.recordings

import android.graphics.Canvas
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import kotlinx.android.synthetic.main.fragment_recordings.recordingsForeground

class RecyclerItemTouchHelper(dragDirs: Int, swipeDirs: Int, private val listener: RecyclerItemTouchHelperListener) :
        ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, target: RecyclerView.ViewHolder?): Boolean {
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
        viewHolder!!
        listener.onSwiped(viewHolder, direction, viewHolder.adapterPosition)
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (viewHolder != null) {
            val foregroundView = (viewHolder as RecordingsAdapter.ViewHolder).recordingsForeground

            getDefaultUIUtil().onSelected(foregroundView)
        }
    }

    override fun onChildDrawOver(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val foregroundView = (viewHolder as RecordingsAdapter.ViewHolder).recordingsForeground
        getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive)
    }

    override fun clearView(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?) {
        val foregroundView = (viewHolder as RecordingsAdapter.ViewHolder).recordingsForeground
        getDefaultUIUtil().clearView(foregroundView)
    }

    override fun onChildDraw(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val foregroundView = (viewHolder as RecordingsAdapter.ViewHolder).recordingsForeground
        getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, dX, dY,
                actionState, isCurrentlyActive)
    }

}

interface RecyclerItemTouchHelperListener {
    fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int, position: Int)
}
