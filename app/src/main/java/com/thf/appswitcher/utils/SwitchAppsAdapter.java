package com.thf.AppSwitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.Switch;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import com.thf.AppSwitcher.R;

public class SwitchAppsAdapter extends RecyclerView.Adapter<SwitchAppsAdapter.MyViewHolder> {
	private static final String TAG = "AppSwitcherService";
	private List<AppData> appDataList0;
	private int position = 0;
	private int selectedPosition = 0;
	private static String STREAM_URL;
	//private Station currentStation;
	private Context context;
	private float brightness;

	public interface Listener {
		void onItemClick(View item, AppData app);

		void onTouch();

		void onTitleChanged(String title);
	}

	private final Listener listener;

	public SwitchAppsAdapter(Context context, List<AppData> appDataList, float brightness, Listener listener) {
		appDataList0 = appDataList;
		this.brightness = brightness;
		this.listener = listener;
		this.context = context;
	}

	class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnTouchListener {

		TextView name;
		ImageView logo;

		MyViewHolder(View v) {
			super(v);
			v.setOnClickListener(this);
			v.setOnTouchListener(this);
			logo = v.findViewById(R.id.itemLogo);
			name = v.findViewById(R.id.itemName);
		}

		@Override
		public void onClick(View v) {
			v.setBackgroundColor(Color.TRANSPARENT);
			AppData app = appDataList0.get(getAdapterPosition());
			//Toast.makeText(context, getAdapterPosition() + "", Toast.LENGTH_SHORT).show();
			listener.onItemClick(v, app);

			/*
			ComponentName name = new ComponentName(app.getPackageName(), app.getActivityName());
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_LAUNCHER);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			i.setComponent(name);
			
			try {
				startActivity(i);
				} catch (Exception ex) {
				Log.e(TAG, "Error starting activity: " + ex.getMessage());
			}
			//finish();
			*/
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {

				//myBomb.disarm();
				clearPosition();
				//settingsSel.setVisibility(View.VISIBLE);
				v.setBackgroundColor(Color.TRANSPARENT);

				listener.onTouch();
			}
			return false;
		}

	}

	//MyAdapter(List<AppData> appDataList) {
	//		appDataList0 = appDataList;
	//	}

	@Override
	public SwitchAppsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_switch, parent, false);
		return new MyViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(MyViewHolder holder, int position) {
		AppData app = appDataList0.get(position);
		//holder.itemView.setSelected(true);

		holder.name.setText(app.getDescription());

		Drawable icon = app.getIcon(context);
        
		if (icon != null) {
            ColorMatrix cm = new ColorMatrix();
    cm.setSaturation(0);
            icon.setColorFilter(new ColorMatrixColorFilter(cm));
			holder.logo.setImageDrawable(icon);
		}

		if (position == selectedPosition || selectedPosition == -99) {
			holder.name.setAlpha(1f);
			holder.logo.setAlpha(1f);
			if (position == selectedPosition) {
				listener.onTitleChanged(app.getDescription());
				//txtTitle.setText(app.getDescription());
			}
		} else {
			holder.name.setAlpha(brightness);
			holder.logo.setAlpha(brightness);
		}

	}

	@Override
	public int getItemCount() {
		return appDataList0.size();
	}

	public void setItems(List<AppData> newList) {
		appDataList0 = newList;
		selectedPosition = 0;
		this.notifyDataSetChanged();
	}

	public void clearPosition() {
		if (selectedPosition != -99) {
			Integer oldPosition = selectedPosition;
			selectedPosition = -99;

			this.notifyItemRangeChanged(0, appDataList0.size());
		}
		/*
		for (int i = 0; i <= appDataList0.size() - 1; i++) {
			this.notifyItemChanged(i);
		}
		*/
		//selectedPosition = oldPosition;

	}

	public int setPosition() {

		this.notifyItemRangeChanged(0, appDataList0.size());

		if (selectedPosition == -99) {
			selectedPosition = 0;
			this.notifyItemChanged(selectedPosition);
		} else {
			Integer oldPosition = selectedPosition;
			selectedPosition++;
			if (selectedPosition > getItemCount() - 1) {
				selectedPosition = 0;
			}

			this.notifyItemChanged(oldPosition);
			this.notifyItemChanged(selectedPosition);

			//linearLayoutManager.scrollToPosition(selectedPosition);
		}
		return selectedPosition;
	}

	public AppData getCurrentApp() {
		return appDataList0.get(selectedPosition);
	}
}