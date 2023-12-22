package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import com.thf.AppSwitcher.R;

public class SwitchAppsAdapter extends RecyclerView.Adapter<SwitchAppsAdapter.MyViewHolder> {
  private static final String TAG = "AppSwitcherService";
  private List<AppDataIcon> appDataList0;
  private static int selectedPosition = 0;
  private Context context;
  private float brightness;
  private boolean grayscaleIcons = false;

  public interface Listener {
    void onItemClick(View item, AppDataIcon app);

    void onTouch();

    void onTitleChanged(String title);
  }

  private final Listener listener;

  public SwitchAppsAdapter(
      Context context, float brightness, boolean grayscaleIcons, Listener listener) {

    this.brightness = brightness;
    this.listener = listener;
    this.context = context;
    this.grayscaleIcons = grayscaleIcons;
  }

  public void setBrightness(float brightness) {
    this.brightness = brightness;
  }

  public void setGrayscaleIcons(boolean grayscaleIcons) {
    this.grayscaleIcons = grayscaleIcons;
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
      AppDataIcon app = appDataList0.get(getAdapterPosition());
      // Toast.makeText(context, getAdapterPosition() + "", Toast.LENGTH_SHORT).show();
      listener.onItemClick(v, app);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        clearPosition();
        v.setBackgroundColor(Color.TRANSPARENT);
        listener.onTouch();
      }
      return false;
    }
  }

  @Override
  public SwitchAppsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_switch, parent, false);
    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    AppDataIcon app = appDataList0.get(position);
    // holder.itemView.setSelected(true);
    holder.name.setText(app.getDescription());

    Drawable icon = app.getIcon();
    if (icon != null) holder.logo.setImageDrawable(icon.mutate());
    holder.logo.getDrawable().clearColorFilter();

    holder.border.setBackground(null);

    ColorMatrix matrix = new ColorMatrix();
    matrix.setSaturation(1f);

    if (position == selectedPosition || selectedPosition == -99) {
      holder.name.setAlpha(1f);
      holder.logo.setAlpha(1f);
      if (position == selectedPosition) {
        listener.onTitleChanged(app.getDescription());
      }
    } else {
      if (grayscaleIcons) matrix.setSaturation(0);
      holder.name.setAlpha(brightness);
      holder.logo.setAlpha(brightness);
    }

    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
    holder.logo.setColorFilter(filter);
  }

  @Override
  public int getItemCount() {
    if (appDataList0 == null) return 0;
    return appDataList0.size();
  }

  public void setItems(List<AppDataIcon> newList) {
    appDataList0 = newList;
    selectedPosition = 0;
    this.notifyDataSetChanged();
  }

  public void clearPosition() {
    if (appDataList0 == null) return;
    if (selectedPosition != -99) {
      Integer oldPosition = selectedPosition;
      selectedPosition = -99;

      this.notifyItemRangeChanged(0, appDataList0.size());
    }
  }

  public Integer setPosition() {

    if (appDataList0 == null) return null;

    // this.notifyItemRangeChanged(0, appDataList0.size());

    if (selectedPosition == -99) {
      selectedPosition = 0;
      this.notifyItemRangeChanged(0, appDataList0.size());
      // this.notifyItemChanged(selectedPosition);
    } else {
      Integer oldPosition = selectedPosition;
      selectedPosition++;
      if (selectedPosition > getItemCount() - 1) {
        selectedPosition = 0;
      }
      this.notifyItemChanged(selectedPosition);
      this.notifyItemChanged(oldPosition);
    }
    return selectedPosition;
  }

  public AppData getCurrentApp() {
    if (appDataList0 == null || appDataList0.size() == 0) {
      return null;
    }

    try {
      return appDataList0.get(selectedPosition);
    } catch (IndexOutOfBoundsException ex) {
      Log.e(TAG, "current application cannot be provided based on index " + selectedPosition);
      return null;
    }
  }
}
