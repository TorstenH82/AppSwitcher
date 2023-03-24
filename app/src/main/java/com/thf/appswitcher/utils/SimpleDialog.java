package com.thf.AppSwitcher.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.view.ContextThemeWrapper;
import androidx.appcompat.app.AlertDialog;
import com.thf.AppSwitcher.R;
import java.util.List;

public class SimpleDialog {
  private Context context;
  private String title;
  private String message;
  private boolean showNegative;
  private String reference;

  public SimpleDialog(
      String reference,
      Context context,
      SimpleDialogCallbacks listener,
      String title,
      String message,
      boolean showNegative) {
    this.context = context;
    this.reference = reference;
    this.title = title;
    this.message = message;
    this.showNegative = showNegative;
    this.listener = listener;
  }

  public SimpleDialog(Context context, String title, String message) {
    this.context = context;
    this.title = title;
    this.message = message;
    this.showNegative = false;
  }

  private SimpleDialogCallbacks listener;

  public interface SimpleDialogCallbacks {
    public void onClick(boolean positive, String reference);
  }

  public void show() {
    AlertDialog.Builder alertDialog =
        new AlertDialog.Builder(new ContextThemeWrapper(this.context, R.style.AlertDialogCustom))
            .setTitle(this.title)
            .setMessage(this.message)

            // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton(
                R.string.yes,
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int which) {
                    if (listener != null) listener.onClick(true, reference);
                  }
                })
            .setIcon(R.mipmap.ic_launcher);

    if (showNegative) {
      alertDialog.setNegativeButton(
          R.string.no,
          new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              if (listener != null) listener.onClick(false, reference);
            }
          });
    }

    alertDialog.show();
  }
}
