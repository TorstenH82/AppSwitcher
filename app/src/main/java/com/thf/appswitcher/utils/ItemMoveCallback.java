package com.thf.AppSwitcher.utils;

import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

public class ItemMoveCallback extends ItemTouchHelper.Callback {

	private final RecyclerViewAdapter mAdapter;

	public ItemMoveCallback(RecyclerViewAdapter adapter) {
		mAdapter = adapter;
	}

	@Override
	public boolean isLongPressDragEnabled() {
		return false; // true;
	}

	@Override
	public boolean isItemViewSwipeEnabled() {
		return false;
	}

	@Override
	public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {

	}

	@Override
	public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
		return makeMovementFlags(dragFlags, 0);
	}

	@Override
	public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
			RecyclerView.ViewHolder target) {
		mAdapter.onRowMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
		return true;
	}

	@Override
	public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

		if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
			if (viewHolder instanceof RecyclerViewAdapter.MyViewHolder) {
				RecyclerViewAdapter.MyViewHolder myViewHolder = (RecyclerViewAdapter.MyViewHolder) viewHolder;
				mAdapter.onRowSelected(myViewHolder);
			}

		}

		super.onSelectedChanged(viewHolder, actionState);
	}

	@Override
	public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
		super.clearView(recyclerView, viewHolder);

		if (viewHolder instanceof RecyclerViewAdapter.MyViewHolder) {
			RecyclerViewAdapter.MyViewHolder myViewHolder = (RecyclerViewAdapter.MyViewHolder) viewHolder;
			mAdapter.onRowClear(myViewHolder);
		}
	}

}