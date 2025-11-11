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
public final class RuleDao_Impl implements RuleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DetectionRule> __insertionAdapterOfDetectionRule;

  private final EntityDeletionOrUpdateAdapter<DetectionRule> __deletionAdapterOfDetectionRule;

  private final EntityDeletionOrUpdateAdapter<DetectionRule> __updateAdapterOfDetectionRule;

  private final SharedSQLiteStatement __preparedStmtOfSetRuleEnabled;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllRules;

  public RuleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDetectionRule = new EntityInsertionAdapter<DetectionRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `rules` (`id`,`name`,`enabled`,`metric`,`condition`,`threshold`,`timeWindowSeconds`,`action`,`severity`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DetectionRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getMetric());
        statement.bindString(5, entity.getCondition());
        statement.bindDouble(6, entity.getThreshold());
        statement.bindLong(7, entity.getTimeWindowSeconds());
        statement.bindString(8, entity.getAction());
        statement.bindString(9, entity.getSeverity());
      }
    };
    this.__deletionAdapterOfDetectionRule = new EntityDeletionOrUpdateAdapter<DetectionRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `rules` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DetectionRule entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfDetectionRule = new EntityDeletionOrUpdateAdapter<DetectionRule>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `rules` SET `id` = ?,`name` = ?,`enabled` = ?,`metric` = ?,`condition` = ?,`threshold` = ?,`timeWindowSeconds` = ?,`action` = ?,`severity` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DetectionRule entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        final int _tmp = entity.getEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        statement.bindString(4, entity.getMetric());
        statement.bindString(5, entity.getCondition());
        statement.bindDouble(6, entity.getThreshold());
        statement.bindLong(7, entity.getTimeWindowSeconds());
        statement.bindString(8, entity.getAction());
        statement.bindString(9, entity.getSeverity());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfSetRuleEnabled = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE rules SET enabled = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllRules = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM rules";
        return _query;
      }
    };
  }

  @Override
  public Object insertRule(final DetectionRule rule, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfDetectionRule.insertAndReturnId(rule);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteRule(final DetectionRule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDetectionRule.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateRule(final DetectionRule rule, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDetectionRule.handle(rule);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setRuleEnabled(final long ruleId, final boolean enabled,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetRuleEnabled.acquire();
        int _argIndex = 1;
        final int _tmp = enabled ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, ruleId);
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
          __preparedStmtOfSetRuleEnabled.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllRules(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllRules.acquire();
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
          __preparedStmtOfDeleteAllRules.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DetectionRule>> getAllRules() {
    final String _sql = "SELECT * FROM rules ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"rules"}, new Callable<List<DetectionRule>>() {
      @Override
      @NonNull
      public List<DetectionRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfMetric = CursorUtil.getColumnIndexOrThrow(_cursor, "metric");
          final int _cursorIndexOfCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "condition");
          final int _cursorIndexOfThreshold = CursorUtil.getColumnIndexOrThrow(_cursor, "threshold");
          final int _cursorIndexOfTimeWindowSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "timeWindowSeconds");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final List<DetectionRule> _result = new ArrayList<DetectionRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DetectionRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final String _tmpMetric;
            _tmpMetric = _cursor.getString(_cursorIndexOfMetric);
            final String _tmpCondition;
            _tmpCondition = _cursor.getString(_cursorIndexOfCondition);
            final double _tmpThreshold;
            _tmpThreshold = _cursor.getDouble(_cursorIndexOfThreshold);
            final int _tmpTimeWindowSeconds;
            _tmpTimeWindowSeconds = _cursor.getInt(_cursorIndexOfTimeWindowSeconds);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            _item = new DetectionRule(_tmpId,_tmpName,_tmpEnabled,_tmpMetric,_tmpCondition,_tmpThreshold,_tmpTimeWindowSeconds,_tmpAction,_tmpSeverity);
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
  public Object getEnabledRules(final Continuation<? super List<DetectionRule>> $completion) {
    final String _sql = "SELECT * FROM rules WHERE enabled = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DetectionRule>>() {
      @Override
      @NonNull
      public List<DetectionRule> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfMetric = CursorUtil.getColumnIndexOrThrow(_cursor, "metric");
          final int _cursorIndexOfCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "condition");
          final int _cursorIndexOfThreshold = CursorUtil.getColumnIndexOrThrow(_cursor, "threshold");
          final int _cursorIndexOfTimeWindowSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "timeWindowSeconds");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final List<DetectionRule> _result = new ArrayList<DetectionRule>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DetectionRule _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final String _tmpMetric;
            _tmpMetric = _cursor.getString(_cursorIndexOfMetric);
            final String _tmpCondition;
            _tmpCondition = _cursor.getString(_cursorIndexOfCondition);
            final double _tmpThreshold;
            _tmpThreshold = _cursor.getDouble(_cursorIndexOfThreshold);
            final int _tmpTimeWindowSeconds;
            _tmpTimeWindowSeconds = _cursor.getInt(_cursorIndexOfTimeWindowSeconds);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            _item = new DetectionRule(_tmpId,_tmpName,_tmpEnabled,_tmpMetric,_tmpCondition,_tmpThreshold,_tmpTimeWindowSeconds,_tmpAction,_tmpSeverity);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRuleById(final long ruleId,
      final Continuation<? super DetectionRule> $completion) {
    final String _sql = "SELECT * FROM rules WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, ruleId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DetectionRule>() {
      @Override
      @Nullable
      public DetectionRule call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "enabled");
          final int _cursorIndexOfMetric = CursorUtil.getColumnIndexOrThrow(_cursor, "metric");
          final int _cursorIndexOfCondition = CursorUtil.getColumnIndexOrThrow(_cursor, "condition");
          final int _cursorIndexOfThreshold = CursorUtil.getColumnIndexOrThrow(_cursor, "threshold");
          final int _cursorIndexOfTimeWindowSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "timeWindowSeconds");
          final int _cursorIndexOfAction = CursorUtil.getColumnIndexOrThrow(_cursor, "action");
          final int _cursorIndexOfSeverity = CursorUtil.getColumnIndexOrThrow(_cursor, "severity");
          final DetectionRule _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final boolean _tmpEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnabled);
            _tmpEnabled = _tmp != 0;
            final String _tmpMetric;
            _tmpMetric = _cursor.getString(_cursorIndexOfMetric);
            final String _tmpCondition;
            _tmpCondition = _cursor.getString(_cursorIndexOfCondition);
            final double _tmpThreshold;
            _tmpThreshold = _cursor.getDouble(_cursorIndexOfThreshold);
            final int _tmpTimeWindowSeconds;
            _tmpTimeWindowSeconds = _cursor.getInt(_cursorIndexOfTimeWindowSeconds);
            final String _tmpAction;
            _tmpAction = _cursor.getString(_cursorIndexOfAction);
            final String _tmpSeverity;
            _tmpSeverity = _cursor.getString(_cursorIndexOfSeverity);
            _result = new DetectionRule(_tmpId,_tmpName,_tmpEnabled,_tmpMetric,_tmpCondition,_tmpThreshold,_tmpTimeWindowSeconds,_tmpAction,_tmpSeverity);
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
