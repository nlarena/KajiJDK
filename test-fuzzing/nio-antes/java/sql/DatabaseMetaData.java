package java.sql;

/**
 * KajiLibrary's java.sql.DatabaseMetaData -- everything that can be asked **about** a database.
 *
 * <p>It is JDBC's largest interface, and the size has an explanation: it is the price of the rest
 * of the API being small. SQL is standardized on paper and not in practice, so a tool that wants to
 * work against any database needs to be able to ask about each difference --whether it supports
 * transactions, how long a table name can be, which character quotes identifiers-- instead of
 * assuming it. Every question not here would be an assumption wired into the caller.
 *
 * <p>It reads in three parts. The `supports*` and the limit `get*`s describe **capabilities**; the
 * ones that return a {@link ResultSet} --`getTables`, `getColumns`, `getIndexInfo`-- describe the
 * **catalogue**, and each has a column layout fixed by the standard; and a few, like
 * {@link #getConnection}, point back to the object they came from.
 *
 * <p>Almost all questions accept patterns with `%` and `_`, and there is a trap worth knowing: a
 * `null` means "do not filter by this", which is not the same as the pattern `"%"` when the
 * column's value can be null.
 */
public interface DatabaseMetaData extends Wrapper {

    // ---- the constants --------------------------------------------------------------------------
    //
    // They are the codes that appear **inside** the result sets the `get*`s return: the `NULLABLE`
    // column of `getColumns` holds a `columnNullable`, that of `getProcedureColumns` a
    // `procedureNullable`. That there are four almost identical families --`column*`, `procedure*`,
    // `function*`, `attribute*`-- is accumulated history: each arrived with its part of the
    // standard and none could be unified later without breaking someone. (This comment said three
    // while listing four.)

    int procedureColumnUnknown = 0;

    int sqlStateSQL99 = 2;

    int functionResultUnknown = 0;

    int columnNoNulls = 0;

    int bestRowPseudo = 2;

    int typePredBasic = 2;

    int functionNullableUnknown = 2;

    int typeNoNulls = 0;

    int importedKeySetNull = 2;

    int sqlStateXOpen = 1;

    int versionColumnUnknown = 0;

    int functionColumnInOut = 2;

    int typeSearchable = 3;

    short tableIndexOther = 3;

    int procedureColumnResult = 3;

    int bestRowNotPseudo = 1;

    int typeNullable = 1;

    int procedureNullableUnknown = 2;

    int procedureNoNulls = 0;

    int columnNullableUnknown = 2;

    int bestRowUnknown = 0;

    int typePredChar = 1;

    int versionColumnPseudo = 2;

    int importedKeyNotDeferrable = 7;

    int procedureNullable = 1;

    int functionColumnIn = 1;

    int procedureReturnsResult = 2;

    int bestRowTemporary = 0;

    int functionNoTable = 1;

    int bestRowSession = 2;

    int functionReturnsTable = 2;

    int sqlStateSQL = 2;

    int functionReturn = 4;

    int versionColumnNotPseudo = 1;

    int procedureColumnReturn = 5;

    int importedKeyInitiallyDeferred = 5;

    short attributeNoNulls = 0;

    int columnNullable = 1;

    int typePredNone = 0;

    int importedKeyRestrict = 1;

    int functionColumnResult = 5;

    short attributeNullable = 1;

    int functionNoNulls = 0;

    int bestRowTransaction = 1;

    int typeNullableUnknown = 2;

    int functionColumnUnknown = 0;

    int importedKeyNoAction = 3;

    int procedureColumnOut = 4;

    int functionColumnOut = 3;

    int procedureColumnIn = 1;

    short attributeNullableUnknown = 2;

    short tableIndexClustered = 1;

    int procedureNoResult = 1;

    int importedKeyCascade = 0;

    int functionNullable = 1;

    int procedureResultUnknown = 0;

    short tableIndexHashed = 2;

    short tableIndexStatistic = 0;

    int procedureColumnInOut = 2;

    int importedKeyInitiallyImmediate = 6;

    int importedKeySetDefault = 4;

    boolean allProceduresAreCallable() throws java.sql.SQLException;

    boolean allTablesAreSelectable() throws java.sql.SQLException;

    boolean autoCommitFailureClosesAllResultSets() throws java.sql.SQLException;

    boolean dataDefinitionCausesTransactionCommit() throws java.sql.SQLException;

    boolean dataDefinitionIgnoredInTransactions() throws java.sql.SQLException;

    boolean deletesAreDetected(int type) throws java.sql.SQLException;

    boolean doesMaxRowSizeIncludeBlobs() throws java.sql.SQLException;

