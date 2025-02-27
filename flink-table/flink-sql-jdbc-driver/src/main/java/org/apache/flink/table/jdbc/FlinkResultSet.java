/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.jdbc;

import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.client.gateway.StatementResult;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.jdbc.utils.DataConverter;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.DecimalType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.Date;
import java.sql.ResultSetMetaData;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import static org.apache.flink.table.jdbc.utils.DriverUtils.checkNotNull;

/**
 * ResultSet for flink jdbc driver. Only Batch Mode queries are supported. If you force to submit
 * streaming queries, you may get unrecognized updates, deletions and other results.
 */
public class FlinkResultSet extends BaseResultSet {
    private final List<DataType> dataTypeList;
    private final List<String> columnNameList;
    private final Statement statement;
    private final StatementResult result;
    private final DataConverter dataConverter;
    private RowData currentRow;
    private boolean wasNull;

    private volatile boolean closed;

    public FlinkResultSet(
            Statement statement, StatementResult result, DataConverter dataConverter) {
        this.statement = checkNotNull(statement, "Statement cannot be null");
        this.result = checkNotNull(result, "Statement result cannot be null");
        this.dataConverter = checkNotNull(dataConverter, "Data converter cannot be null");
        this.currentRow = null;
        this.wasNull = false;

        final ResolvedSchema schema = result.getResultSchema();
        this.dataTypeList = schema.getColumnDataTypes();
        this.columnNameList = schema.getColumnNames();
    }

    @Override
    public boolean next() throws SQLException {
        checkClosed();

        if (result.hasNext()) {
            // TODO check the kind of currentRow
            currentRow = result.next();
            wasNull = currentRow == null;
            return true;
        } else {
            return false;
        }
    }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException("This result set is already closed");
        }
    }

    private void checkValidRow() throws SQLException {
        if (currentRow == null) {
            throw new SQLException("Not on a valid row");
        }
        if (currentRow.getArity() <= 0) {
            throw new SQLException("Empty row with no data");
        }
    }

    private void checkValidColumn(int columnIndex) throws SQLException {
        if (columnIndex <= 0) {
            throw new SQLException(
                    String.format("Column index[%s] must be positive.", columnIndex));
        }

        final int columnCount = currentRow.getArity();
        if (columnIndex > columnCount) {
            throw new SQLException(
                    String.format(
                            "Column index %s out of bound. There are only %s columns.",
                            columnIndex, columnCount));
        }
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;

        result.close();
    }

    @Override
    public boolean wasNull() throws SQLException {
        checkClosed();

        return wasNull;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);

        try {
            return dataConverter.getString(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getBoolean(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getByte(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getShort(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getInt(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);

        try {
            return dataConverter.getLong(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getFloat(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getDouble(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return getBigDecimal(columnIndex).setScale(scale, RoundingMode.HALF_EVEN);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);
        try {
            return dataConverter.getBinary(currentRow, columnIndex - 1);
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        // TODO support date data
        throw new IllegalArgumentException();
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        // TODO support time data
        throw new IllegalArgumentException();
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        // TODO support time timestamp
        throw new IllegalArgumentException();
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        return getString(getColumnIndex(columnLabel));
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        return getBoolean(getColumnIndex(columnLabel));
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        return getByte(getColumnIndex(columnLabel));
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        return getShort(getColumnIndex(columnLabel));
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        return getInt(getColumnIndex(columnLabel));
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        return getLong(getColumnIndex(columnLabel));
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        return getFloat(getColumnIndex(columnLabel));
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        return getDouble(getColumnIndex(columnLabel));
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        return getBigDecimal(getColumnIndex(columnLabel), scale);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return getBytes(getColumnIndex(columnLabel));
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        return getDate(getColumnIndex(columnLabel));
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        return getTime(getColumnIndex(columnLabel));
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        return getTimestamp(getColumnIndex(columnLabel));
    }

    private int getColumnIndex(String columnLabel) throws SQLException {
        int columnIndex = columnNameList.indexOf(columnLabel) + 1;
        if (columnIndex <= 0) {
            throw new SQLDataException(String.format("Column[%s] is not exist", columnLabel));
        }
        return columnIndex;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        // TODO support result set meta data
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getMetaData is not supported");
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        // TODO support get object
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return getObject(getColumnIndex(columnLabel));
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        return getColumnIndex(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        checkClosed();
        checkValidRow();
        checkValidColumn(columnIndex);

        DataType dataType = dataTypeList.get(columnIndex - 1);
        if (!(dataType.getLogicalType() instanceof DecimalType)) {
            throw new SQLException(
                    String.format(
                            "Invalid data type, expect %s but was %s",
                            DecimalType.class.getSimpleName(),
                            dataType.getLogicalType().getClass().getSimpleName()));
        }
        DecimalType decimalType = (DecimalType) dataType.getLogicalType();
        try {
            return dataConverter.getDecimal(
                    currentRow,
                    columnIndex - 1,
                    decimalType.getPrecision(),
                    decimalType.getScale());
        } catch (Exception e) {
            throw new SQLDataException(e);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        return getBigDecimal(getColumnIndex(columnLabel));
    }

    @Override
    public Statement getStatement() throws SQLException {
        return statement;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        // TODO support array data
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getArray is not supported");
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return getArray(getColumnIndex(columnLabel));
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        // TODO get date with timezone
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        // TODO get date with timezone
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        // TODO get time with timezone
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        // TODO get time with timezone
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        // TODO get timestamp with timezone
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        // TODO get timestamp with timezone
        throw new SQLFeatureNotSupportedException("FlinkResultSet#getObject is not supported");
    }

    @Override
    public boolean isClosed() throws SQLException {
        return this.closed;
    }
}
