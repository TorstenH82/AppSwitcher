package com.thf.AppSwitcher.utils;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.content.Intent;
import android.view.MotionEvent;
import android.widget.ImageButton;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.thf.AppSwitcher.utils.SharedPreferencesHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Hashtable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.thf.AppSwitcher.R;
import com.thf.AppSwitcher.utils.RecyclerViewAdapter.MyViewHolder;
import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
  private static final String TAG = "AppSwitcherService";
  private Context context;
  private SharedPreferencesHelper sharedPreferencesHelper;
  private ItemTouchHelper touchHelper;
  private boolean apps;
  private String list;
  private List<AppData> selectedList = new ArrayList<>();
  private List<AppData> appDataList0;
  private List<AppData> appDataList0All;

  class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    TextView name;
    TextView packageName;
    ImageView logo, expand;
    CheckBox relevant;
    ImageButton reorder;

    MyViewHolder(View v) {
      super(v);
      v.setOnClickListener(this);

      relevant = v.findViewById(R.id.relevant);
      relevant.setOnClickListener(this);
      reorder = v.findViewById(R.id.reorderButton);
      name = v.findViewById(R.id.itemName);
      packageName = v.findViewById(R.id.itemPackageName);
      logo = v.findViewById(R.id.itemLogo);
      expand = v.findViewById(R.id.itemExpand);
      expand.setOnClickListener(this);

      if (apps) expand.setVisibility(View.GONE);
      if ("sort".equals(list)) {
        expand.setVisibility(View.GONE);
        relevant.setVisibility(View.GONE);
        reorder.setVisibility(View.VISIBLE);
      }
    }

    @Override
    public void onClick(View v) {
      AppData app = appDataList0.get(getAdapterPosition());
      String key = app.getKey();

      switch (v.getId()) {
        case R.id.itemExpand:
          expandCollapseList(app.getPackageName());
          break;
        case R.id.relevant:
          CheckBox checkBox = (CheckBox) v;
          app.setList(list);
          if (checkBox.isChecked()) {
            selectedList.add(app);
            app.setSort(9999);
            sharedPreferencesHelper.putIntoList(app, "selected");
          } else {
            selectedList.remove(app);
            sharedPreferencesHelper.removeFromList(app, "selected");
          }
          break;
        default:
          if (!apps) {
            ComponentName name =
                new ComponentName(
                    appDataList0.get(getAdapterPosition()).getPackageName(),
                    appDataList0.get(getAdapterPosition()).getActivityName());
            Intent i = new Intent(Intent.ACTION_MAIN);

            // i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            i.setComponent(name);
            try {
              context.startActivity(i);
            } catch (Exception ex) {
              Log.e(TAG, "Error starting activity: " + ex.getMessage());
            }
          } else {
            Intent i =
                context
                    .getPackageManager()
                    .getLaunchIntentForPackage(
                        appDataList0.get(getAdapterPosition()).getPackageName());
            if (i != null) {
              try {
                context.startActivity(i);
              } catch (Exception ex) {
                Log.e(TAG, "Error starting activity: " + ex.getMessage());
              }
            }
          }
      }
    }
  }

  public void setTouchHelper(ItemTouchHelper touchHelper) {
    this.touchHelper = touchHelper;
  }

  public RecyclerViewAdapter(
      List<AppData> appDataList,
      Context context,
      boolean apps,
      String list,
      List<AppData> selectedList) {
    this.context = context;
    this.sharedPreferencesHelper = new SharedPreferencesHelper(context);
    this.appDataList0 = appDataList;
    this.apps = apps;
    this.list = list;
    this.selectedList = selectedList;
  }

  @Override
  public RecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view, parent, false);
    return new MyViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(MyViewHolder holder, int position) {
    AppData app = appDataList0.get(position);
    String key = app.getKey();

    holder.name.setText(app.getName());

    if (touchHelper != null) {
      holder.reorder.setOnTouchListener(
          new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
              if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder);
              }
              return false;
            }
          });
    }

    Drawable icon = app.getIcon(context);
    if (icon != null) {
      holder.logo.setImageDrawable(icon);
    }

    if (!apps) {
      if ("app".equals(app.getCategory())) {
        holder.name.setText(app.getName());
        holder.packageName.setText(app.getPackageName());

        if (!"sort".equals(list)) holder.expand.setVisibility(View.VISIBLE);

        if (htExpanded.containsKey(app.getPackageName())) {
          holder.expand.setImageResource(R.drawable.up_up);
        } else {
          holder.expand.setImageResource(R.drawable.down_up);
        }

      } else {
        holder.name.setText(app.getFullDescription());
        holder.packageName.setText(app.getPackageName() + "/" + app.getActivityName());
        holder.expand.setVisibility(View.GONE);
      }

    } else {
      holder.name.setText(app.getName());
      holder.packageName.setText(app.getPackageName());
    }

    holder.relevant.setChecked(selectedList.indexOf(app) != -1);
  }

  @Override
  public int getItemCount() {
    return appDataList0.size();
  }

  public void onRowMoved(int fromPosition, int toPosition) {
    if (fromPosition < toPosition) {
      for (int i = fromPosition; i < toPosition; i++) {
        Collections.swap(appDataList0, i, i + 1);
      }
    } else {
      for (int i = fromPosition; i > toPosition; i--) {
        Collections.swap(appDataList0, i, i - 1);
      }
    }
    notifyItemMoved(fromPosition, toPosition);
  }

  public void onRowSelected(MyViewHolder myViewHolder) {
    myViewHolder.reorder.setSelected(true);
    // myViewHolder.itemView.setBackgroundColor(Color.GRAY);
  }

  public void onRowClear(MyViewHolder myViewHolder) {
    // myViewHolder.itemView.setBackgroundColor(Color.WHITE);
    myViewHolder.reorder.setSelected(false);
    int sort = -1;
    selectedList.clear();
    for (AppData app : appDataList0) {
      sort++;
      app.setSort(sort);
      selectedList.add(app);
    }
    sharedPreferencesHelper.saveList(selectedList, "selected");
  }

  private Hashtable<String, Boolean> htExpanded;

  public void expandCollapseList(String packageName) {

    if (htExpanded.containsKey(packageName)) {
      htExpanded.remove(packageName);
    } else {
      htExpanded.put(packageName, true);
    }
    setItems(appDataList0All);
  }

  public void setItems(List<AppData> newList) {

    if (htExpanded == null) {
      htExpanded = new Hashtable<String, Boolean>();

      Iterator<AppData> i = selectedList.iterator();
      while (i.hasNext()) {
        AppData s = i.next(); // must be called before you can call i.remove()
        if (!htExpanded.containsKey(s.getPackageName())
            && !TextUtils.equals(s.getCategory(), "app")) {
          htExpanded.put(s.getPackageName(), true);
        }
      }
    }

    appDataList0All = new ArrayList<>(newList);

    appDataList0.clear();
    Iterator<AppData> i = appDataList0All.iterator();
    while (i.hasNext()) {
      AppData s = i.next(); // must be called before you can call i.remove()
      if (s.getCategory() == "app"
          || "sort".equals(list)
          || htExpanded.containsKey(s.getPackageName())) {
        appDataList0.add(s);
      }
    }
    this.notifyDataSetChanged();
  }
}
