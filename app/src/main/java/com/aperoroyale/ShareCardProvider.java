package com.aperoroyale;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Grants read-only access to one locally generated poster selected by the user. */
public final class ShareCardProvider extends ContentProvider {
  @Override public boolean onCreate() { return true; }

  @Override public String getType(Uri uri) { return "image/png"; }

  @Override public ParcelFileDescriptor openFile(Uri uri, String mode)
      throws FileNotFoundException {
    String name = uri.getLastPathSegment();
    if (!"r".equals(mode) || name == null || !name.matches("round-[0-9]+\\.png"))
      throw new FileNotFoundException();
    File file = new File(new File(getContext().getCacheDir(), "share-cards"), name);
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
  }

  @Override public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    String name = uri.getLastPathSegment();
    if (name == null || !name.matches("round-[0-9]+\\.png")) return null;
    File file = new File(new File(getContext().getCacheDir(), "share-cards"), name);
    String[] columns = projection == null
        ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
    MatrixCursor cursor = new MatrixCursor(columns, 1);
    Object[] values = new Object[columns.length];
    for (int i = 0; i < columns.length; i++) {
      if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = name;
      if (OpenableColumns.SIZE.equals(columns[i])) values[i] = file.length();
    }
    cursor.addRow(values);
    return cursor;
  }
  @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
  @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
    throw new UnsupportedOperationException();
  }
  @Override public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) { throw new UnsupportedOperationException(); }
}
