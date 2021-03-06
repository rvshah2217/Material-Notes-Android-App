package com.bijoysingh.quicknote.database;

import android.app.Activity;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.content.Context;
import android.content.Intent;

import com.bijoysingh.quicknote.FloatingNoteService;
import com.bijoysingh.quicknote.R;
import com.bijoysingh.quicknote.activities.CreateOrEditAdvancedNoteActivity;
import com.bijoysingh.quicknote.activities.CreateSimpleNoteActivity;
import com.bijoysingh.quicknote.activities.ThemedActivity;
import com.bijoysingh.quicknote.activities.external.ExportableNote;
import com.bijoysingh.quicknote.activities.sheets.EnterPincodeBottomSheet;
import com.bijoysingh.quicknote.formats.Format;
import com.bijoysingh.quicknote.formats.FormatType;
import com.bijoysingh.quicknote.formats.NoteType;
import com.bijoysingh.quicknote.utils.NoteState;
import com.github.bijoysingh.starter.prefs.DataStore;
import com.github.bijoysingh.starter.util.DateFormatter;
import com.github.bijoysingh.starter.util.IntentUtils;
import com.github.bijoysingh.starter.util.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.bijoysingh.quicknote.activities.external.ExportNotesKt.searchInNote;

@Entity(
    tableName = "note",
    indices = {@Index("uid")}
)
public class Note {
  @PrimaryKey(autoGenerate = true)
  public Integer uid;

  public String title;

  public String description;

  public String displayTimestamp;

  public Long timestamp;

  public Integer color;

  public String state;

  public boolean locked;

  public boolean isUnsaved() {
    return uid == null || uid == 0;
  }

  public String getText() {
    String text = "";
    List<Format> formats = Format.getFormats(description);
    for (Format format : formats) {
      if (format.formatType == FormatType.HEADING) {
        continue;
      } else if (format.formatType == FormatType.CHECKLIST_CHECKED) {
        text += "\u2611 ";
      } else if (format.formatType == FormatType.CHECKLIST_UNCHECKED) {
        text += "\u2610 ";
      }
      text += format.text + "\n";
    }
    return text.trim();
  }

  public String getLockedText() {
    if (locked) {
      return "******************\n***********\n****************";
    }
    return getText();
  }

  public NoteState getNoteState() {
    try {
      return NoteState.valueOf(state);
    } catch (Exception exception) {
      return NoteState.DEFAULT;
    }
  }

  public String getTitle() {
    List<Format> formats = Format.getFormats(description);
    if (!formats.isEmpty() && formats.get(0).formatType == FormatType.HEADING) {
      return formats.get(0).text;
    }
    return "";
  }

  public boolean search(String keywords) {
    return searchInNote(this, keywords);
  }

  public void save(Context context) {
    long id = Note.db(context).insertNote(this);
    uid = isUnsaved() ? ((int) id) : uid;
  }

  public void delete(Context context) {
    if (isUnsaved()) {
      return;
    }
    Note.db(context).delete(this);
    description = Format.getNote(new ArrayList<Format>());
    uid = 0;
  }

  public void mark(Context context, NoteState noteState) {
    state = noteState.name();
    save(context);
  }

  public void share(Context context) {
    new IntentUtils.ShareBuilder(context)
        .setSubject(getTitle())
        .setText(getText())
        .setChooserText(context.getString(R.string.share_using))
        .share();
  }

  public void copy(Context context) {
    TextUtils.copyToClipboard(context, getText());
  }

  public void popup(Activity activity) {
    FloatingNoteService.openNote(activity, this, true);
  }

  public Intent editIntent(Context context) {
    Intent intent = new Intent(context, CreateOrEditAdvancedNoteActivity.class);
    intent.putExtra(CreateSimpleNoteActivity.NOTE_ID, uid);
    return intent;
  }

  public void edit(final Context context) {
    if (context instanceof ThemedActivity && locked) {
      EnterPincodeBottomSheet.Companion.openUnlockSheet(
          (ThemedActivity) context,
          new EnterPincodeBottomSheet.PincodeSuccessListener() {
            @Override
            public void onFailure() {
              edit(context);
            }

            @Override
            public void onSuccess() {
              context.startActivity(editIntent(context));
            }
          },
          DataStore.get(context));
      return;
    } else if (locked) {
      return;
    }
    context.startActivity(editIntent(context));
  }

  public void edit(Context context, boolean nightMode) {
    Intent intent = editIntent(context);
    intent.putExtra(ThemedActivity.Companion.getKey(), nightMode);
    context.startActivity(intent);
  }

  public List<Format> getFormats() {
    return Format.getFormats(description);
  }

  public static NoteDao db(Context context) {
    return AppDatabase.getDatabase(context).notes();
  }

  public static Note gen() {
    Note note = new Note();
    note.state = NoteState.DEFAULT.name();
    note.timestamp = Calendar.getInstance().getTimeInMillis();
    note.displayTimestamp = DateFormatter.getDate(Calendar.getInstance());
    note.color = 0xFF00796B;
    return note;
  }

  public static Note gen(ExportableNote exportableNote) {
    Note note = Note.gen();
    note.title = exportableNote.getTitle();
    note.color = exportableNote.getColor();
    note.description = exportableNote.getDescription();
    note.displayTimestamp = exportableNote.getDisplayTimestamp();
    note.timestamp = exportableNote.getTimestamp();
    return note;
  }

  public static Note gen(String title, String description) {
    Note note = Note.gen();
    List<Format> formats = new ArrayList<>();
    if (TextUtils.isNullOrEmpty(title)) {
      formats.add(new Format(FormatType.HEADING, title));
    }
    formats.add(new Format(FormatType.TEXT, description));
    note.title = NoteType.NOTE.name();
    note.description = Format.getNote(formats);
    return note;
  }
}
