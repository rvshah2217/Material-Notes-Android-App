package com.bijoysingh.quicknote.recyclerview;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bijoysingh.quicknote.R;
import com.bijoysingh.quicknote.activities.CreateSimpleNoteActivity;
import com.bijoysingh.quicknote.activities.MainActivity;
import com.bijoysingh.quicknote.activities.ThemedActivity;
import com.bijoysingh.quicknote.activities.ViewAdvancedNoteActivity;
import com.bijoysingh.quicknote.activities.sheets.EnterPincodeBottomSheet;
import com.bijoysingh.quicknote.activities.sheets.NoteOptionsBottomSheet;
import com.bijoysingh.quicknote.database.Note;
import com.bijoysingh.quicknote.items.NoteRecyclerItem;
import com.bijoysingh.quicknote.items.RecyclerItem;
import com.github.bijoysingh.starter.prefs.DataStore;
import com.github.bijoysingh.starter.recyclerview.RecyclerViewHolder;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class NoteRecyclerHolder extends RecyclerViewHolder<RecyclerItem> {

  private CardView view;
  private TextView timestamp;
  private TextView title;
  private TextView description;
  private ImageView edit;
  private ImageView share;
  private ImageView delete;
  private ImageView copy;
  private ImageView moreOptions;
  private MainActivity activity;

  /**
   * Constructor for the recycler view holder
   *
   * @param context the application/activity context
   * @param view    the view of the current item
   */
  public NoteRecyclerHolder(Context context, View view) {
    super(context, view);
    this.view = (CardView) view;
    timestamp = view.findViewById(R.id.timestamp);
    title = view.findViewById(R.id.title);
    description = view.findViewById(R.id.description);
    share = view.findViewById(R.id.share_button);
    delete = view.findViewById(R.id.delete_button);
    copy = view.findViewById(R.id.copy_button);
    activity = (MainActivity) context;
    moreOptions = view.findViewById(R.id.options_button);
    edit = view.findViewById(R.id.edit_button);
  }

  @Override
  public void populate(RecyclerItem itemData, Bundle extra) {
    NoteRecyclerItem item = (NoteRecyclerItem) itemData;
    final Note data = item.note;
    String noteTitle = data.getTitle();
    title.setText(noteTitle);
    title.setVisibility(noteTitle.isEmpty() ? GONE : VISIBLE);

    description.setText(data.getLockedText());
    timestamp.setText(data.displayTimestamp);

    view.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        actionOrUnlockNote(data, new Runnable() {
          @Override
          public void run() {
            openNote(data);
          }
        });
      }
    });
    view.setCardBackgroundColor(data.color);
    delete.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        actionOrUnlockNote(data, new Runnable() {
          @Override
          public void run() {
            activity.moveItemToTrashOrDelete(data);
          }
        });
      }
    });
    share.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        actionOrUnlockNote(data, new Runnable() {
          @Override
          public void run() {
            data.share(context);
          }
        });
      }
    });
    edit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        data.edit(context);
      }
    });
    copy.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        actionOrUnlockNote(data, new Runnable() {
          @Override
          public void run() {
            data.copy(context);
          }
        });
      }
    });

    moreOptions.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        NoteOptionsBottomSheet.Companion.openSheet(activity, data);
      }
    });
    view.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View view) {
        NoteOptionsBottomSheet.Companion.openSheet(activity, data);
        return false;
      }
    });
  }

  private void actionOrUnlockNote(final Note data, final Runnable runnable) {
    if (context instanceof ThemedActivity && data.locked) {
      EnterPincodeBottomSheet.Companion.openUnlockSheet(
          (ThemedActivity) context,
          new EnterPincodeBottomSheet.PincodeSuccessListener() {
            @Override
            public void onFailure() {
              actionOrUnlockNote(data, runnable);
            }

            @Override
            public void onSuccess() {
              runnable.run();
            }
          },
          DataStore.get(context));
      return;
    } else if (data.locked) {
      return;
    }
    runnable.run();
  }

  private void openNote(final Note data) {
    Intent intent = new Intent(context, ViewAdvancedNoteActivity.class);
    intent.putExtra(CreateSimpleNoteActivity.NOTE_ID, data.uid);
    intent.putExtra(ThemedActivity.Companion.getKey(), ((ThemedActivity) context).isNightMode());
    context.startActivity(intent);
  }
}
