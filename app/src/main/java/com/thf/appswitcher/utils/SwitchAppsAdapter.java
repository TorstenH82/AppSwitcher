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
import android.widget.LinearLayout;
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
  // private Station currentStation;
  private Context context;
  private float brightness;
  private boolean grayscaleIcons = false;
  private ColorMatrix cmInactive = new ColorMatrix();
  private ColorMatrix cmActive = new ColorMatrix();
  private ColorMatrixColorFilter cmfInactive;
  private ColorMatrixColorFilter cmfActive;

  public interface Listener {
    void onItemClick(View item, AppData app);

    void onTouch();

    void onTitleChanged(String title);
  }

  private final Listener listener;

  public SwitchAppsAdapter(
      Context context,
      float brightness,
      boolean grayscaleIcons,
      Listener listener) {
    
    this.brightness = brightness;
    this.listener = listener;
    this.context = context;
    this.grayscaleIcons = grayscaleIcons;

    if (grayscaleIcons) cmInactive.setSaturation(0);
    // cmActive.setSaturation(1);
    cmfInactive = new ColorMatrixColorFilter(cmInactive);
    cmfActive = new ColorMatrixColorFilter(cmActive);
  }

  public void setBrightness(float brightness) {
    this.brightness = brightness;
  }

  public void setGrayscaleIcons(boolean grayscaleIcons) {
    this.grayscaleIcons = grayscaleIcons;
    if (grayscaleIcons) {
      cmInactive.setSaturation(0);
    } else {
      cmInactive.setSaturation(1);
    }
    cmfInactive = new ColorMatrixColorFilter(cmInactive);
    this.notifyDataSetChanged();
  }

  class MyViewHolder extends RecyclerView.ViewHolder
      implements View.OnClickListener, View.OnTouchListener {

    TextView name;
    ImageView logo;
    LinearLayout border;

    MyViewHolder(View v) {
      super(v);
      v.setOnClickListener(this);
      v.setOnTouchListener(this);
      logo = v.findViewById(R.id.itemLogo);
      name = v.findViewById(R.id.itemName);
      border = v.findViewById(R.id.border);
    }

    @Override
    public void onClick(View v) {
      v.setBackgroundColor(Color.TRANSPARENT);
      AppData app = appDataList0.get(getAdapterPosition());
      // Toast.makeText(context, getAdapterPosition() + "", Toast.LENGTH_SHORT).show();
      listener.onItemClick(v, app);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {

        // myBomb.disarm();
        clearPosition();
        // settingsSel.setVisibility(View.VISIBLE);
        v.setBackgroundColor(Color.TRANSPARENT);
        listener.onTouch();
      }
      return false;
    }
  }

  // MyAdapter(List<AppData> appDataList) {
  //		appDataList0 = appDataList;
  //	}

  @Override
  public SwitchAppsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_switch, parent, false);
    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    AppData app = appDataList0.get(position);
    // holder.itemView.setSelected(true);

    holder.name.setText(app.getDescription());

    Drawable icon = app.getIcon(context);
    icon.mutate();
    if (icon != null) holder.logo.setImageDrawable(icon);

    holder.border.setBackground(null);

    if (position == selectedPosition || selectedPosition == -99) {
      if (icon != null) icon.setColorFilter(cmfActive);
      // holder.logo.setImageDrawable(icon);

      holder.name.setAlpha(1f);
      holder.logo.setAlpha(1f);

      if (position == selectedPosition) {
        listener.onTitleChanged(app.getDescription());
        // txtTitle.setText(app.getDescription());
        /*
        if (grayscaleIcons)
          holder.border.setBackground(context.getDrawable(R.drawable.round_rect_shape));
        */
      }
    } else {
      holder.name.setAlpha(brightness);
      holder.logo.setAlpha(brightness);
      icon.setColorFilter(cmfInactive);
    }
  }

  @Override
  public int getItemCount() {
    if (appDataList0 == null) return 0;
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
    // selectedPosition = oldPosition;

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

      // linearLayoutManager.scrollToPosition(selectedPosition);
    }
    return selectedPosition;
  }

  public AppData getCurrentApp() {
    return appDataList0.get(selectedPosition);
  }
}