    boolean generatedKeyAlwaysReturned() throws java.sql.SQLException;

    boolean insertsAreDetected(int type) throws java.sql.SQLException;

    boolean isCatalogAtStart() throws java.sql.SQLException;

    boolean isReadOnly() throws java.sql.SQLException;

    boolean locatorsUpdateCopy() throws java.sql.SQLException;

    boolean nullPlusNonNullIsNull() throws java.sql.SQLException;

    boolean nullsAreSortedAtEnd() throws java.sql.SQLException;

    boolean nullsAreSortedAtStart() throws java.sql.SQLException;

    boolean nullsAreSortedHigh() throws java.sql.SQLException;

    boolean nullsAreSortedLow() throws java.sql.SQLException;

    boolean othersDeletesAreVisible(int type) throws java.sql.SQLException;

    boolean othersInsertsAreVisible(int type) throws java.sql.SQLException;

    boolean othersUpdatesAreVisible(int type) throws java.sql.SQLException;

    boolean ownDeletesAreVisible(int type) throws java.sql.SQLException;

    boolean ownInsertsAreVisible(int type) throws java.sql.SQLException;

    boolean ownUpdatesAreVisible(int type) throws java.sql.SQLException;

    boolean storesLowerCaseIdentifiers() throws java.sql.SQLException;

    boolean storesLowerCaseQuotedIdentifiers() throws java.sql.SQLException;

    boolean storesMixedCaseIdentifiers() throws java.sql.SQLException;

    boolean storesMixedCaseQuotedIdentifiers() throws java.sql.SQLException;

    boolean storesUpperCaseIdentifiers() throws java.sql.SQLException;

    boolean storesUpperCaseQuotedIdentifiers() throws java.sql.SQLException;

    boolean supportsANSI92EntryLevelSQL() throws java.sql.SQLException;

    boolean supportsANSI92FullSQL() throws java.sql.SQLException;

    boolean supportsANSI92IntermediateSQL() throws java.sql.SQLException;

    boolean supportsAlterTableWithAddColumn() throws java.sql.SQLException;

    boolean supportsAlterTableWithDropColumn() throws java.sql.SQLException;

    boolean supportsBatchUpdates() throws java.sql.SQLException;

    boolean supportsCatalogsInDataManipulation() throws java.sql.SQLException;

    boolean supportsCatalogsInIndexDefinitions() throws java.sql.SQLException;

    boolean supportsCatalogsInPrivilegeDefinitions() throws java.sql.SQLException;

    boolean supportsCatalogsInProcedureCalls() throws java.sql.SQLException;

    boolean supportsCatalogsInTableDefinitions() throws java.sql.SQLException;

    boolean supportsColumnAliasing() throws java.sql.SQLException;

    boolean supportsConvert() throws java.sql.SQLException;

    boolean supportsConvert(int fromType, int toType) throws java.sql.SQLException;

    boolean supportsCoreSQLGrammar() throws java.sql.SQLException;

    boolean supportsCorrelatedSubqueries() throws java.sql.SQLException;

    boolean supportsDataDefinitionAndDataManipulationTransactions() throws java.sql.SQLException;

    boolean supportsDataManipulationTransactionsOnly() throws java.sql.SQLException;

    boolean supportsDifferentTableCorrelationNames() throws java.sql.SQLException;

    boolean supportsExpressionsInOrderBy() throws java.sql.SQLException;

    boolean supportsExtendedSQLGrammar() throws java.sql.SQLException;

    boolean supportsFullOuterJoins() throws java.sql.SQLException;

    boolean supportsGetGeneratedKeys() throws java.sql.SQLException;

    boolean supportsGroupBy() throws java.sql.SQLException;

    boolean supportsGroupByBeyondSelect() throws java.sql.SQLException;

    boolean supportsGroupByUnrelated() throws java.sql.SQLException;

    boolean supportsIntegrityEnhancementFacility() throws java.sql.SQLException;

    boolean supportsLikeEscapeClause() throws java.sql.SQLException;

    boolean supportsLimitedOuterJoins() throws java.sql.SQLException;

    boolean supportsMinimumSQLGrammar() throws java.sql.SQLException;

    boolean supportsMixedCaseIdentifiers() throws java.sql.SQLException;

    boolean supportsMixedCaseQuotedIdentifiers() throws java.sql.SQLException;

    boolean supportsMultipleOpenResults() throws java.sql.SQLException;

    boolean supportsMultipleResultSets() throws java.sql.SQLException;

    boolean supportsMultipleTransactions() throws java.sql.SQLException;

