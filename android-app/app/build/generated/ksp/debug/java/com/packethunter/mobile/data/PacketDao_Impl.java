package com.packethunter.mobile.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class PacketDao_Impl implements PacketDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PacketInfo> __insertionAdapterOfPacketInfo;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldPackets;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllPackets;

  public PacketDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPacketInfo = new EntityInsertionAdapter<PacketInfo>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `packets` (`id`,`timestamp`,`protocol`,`sourceIp`,`destIp`,`sourcePort`,`destPort`,`length`,`flags`,`payload`,`payloadPreview`,`sessionId`,`direction`,`httpMethod`,`httpUrl`,`dnsQuery`,`dnsResponse`,`tlsSni`,`tlsCertFingerprint`,`destCountry`,`destCity`,`destLat`,`destLon`,`destAsn`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PacketInfo entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getProtocol());
        statement.bindString(4, entity.getSourceIp());
        statement.bindString(5, entity.getDestIp());
        statement.bindLong(6, entity.getSourcePort());
        statement.bindLong(7, entity.getDestPort());
        statement.bindLong(8, entity.getLength());
        statement.bindString(9, entity.getFlags());
        if (entity.getPayload() == null) {
          statement.bindNull(10);
        } else {
          statement.bindBlob(10, entity.getPayload());
        }
        statement.bindString(11, entity.getPayloadPreview());
        statement.bindString(12, entity.getSessionId());
        statement.bindString(13, entity.getDirection());
        if (entity.getHttpMethod() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getHttpMethod());
        }
        if (entity.getHttpUrl() == null) {
          statement.bindNull(15);
        } else {
          statement.bindString(15, entity.getHttpUrl());
        }
        if (entity.getDnsQuery() == null) {
          statement.bindNull(16);
        } else {
          statement.bindString(16, entity.getDnsQuery());
        }
        if (entity.getDnsResponse() == null) {
          statement.bindNull(17);
        } else {
          statement.bindString(17, entity.getDnsResponse());
        }
        if (entity.getTlsSni() == null) {
          statement.bindNull(18);
        } else {
          statement.bindString(18, entity.getTlsSni());
        }
        if (entity.getTlsCertFingerprint() == null) {
          statement.bindNull(19);
        } else {
          statement.bindString(19, entity.getTlsCertFingerprint());
        }
        if (entity.getDestCountry() == null) {
          statement.bindNull(20);
        } else {
          statement.bindString(20, entity.getDestCountry());
        }
        if (entity.getDestCity() == null) {
          statement.bindNull(21);
        } else {
          statement.bindString(21, entity.getDestCity());
        }
        if (entity.getDestLat() == null) {
          statement.bindNull(22);
        } else {
          statement.bindDouble(22, entity.getDestLat());
        }
        if (entity.getDestLon() == null) {
          statement.bindNull(23);
        } else {
          statement.bindDouble(23, entity.getDestLon());
        }
        if (entity.getDestAsn() == null) {
          statement.bindNull(24);
        } else {
          statement.bindString(24, entity.getDestAsn());
        }
      }
    };
    this.__preparedStmtOfDeleteOldPackets = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM packets WHERE timestamp < ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllPackets = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM packets";
        return _query;
      }
    };
  }

  @Override
  public Object insertPacket(final PacketInfo packet,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPacketInfo.insertAndReturnId(packet);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPackets(final List<PacketInfo> packets,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPacketInfo.insert(packets);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldPackets(final long beforeTime,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldPackets.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, beforeTime);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOldPackets.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllPackets(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllPackets.acquire();
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
          __preparedStmtOfDeleteAllPackets.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PacketInfo>> getRecentPackets(final int limit) {
    final String _sql = "SELECT * FROM packets ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"packets"}, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
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
  public Flow<List<PacketInfo>> getPacketsByProtocol(final String protocol) {
    final String _sql = "SELECT * FROM packets WHERE protocol = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, protocol);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"packets"}, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
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
  public Flow<List<PacketInfo>> getPacketsByIp(final String ip) {
    final String _sql = "SELECT * FROM packets WHERE sourceIp = ? OR destIp = ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, ip);
    _argIndex = 2;
    _statement.bindString(_argIndex, ip);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"packets"}, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
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
  public Object getPacketsBySession(final String sessionId,
      final Continuation<? super List<PacketInfo>> $completion) {
    final String _sql = "SELECT * FROM packets WHERE sessionId = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sessionId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
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
  public Object getPacketsByConnection(final String sourceIp, final int sourcePort,
      final String destIp, final int destPort,
      final Continuation<? super List<PacketInfo>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM packets \n"
            + "        WHERE (sourceIp = ? AND sourcePort = ? AND destIp = ? AND destPort = ?)\n"
            + "           OR (sourceIp = ? AND sourcePort = ? AND destIp = ? AND destPort = ?)\n"
            + "        ORDER BY timestamp ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 8);
    int _argIndex = 1;
    _statement.bindString(_argIndex, sourceIp);
    _argIndex = 2;
    _statement.bindLong(_argIndex, sourcePort);
    _argIndex = 3;
    _statement.bindString(_argIndex, destIp);
    _argIndex = 4;
    _statement.bindLong(_argIndex, destPort);
    _argIndex = 5;
    _statement.bindString(_argIndex, destIp);
    _argIndex = 6;
    _statement.bindLong(_argIndex, destPort);
    _argIndex = 7;
    _statement.bindString(_argIndex, sourceIp);
    _argIndex = 8;
    _statement.bindLong(_argIndex, sourcePort);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
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
  public Flow<List<PacketInfo>> getPacketsInTimeRange(final long startTime, final long endTime) {
    final String _sql = "SELECT * FROM packets WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endTime);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"packets"}, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
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
  public Object getPacketsByIds(final List<Long> ids,
      final Continuation<? super List<PacketInfo>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM packets WHERE id IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (long _item : ids) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PacketInfo>>() {
      @Override
      @NonNull
      public List<PacketInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfProtocol = CursorUtil.getColumnIndexOrThrow(_cursor, "protocol");
          final int _cursorIndexOfSourceIp = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceIp");
          final int _cursorIndexOfDestIp = CursorUtil.getColumnIndexOrThrow(_cursor, "destIp");
          final int _cursorIndexOfSourcePort = CursorUtil.getColumnIndexOrThrow(_cursor, "sourcePort");
          final int _cursorIndexOfDestPort = CursorUtil.getColumnIndexOrThrow(_cursor, "destPort");
          final int _cursorIndexOfLength = CursorUtil.getColumnIndexOrThrow(_cursor, "length");
          final int _cursorIndexOfFlags = CursorUtil.getColumnIndexOrThrow(_cursor, "flags");
          final int _cursorIndexOfPayload = CursorUtil.getColumnIndexOrThrow(_cursor, "payload");
          final int _cursorIndexOfPayloadPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "payloadPreview");
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfDirection = CursorUtil.getColumnIndexOrThrow(_cursor, "direction");
          final int _cursorIndexOfHttpMethod = CursorUtil.getColumnIndexOrThrow(_cursor, "httpMethod");
          final int _cursorIndexOfHttpUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "httpUrl");
          final int _cursorIndexOfDnsQuery = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsQuery");
          final int _cursorIndexOfDnsResponse = CursorUtil.getColumnIndexOrThrow(_cursor, "dnsResponse");
          final int _cursorIndexOfTlsSni = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsSni");
          final int _cursorIndexOfTlsCertFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "tlsCertFingerprint");
          final int _cursorIndexOfDestCountry = CursorUtil.getColumnIndexOrThrow(_cursor, "destCountry");
          final int _cursorIndexOfDestCity = CursorUtil.getColumnIndexOrThrow(_cursor, "destCity");
          final int _cursorIndexOfDestLat = CursorUtil.getColumnIndexOrThrow(_cursor, "destLat");
          final int _cursorIndexOfDestLon = CursorUtil.getColumnIndexOrThrow(_cursor, "destLon");
          final int _cursorIndexOfDestAsn = CursorUtil.getColumnIndexOrThrow(_cursor, "destAsn");
          final List<PacketInfo> _result = new ArrayList<PacketInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PacketInfo _item_1;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final String _tmpSourceIp;
            _tmpSourceIp = _cursor.getString(_cursorIndexOfSourceIp);
            final String _tmpDestIp;
            _tmpDestIp = _cursor.getString(_cursorIndexOfDestIp);
            final int _tmpSourcePort;
            _tmpSourcePort = _cursor.getInt(_cursorIndexOfSourcePort);
            final int _tmpDestPort;
            _tmpDestPort = _cursor.getInt(_cursorIndexOfDestPort);
            final int _tmpLength;
            _tmpLength = _cursor.getInt(_cursorIndexOfLength);
            final String _tmpFlags;
            _tmpFlags = _cursor.getString(_cursorIndexOfFlags);
            final byte[] _tmpPayload;
            if (_cursor.isNull(_cursorIndexOfPayload)) {
              _tmpPayload = null;
            } else {
              _tmpPayload = _cursor.getBlob(_cursorIndexOfPayload);
            }
            final String _tmpPayloadPreview;
            _tmpPayloadPreview = _cursor.getString(_cursorIndexOfPayloadPreview);
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpDirection;
            _tmpDirection = _cursor.getString(_cursorIndexOfDirection);
            final String _tmpHttpMethod;
            if (_cursor.isNull(_cursorIndexOfHttpMethod)) {
              _tmpHttpMethod = null;
            } else {
              _tmpHttpMethod = _cursor.getString(_cursorIndexOfHttpMethod);
            }
            final String _tmpHttpUrl;
            if (_cursor.isNull(_cursorIndexOfHttpUrl)) {
              _tmpHttpUrl = null;
            } else {
              _tmpHttpUrl = _cursor.getString(_cursorIndexOfHttpUrl);
            }
            final String _tmpDnsQuery;
            if (_cursor.isNull(_cursorIndexOfDnsQuery)) {
              _tmpDnsQuery = null;
            } else {
              _tmpDnsQuery = _cursor.getString(_cursorIndexOfDnsQuery);
            }
            final String _tmpDnsResponse;
            if (_cursor.isNull(_cursorIndexOfDnsResponse)) {
              _tmpDnsResponse = null;
            } else {
              _tmpDnsResponse = _cursor.getString(_cursorIndexOfDnsResponse);
            }
            final String _tmpTlsSni;
            if (_cursor.isNull(_cursorIndexOfTlsSni)) {
              _tmpTlsSni = null;
            } else {
              _tmpTlsSni = _cursor.getString(_cursorIndexOfTlsSni);
            }
            final String _tmpTlsCertFingerprint;
            if (_cursor.isNull(_cursorIndexOfTlsCertFingerprint)) {
              _tmpTlsCertFingerprint = null;
            } else {
              _tmpTlsCertFingerprint = _cursor.getString(_cursorIndexOfTlsCertFingerprint);
            }
            final String _tmpDestCountry;
            if (_cursor.isNull(_cursorIndexOfDestCountry)) {
              _tmpDestCountry = null;
            } else {
              _tmpDestCountry = _cursor.getString(_cursorIndexOfDestCountry);
            }
            final String _tmpDestCity;
            if (_cursor.isNull(_cursorIndexOfDestCity)) {
              _tmpDestCity = null;
            } else {
              _tmpDestCity = _cursor.getString(_cursorIndexOfDestCity);
            }
            final Double _tmpDestLat;
            if (_cursor.isNull(_cursorIndexOfDestLat)) {
              _tmpDestLat = null;
            } else {
              _tmpDestLat = _cursor.getDouble(_cursorIndexOfDestLat);
            }
            final Double _tmpDestLon;
            if (_cursor.isNull(_cursorIndexOfDestLon)) {
              _tmpDestLon = null;
            } else {
              _tmpDestLon = _cursor.getDouble(_cursorIndexOfDestLon);
            }
            final String _tmpDestAsn;
            if (_cursor.isNull(_cursorIndexOfDestAsn)) {
              _tmpDestAsn = null;
            } else {
              _tmpDestAsn = _cursor.getString(_cursorIndexOfDestAsn);
            }
            _item_1 = new PacketInfo(_tmpId,_tmpTimestamp,_tmpProtocol,_tmpSourceIp,_tmpDestIp,_tmpSourcePort,_tmpDestPort,_tmpLength,_tmpFlags,_tmpPayload,_tmpPayloadPreview,_tmpSessionId,_tmpDirection,_tmpHttpMethod,_tmpHttpUrl,_tmpDnsQuery,_tmpDnsResponse,_tmpTlsSni,_tmpTlsCertFingerprint,_tmpDestCountry,_tmpDestCity,_tmpDestLat,_tmpDestLon,_tmpDestAsn);
            _result.add(_item_1);
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
  public Object getPacketCount(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COUNT(*) FROM packets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
  public Object getPacketCountSince(final long startTime,
      final Continuation<? super Long> $completion) {
    final String _sql = "SELECT COUNT(*) FROM packets WHERE timestamp >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final long _tmp;
            _tmp = _cursor.getLong(0);
            _result = _tmp;
          } else {
            _result = 0L;
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
  public Object getTotalBytes(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT SUM(length) FROM packets";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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

  @Override
  public Object getTotalBytesSince(final long startTime,
      final Continuation<? super Long> $completion) {
    final String _sql = "SELECT SUM(length) FROM packets WHERE timestamp >= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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

  @Override
  public Object getProtocolDistribution(
      final Continuation<? super List<ProtocolCount>> $completion) {
    final String _sql = "SELECT protocol, COUNT(*) as count FROM packets GROUP BY protocol";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ProtocolCount>>() {
      @Override
      @NonNull
      public List<ProtocolCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfProtocol = 0;
          final int _cursorIndexOfCount = 1;
          final List<ProtocolCount> _result = new ArrayList<ProtocolCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProtocolCount _item;
            final String _tmpProtocol;
            _tmpProtocol = _cursor.getString(_cursorIndexOfProtocol);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            _item = new ProtocolCount(_tmpProtocol,_tmpCount);
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
  public Object getTopTalkers(final int limit,
      final Continuation<? super List<TalkerStats>> $completion) {
    final String _sql = "\n"
            + "        SELECT destIp as ip, COUNT(*) as count, SUM(length) as bytes \n"
            + "        FROM packets \n"
            + "        GROUP BY destIp \n"
            + "        ORDER BY bytes DESC \n"
            + "        LIMIT ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<TalkerStats>>() {
      @Override
      @NonNull
      public List<TalkerStats> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfIp = 0;
          final int _cursorIndexOfCount = 1;
          final int _cursorIndexOfBytes = 2;
          final List<TalkerStats> _result = new ArrayList<TalkerStats>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final TalkerStats _item;
            final String _tmpIp;
            _tmpIp = _cursor.getString(_cursorIndexOfIp);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final long _tmpBytes;
            _tmpBytes = _cursor.getLong(_cursorIndexOfBytes);
            _item = new TalkerStats(_tmpIp,_tmpCount,_tmpBytes);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
