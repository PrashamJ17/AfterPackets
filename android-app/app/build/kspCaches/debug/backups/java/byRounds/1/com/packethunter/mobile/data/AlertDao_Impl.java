package com.packethunter.mobile.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
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
public final class AlertDao_Impl implements AlertDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Alert> __insertionAdapterOfAlert;

  private final EntityDeletionOrUpdateAdapter<Alert> __updateAdapterOfAlert;

  private final SharedSQLiteStatement __preparedStmtOfAcknowledgeAlert;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldAlerts;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllAlerts;

  public AlertDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAlert = new EntityInsertionAdapter<Alert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `alerts` (`id`,`timestamp`,`severity`,`type`,`title`,`description`,`relatedPacketIds`,`acknowledged`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Alert entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getSeverity());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getTitle());
        statement.bindString(6, entity.getDescription());
        statement.bindString(7, entity.getRelatedPacketIds());
        final int _tmp = entity.getAcknowledged() ? 1 : 0;
        statement.bindLong(8, _tmp);
      }
    };
    this.__updateAdapterOfAlert = new EntityDeletionOrUpdateAdapter<Alert>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `alerts` SET `id` = ?,`timestamp` = ?,`severity` = ?,`type` = ?,`title` = ?,`description` = ?,`relatedPacketIds` = ?,`acknowledged` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Alert entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getSeverity());
        statement.bindString(4, entity.getType());
        statement.bindString(5, entity.getTitle());
        statement.bindString(6, entity.getDescription());
        statement.bindString(7, entity.getRelatedPacketIds());
        final int _tmp = entity.getAcknowledged() ? 1 : 0;
        statement.bindLong(8, _tmp);
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfAcknowledgeAlert = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE alerts SET acknowledged = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOldAlerts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM alerts WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllAlerts = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM alerts";
        return _query;
      }
    };
  }

  @Override
  public Object insertAlert(final Alert alert, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAlert.insertAndReturnId(alert);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAlert(final Alert alert, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAlert.handle(alert);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object acknowledgeAlert(final long alertId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAcknowledgeAlert.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, alertId);
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
          __preparedStmtOfAcknowledgeAlert.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldAlerts(final long beforeTime,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldAlerts.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, beforeTime);
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
          __preparedStmtOfDeleteOldAlerts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllAlerts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllAlerts.acquire();
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
          __preparedStmtOfDeleteAllAlerts.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Alert>> getAllAlerts() {
    final String _sql = "SELECT * FROM alerts ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"alerts"}, new Callable<List<Alert>>() {
      @Override
      @NonNull
      public List<Alert> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRelatedPacketIds = CursorUtil.getColumnIndexOrThrow(_cursor, "relatedPacketIds");
          final int _cursorIndexOfAcknowledged = CursorUtil.getColumnIndexOrThrow(_cursor, "acknowledged");
          final List<Alert> _result = new ArrayList<Alert>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Alert _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpRelatedPacketIds;
            _tmpRelatedPacketIds = _cursor.getString(_cursorIndexOfRelatedPacketIds);
            final boolean _tmpAcknowledged;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAcknowledged);
            _tmpAcknowledged = _tmp != 0;
            _item = new Alert(_tmpId,_tmpTimestamp,_tmpSeverity,_tmpType,_tmpTitle,_tmpDescription,_tmpRelatedPacketIds,_tmpAcknowledged);
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
  public Flow<List<Alert>> getUnacknowledgedAlerts() {
    final String _sql = "SELECT * FROM alerts WHERE acknowledged = 0 ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"alerts"}, new Callable<List<Alert>>() {
      @Override
      @NonNull
      public List<Alert> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfRelatedPacketIds = CursorUtil.getColumnIndexOrThrow(_cursor, "relatedPacketIds");
          final int _cursorIndexOfAcknowledged = CursorUtil.getColumnIndexOrThrow(_cursor, "acknowledged");
          final List<Alert> _result = new ArrayList<Alert>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Alert _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpRelatedPacketIds;
            _tmpRelatedPacketIds = _cursor.getString(_cursorIndexOfRelatedPacketIds);
            final boolean _tmpAcknowledged;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfAcknowledged);
            _tmpAcknowledged = _tmp != 0;
            _item = new Alert(_tmpId,_tmpTimestamp,_tmpSeverity,_tmpType,_tmpTitle,_tmpDescription,_tmpRelatedPacketIds,_tmpAcknowledged);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