    boolean supportsNamedParameters() throws java.sql.SQLException;

    boolean supportsNonNullableColumns() throws java.sql.SQLException;

    boolean supportsOpenCursorsAcrossCommit() throws java.sql.SQLException;

    boolean supportsOpenCursorsAcrossRollback() throws java.sql.SQLException;

    boolean supportsOpenStatementsAcrossCommit() throws java.sql.SQLException;

    boolean supportsOpenStatementsAcrossRollback() throws java.sql.SQLException;

    boolean supportsOrderByUnrelated() throws java.sql.SQLException;

    boolean supportsOuterJoins() throws java.sql.SQLException;

    boolean supportsPositionedDelete() throws java.sql.SQLException;

    boolean supportsPositionedUpdate() throws java.sql.SQLException;

    default boolean supportsRefCursors() throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("supportsRefCursors not implemented");
    }

    boolean supportsResultSetConcurrency(int type, int concurrency) throws java.sql.SQLException;

    boolean supportsResultSetHoldability(int holdability) throws java.sql.SQLException;

    boolean supportsResultSetType(int type) throws java.sql.SQLException;

    boolean supportsSavepoints() throws java.sql.SQLException;

    boolean supportsSchemasInDataManipulation() throws java.sql.SQLException;

    boolean supportsSchemasInIndexDefinitions() throws java.sql.SQLException;

    boolean supportsSchemasInPrivilegeDefinitions() throws java.sql.SQLException;

    boolean supportsSchemasInProcedureCalls() throws java.sql.SQLException;

    boolean supportsSchemasInTableDefinitions() throws java.sql.SQLException;

    boolean supportsSelectForUpdate() throws java.sql.SQLException;

    default boolean supportsSharding() throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("supportsSharding not implemented");
    }

    boolean supportsStatementPooling() throws java.sql.SQLException;

    boolean supportsStoredFunctionsUsingCallSyntax() throws java.sql.SQLException;

    boolean supportsStoredProcedures() throws java.sql.SQLException;

    boolean supportsSubqueriesInComparisons() throws java.sql.SQLException;

    boolean supportsSubqueriesInExists() throws java.sql.SQLException;

    boolean supportsSubqueriesInIns() throws java.sql.SQLException;

    boolean supportsSubqueriesInQuantifieds() throws java.sql.SQLException;

    boolean supportsTableCorrelationNames() throws java.sql.SQLException;

    boolean supportsTransactionIsolationLevel(int level) throws java.sql.SQLException;

    boolean supportsTransactions() throws java.sql.SQLException;

    boolean supportsUnion() throws java.sql.SQLException;

    boolean supportsUnionAll() throws java.sql.SQLException;

    boolean updatesAreDetected(int type) throws java.sql.SQLException;

    boolean usesLocalFilePerTable() throws java.sql.SQLException;

    boolean usesLocalFiles() throws java.sql.SQLException;

    int getDatabaseMajorVersion() throws java.sql.SQLException;

    int getDatabaseMinorVersion() throws java.sql.SQLException;

    int getDefaultTransactionIsolation() throws java.sql.SQLException;

    int getDriverMajorVersion();

    int getDriverMinorVersion();

    int getJDBCMajorVersion() throws java.sql.SQLException;

    int getJDBCMinorVersion() throws java.sql.SQLException;

    int getMaxBinaryLiteralLength() throws java.sql.SQLException;

    int getMaxCatalogNameLength() throws java.sql.SQLException;

    int getMaxCharLiteralLength() throws java.sql.SQLException;

    int getMaxColumnNameLength() throws java.sql.SQLException;

    int getMaxColumnsInGroupBy() throws java.sql.SQLException;

    int getMaxColumnsInIndex() throws java.sql.SQLException;

    int getMaxColumnsInOrderBy() throws java.sql.SQLException;

    int getMaxColumnsInSelect() throws java.sql.SQLException;

    int getMaxColumnsInTable() throws java.sql.SQLException;

    int getMaxConnections() throws java.sql.SQLException;

    int getMaxCursorNameLength() throws java.sql.SQLException;

    int getMaxIndexLength() throws java.sql.SQLException;

    int getMaxProcedureNameLength() throws java.sql.SQLException;

    int getMaxRowSize() throws java.sql.SQLException;

    int getMaxSchemaNameLength() throws java.sql.SQLException;

    int getMaxStatementLength() throws java.sql.SQLException;

    int getMaxStatements() throws java.sql.SQLException;

    int getMaxTableNameLength() throws java.sql.SQLException;

    int getMaxTablesInSelect() throws java.sql.SQLException;

    int getMaxUserNameLength() throws java.sql.SQLException;

    int getResultSetHoldability() throws java.sql.SQLException;

    int getSQLStateType() throws java.sql.SQLException;

    java.lang.String getCatalogSeparator() throws java.sql.SQLException;

    java.lang.String getCatalogTerm() throws java.sql.SQLException;

    java.lang.String getDatabaseProductName() throws java.sql.SQLException;

    java.lang.String getDatabaseProductVersion() throws java.sql.SQLException;

    java.lang.String getDriverName() throws java.sql.SQLException;

    java.lang.String getDriverVersion() throws java.sql.SQLException;

    java.lang.String getExtraNameCharacters() throws java.sql.SQLException;

    java.lang.String getIdentifierQuoteString() throws java.sql.SQLException;

    java.lang.String getNumericFunctions() throws java.sql.SQLException;

    java.lang.String getProcedureTerm() throws java.sql.SQLException;

    java.lang.String getSQLKeywords() throws java.sql.SQLException;

    java.lang.String getSchemaTerm() throws java.sql.SQLException;

    java.lang.String getSearchStringEscape() throws java.sql.SQLException;

    java.lang.String getStringFunctions() throws java.sql.SQLException;

    java.lang.String getSystemFunctions() throws java.sql.SQLException;

    java.lang.String getTimeDateFunctions() throws java.sql.SQLException;

    java.lang.String getURL() throws java.sql.SQLException;

    java.lang.String getUserName() throws java.sql.SQLException;

    java.sql.Connection getConnection() throws java.sql.SQLException;

    java.sql.ResultSet getAttributes(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String typeNamePattern, java.lang.String attributeNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getBestRowIdentifier(java.lang.String catalog, java.lang.String schema, java.lang.String table, int scope, boolean nullable) throws java.sql.SQLException;

    java.sql.ResultSet getCatalogs() throws java.sql.SQLException;

    java.sql.ResultSet getClientInfoProperties() throws java.sql.SQLException;

    java.sql.ResultSet getColumnPrivileges(java.lang.String catalog, java.lang.String schema, java.lang.String table, java.lang.String columnNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getColumns(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String tableNamePattern, java.lang.String columnNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getCrossReference(java.lang.String parentCatalog, java.lang.String parentSchema, java.lang.String parentTable, java.lang.String foreignCatalog, java.lang.String foreignSchema, java.lang.String foreignTable) throws java.sql.SQLException;

    java.sql.ResultSet getExportedKeys(java.lang.String catalog, java.lang.String schema, java.lang.String table) throws java.sql.SQLException;

    java.sql.ResultSet getFunctionColumns(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String functionNamePattern, java.lang.String columnNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getFunctions(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String functionNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getImportedKeys(java.lang.String catalog, java.lang.String schema, java.lang.String table) throws java.sql.SQLException;

    java.sql.ResultSet getIndexInfo(java.lang.String catalog, java.lang.String schema, java.lang.String table, boolean unique, boolean approximate) throws java.sql.SQLException;

    java.sql.ResultSet getPrimaryKeys(java.lang.String catalog, java.lang.String schema, java.lang.String table) throws java.sql.SQLException;

    java.sql.ResultSet getProcedureColumns(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String procedureNamePattern, java.lang.String columnNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getProcedures(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String procedureNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getPseudoColumns(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String tableNamePattern, java.lang.String columnNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getSchemas() throws java.sql.SQLException;

    java.sql.ResultSet getSchemas(java.lang.String catalog, java.lang.String schemaPattern) throws java.sql.SQLException;

    java.sql.ResultSet getSuperTables(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String tableNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getSuperTypes(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String typeNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getTablePrivileges(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String tableNamePattern) throws java.sql.SQLException;

    java.sql.ResultSet getTableTypes() throws java.sql.SQLException;

    java.sql.ResultSet getTables(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String tableNamePattern, java.lang.String[] types) throws java.sql.SQLException;

    java.sql.ResultSet getTypeInfo() throws java.sql.SQLException;

    java.sql.ResultSet getUDTs(java.lang.String catalog, java.lang.String schemaPattern, java.lang.String typeNamePattern, int[] types) throws java.sql.SQLException;

    java.sql.ResultSet getVersionColumns(java.lang.String catalog, java.lang.String schema, java.lang.String table) throws java.sql.SQLException;

    java.sql.RowIdLifetime getRowIdLifetime() throws java.sql.SQLException;

    default long getMaxLogicalLobSize() throws java.sql.SQLException {
        throw new java.sql.SQLFeatureNotSupportedException("getMaxLogicalLobSize not implemented");
    }
}
