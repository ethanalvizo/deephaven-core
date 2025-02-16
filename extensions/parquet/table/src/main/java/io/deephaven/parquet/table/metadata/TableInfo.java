/**
 * Copyright (c) 2016-2022 Deephaven Data Labs and Patent Pending
 */
package io.deephaven.parquet.table.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.deephaven.annotations.BuildableStyle;
import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Representation class for per-table information stored in key-value metadata for Deephaven-written Parquet files.
 */
@Value.Immutable
@BuildableStyle
@JsonSerialize(as = ImmutableTableInfo.class)
@JsonDeserialize(as = ImmutableTableInfo.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class TableInfo {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        OBJECT_MAPPER = objectMapper;
    }

    public final String serializeToJSON() throws IOException {
        return OBJECT_MAPPER.writeValueAsString(this);
    }

    public static TableInfo deserializeFromJSON(@NotNull final String tableInfoRaw) throws IOException {
        return OBJECT_MAPPER.readValue(tableInfoRaw, ImmutableTableInfo.class);
    }

    public final Set<String> groupingColumnNames() {
        return groupingColumns().stream().map(GroupingColumnInfo::columnName).collect(Collectors.toSet());
    }

    public final Map<String, GroupingColumnInfo> groupingColumnMap() {
        return groupingColumns().stream()
                .collect(Collectors.toMap(GroupingColumnInfo::columnName, Function.identity()));
    }

    public final Map<String, ColumnTypeInfo> columnTypeMap() {
        return columnTypes().stream().collect(Collectors.toMap(ColumnTypeInfo::columnName, Function.identity()));
    }

    /**
     * @return The Deephaven release version used to write the parquet file
     */
    @Value.Default
    public String version() {
        final String version = TableInfo.class.getPackage().getImplementationVersion();
        if (version == null) {
            // When the code is run from class files as opposed to jars, like in unit tests
            return "unknown";
        }
        return version;
    }

    /**
     * @return List of {@link GroupingColumnInfo grouping columns} for columns with grouped data
     */
    public abstract List<GroupingColumnInfo> groupingColumns();

    /**
     * @return List of {@link ColumnTypeInfo column types} for columns requiring non-default deserialization or type
     *         selection
     */
    public abstract List<ColumnTypeInfo> columnTypes();

    @Value.Check
    final void checkVersion() {
        if (version().isEmpty()) {
            throw new IllegalArgumentException("Empty version");
        }
    }

    public static TableInfo.Builder builder() {
        return ImmutableTableInfo.builder();
    }

    public interface Builder {

        Builder version(String version);

        Builder addGroupingColumns(GroupingColumnInfo groupingColumn);

        Builder addGroupingColumns(GroupingColumnInfo... groupingColumns);

        Builder addAllGroupingColumns(Iterable<? extends GroupingColumnInfo> groupingColumns);

        Builder addColumnTypes(ColumnTypeInfo columnType);

        Builder addColumnTypes(ColumnTypeInfo... columnTypes);

        Builder addAllColumnTypes(Iterable<? extends ColumnTypeInfo> columnTypes);

        TableInfo build();
    }
}
