package com.packethunter.mobile.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FilterPresetDao_Impl implements FilterPresetDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<FilterPreset> __insertionAdapterOfFilterPreset;

  private final EntityDeletionOrUpdateAdapter<FilterPreset> __deletionAdapterOfFilterPreset;

  private final EntityDeletionOrUpdateAdapter<FilterPreset> __updateAdapterOfFilterPreset;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastUsed;

  public FilterPresetDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFilterPreset = new EntityInsertionAdapter<FilterPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `filter_presets` (`id`,`name`,`securityFilters`,`rules`,`createdAt`,`lastUsed`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilterPreset entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getSecurityFilters());
        statement.bindString(4, entity.getRules());
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getLastUsed());
      }
    };
    this.__deletionAdapterOfFilterPreset = new EntityDeletionOrUpdateAdapter<FilterPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `filter_presets` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilterPreset entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfFilterPreset = new EntityDeletionOrUpdateAdapter<FilterPreset>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `filter_presets` SET `id` = ?,`name` = ?,`securityFilters` = ?,`rules` = ?,`createdAt` = ?,`lastUsed` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final FilterPreset entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getSecurityFilters());
        statement.bindString(4, entity.getRules());
        statement.bindLong(5, entity.getCreatedAt());
        statement.bindLong(6, entity.getLastUsed());
        statement.bindLong(7, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateLastUsed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE filter_presets SET lastUsed = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPreset(final FilterPreset preset,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfFilterPreset.insertAndReturnId(preset);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePreset(final FilterPreset preset,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfFilterPreset.handle(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePreset(final FilterPreset preset,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfFilterPreset.handle(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastUsed(final long id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastUsed.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLastUsed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<FilterPreset>> getAllPresets() {
    final String _sql = "SELECT * FROM filter_presets ORDER BY lastUsed DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"filter_presets"}, new Callable<List<FilterPreset>>() {
      @Override
      @NonNull
      public List<FilterPreset> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSecurityFilters = CursorUtil.getColumnIndexOrThrow(_cursor, "securityFilters");
          final int _cursorIndexOfRules = CursorUtil.getColumnIndexOrThrow(_cursor, "rules");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsed");
          final List<FilterPreset> _result = new ArrayList<FilterPreset>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final FilterPreset _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSecurityFilters;
            _tmpSecurityFilters = _cursor.getString(_cursorIndexOfSecurityFilters);
            final String _tmpRules;
            _tmpRules = _cursor.getString(_cursorIndexOfRules);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastUsed;
            _tmpLastUsed = _cursor.getLong(_cursorIndexOfLastUsed);
            _item = new FilterPreset(_tmpId,_tmpName,_tmpSecurityFilters,_tmpRules,_tmpCreatedAt,_tmpLastUsed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPresetById(final long id, final Continuation<? super FilterPreset> $completion) {
    final String _sql = "SELECT * FROM filter_presets WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<FilterPreset>() {
      @Override
      @Nullable
      public FilterPreset call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfSecurityFilters = CursorUtil.getColumnIndexOrThrow(_cursor, "securityFilters");
          final int _cursorIndexOfRules = CursorUtil.getColumnIndexOrThrow(_cursor, "rules");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastUsed = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUsed");
          final FilterPreset _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpSecurityFilters;
            _tmpSecurityFilters = _cursor.getString(_cursorIndexOfSecurityFilters);
            final String _tmpRules;
            _tmpRules = _cursor.getString(_cursorIndexOfRules);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpLastUsed;
            _tmpLastUsed = _cursor.getLong(_cursorIndexOfLastUsed);
            _result = new FilterPreset(_tmpId,_tmpName,_tmpSecurityFilters,_tmpRules,_tmpCreatedAt,_tmpLastUsed);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
