package org.hibernate.omm.translate.translator;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.omm.exception.NotSupportedRuntimeException;
import org.hibernate.omm.translate.translator.mongoast.AstElement;
import org.hibernate.omm.translate.translator.mongoast.AstFieldUpdate;
import org.hibernate.omm.translate.translator.mongoast.AstInsertCommand;
import org.hibernate.omm.translate.translator.mongoast.AstValue;
import org.hibernate.omm.translate.translator.mongoast.DeleteCommand;
import org.hibernate.omm.translate.translator.mongoast.UpdateCommand;
import org.hibernate.omm.translate.translator.mongoast.filters.AstAndFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstComparisonFilterOperation;
import org.hibernate.omm.translate.translator.mongoast.filters.AstComparisonFilterOperator;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFieldOperationFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilter;
import org.hibernate.omm.translate.translator.mongoast.filters.AstFilterField;
import org.hibernate.omm.translate.translator.mongoast.filters.AstMatchesEverythingFilter;
import org.hibernate.omm.util.CollectionUtil;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.insert.Values;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.internal.TableDeleteStandard;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateStandard;

/**
 * Contains mutation Bson command rendering logic, including:
 * <ol>
 *     <li>insertion</li>
 *     <li>updating</li>
 *     <li>deletion</li>
 * </ol>
 *
 * @see MQLTranslator
 * @author Nathan Xu
 * @param <T> {@link JdbcOperation} generic type
 */
public class BsonCommandTranslator<T extends JdbcOperation> extends AbstractBsonTranslator<T> {

    public BsonCommandTranslator(final SessionFactoryImplementor sessionFactory, final Statement statement) {
        super(sessionFactory, statement);
    }

    @Override
    public void visitStandardTableInsert(final TableInsertStandard tableInsert) {
        getCurrentClauseStack().push(Clause.INSERT);
        try {
            renderInsertInto(tableInsert);
            if (tableInsert.getNumberOfReturningColumns() > 0) {
                visitReturningColumns(tableInsert::getReturningColumns);
            }
        } finally {
            getCurrentClauseStack().pop();
        }
    }

    private void renderInsertInto(final TableInsertStandard tableInsert) {
        if (tableInsert.getNumberOfValueBindings() == 0) {
            renderInsertIntoNoColumns(tableInsert);
            return;
        }
        String collectionName =
                mqlAstState.expect(AttachmentKeys.collectionName(), () -> renderIntoIntoAndTable(tableInsert));

        List<AstElement> elements = new ArrayList<>();
        tableInsert.forEachValueBinding((columnPosition, columnValueBinding) -> {
            AstValue value = mqlAstState.expect(
                    AttachmentKeys.fieldValue(),
                    () -> columnValueBinding.getValueExpression().accept(this));
            elements.add(new AstElement(columnValueBinding.getColumnReference().getColumnExpression(), value));
        });

        root = new AstInsertCommand(collectionName, elements);
    }

    @Override
    protected void renderIntoIntoAndTable(final TableInsertStandard tableInsert) {
        mqlAstState.attach(
                AttachmentKeys.collectionName(), tableInsert.getMutatingTable().getTableName());
        registerAffectedTable(tableInsert.getMutatingTable().getTableName());
    }

    @Override
    protected void visitInsertStatementOnly(final InsertSelectStatement statement) {
        getClauseStack().push(Clause.INSERT);

        registerAffectedTable(statement.getTargetTable());

        getClauseStack().push(Clause.VALUES);

        if (statement.getSourceSelectStatement() != null) {
            // TODO implement using aggregate command
        } else {
            List<ColumnReference> targetColumnReferences = statement.getTargetColumns();

            if (targetColumnReferences != null) {
                List<Values> targetColumnValuesList = statement.getValuesList();
                for (int valuesIndex = 0;
                        valuesIndex < statement.getValuesList().size();
                        valuesIndex++) {
                    for (int columnIndex = 0; columnIndex < targetColumnReferences.size(); columnIndex++) {
                        targetColumnValuesList
                                .get(valuesIndex)
                                .getExpressions()
                                .get(columnIndex)
                                .accept(this);
                    }
                }
            }
        }
        getClauseStack().pop();
        getClauseStack().pop();
    }

    @Override
    public void visitColumnReference(final ColumnReference columnReference) {
        if (determineColumnReferenceQualifier(columnReference) != null) {
            throw new NotSupportedRuntimeException();
        }
        mqlAstState.attach(AttachmentKeys.fieldName(), columnReference.getColumnExpression());
    }

    @Override
    public void visitStandardTableDelete(final TableDeleteStandard tableDelete) {
        getClauseStack().push(Clause.DELETE);
        try {
            registerAffectedTable(tableDelete.getMutatingTable().getTableName());

            getClauseStack().push(Clause.WHERE);
            try {
                List<AstFilter> filters = new ArrayList<>();
                tableDelete.forEachKeyBinding((columnPosition, columnValueBinding) -> {
                    AstValue value = mqlAstState.expect(
                            AttachmentKeys.fieldValue(),
                            () -> columnValueBinding.getValueExpression().accept(this));
                    filters.add(new AstFieldOperationFilter(
                            new AstFilterField(
                                    columnValueBinding.getColumnReference().getColumnExpression()),
                            new AstComparisonFilterOperation(AstComparisonFilterOperator.EQ, value)));
                });

                //                if (tableDelete.getNumberOfOptimisticLockBindings() > 0) {
                //                    // TODO: untested path
                //                }
                //
                //                if (tableDelete.getWhereFragment() != null) {
                //                    // TODO: untested path
                //                }
                root = new DeleteCommand(tableDelete.getMutatingTable().getTableName(), new AstAndFilter(filters));
            } finally {
                getClauseStack().pop();
            }
        } finally {
            getClauseStack().pop();
        }
    }

    @Override
    protected void visitDeleteStatementOnly(final DeleteStatement statement) {
        if (CollectionUtil.isNotEmpty(statement.getReturningColumns())) {
            throw new NotSupportedRuntimeException("delete statement with returning columns not supported");
        }
        if (statement.getFromClause().getRoots().size() > 1) {
            throw new NotSupportedRuntimeException("delete statement with multiple roots not supported");
        }
        if (statement.getFromClause().getRoots().get(0).hasRealJoins()) {
            throw new NotSupportedRuntimeException("delete statement with root having real joins not supported");
        }
        renderDeleteClause(statement);
        AstFilter filter = statement.getRestriction() != null
                ? mqlAstState.expect(AttachmentKeys.filter(), () -> visitWhereClause(statement.getRestriction()))
                : new AstMatchesEverythingFilter();
        root = new DeleteCommand(statement.getTargetTable().getTableExpression(), filter);
    }

    @Override
    protected void renderDeleteClause(final DeleteStatement statement) {
        try {
            getClauseStack().push(Clause.DELETE);
            renderDmlTargetTableExpression(statement.getTargetTable());
        } finally {
            getClauseStack().pop();
        }
    }

    @Override
    public void visitStandardTableUpdate(final TableUpdateStandard tableUpdate) {
        getClauseStack().push(Clause.UPDATE);
        try {
            visitTableUpdate(tableUpdate);
        } finally {
            getClauseStack().pop();
        }
    }

    private void visitTableUpdate(final RestrictedTableMutation<? extends MutationOperation> tableUpdate) {
        registerAffectedTable(tableUpdate.getMutatingTable().getTableName());

        getClauseStack().push(Clause.WHERE);
        List<AstFilter> filters = new ArrayList<>();
        List<AstFieldUpdate> updates = new ArrayList<>();
        try {
            int predicates = tableUpdate.getNumberOfKeyBindings() + tableUpdate.getNumberOfOptimisticLockBindings();
            boolean hasWhereFragment = tableUpdate instanceof TableUpdateStandard tableUpdateStandard
                    && tableUpdateStandard.getWhereFragment() != null;
            if (hasWhereFragment) {
                predicates++;
            }
            if (predicates == 0) {
                // TODO: untested
            } else {
                tableUpdate.forEachKeyBinding((position, columnValueBinding) -> {
                    AstValue value = mqlAstState.expect(
                            AttachmentKeys.fieldValue(),
                            () -> columnValueBinding.getValueExpression().accept(this));
                    filters.add(new AstFieldOperationFilter(
                            new AstFilterField(
                                    columnValueBinding.getColumnReference().getColumnExpression()),
                            new AstComparisonFilterOperation(AstComparisonFilterOperator.EQ, value)));
                });

                //                if (tableUpdate.getNumberOfOptimisticLockBindings() > 0) {
                //                    // TODO: untested
                //                }
                //
                //                if (hasWhereFragment) {
                //                    // TODO: untested
                //                }
                //                if (predicates > 1) {
                //                    // TODO: untested
                //                }
            }
        } finally {
            getClauseStack().pop();
        }
        getClauseStack().push(Clause.SET);
        try {
            tableUpdate.forEachValueBinding((columnPosition, columnValueBinding) -> {
                AstValue value = mqlAstState.expect(
                        AttachmentKeys.fieldValue(),
                        () -> columnValueBinding.getValueExpression().accept(this));
                updates.add(new AstFieldUpdate(
                        columnValueBinding.getColumnReference().getColumnExpression(), value));
            });
        } finally {
            getClauseStack().pop();
        }
        root = new UpdateCommand(tableUpdate.getMutatingTable().getTableName(), new AstAndFilter(filters), updates);
    }

    @Override
    protected void visitUpdateStatementOnly(final UpdateStatement statement) {
        if (statement.getFromClause().getRoots().size() > 1) {
            throw new NotSupportedRuntimeException("update statement with multiple roots not supported");
        }
        if (statement.getFromClause().getRoots().get(0).hasRealJoins()) {
            throw new NotSupportedRuntimeException("update statement with root having real joins not supported");
        }
        AstFilter filter =
                mqlAstState.expect(AttachmentKeys.filter(), () -> visitWhereClause(statement.getRestriction()));
        List<AstFieldUpdate> updates =
                mqlAstState.expect(AttachmentKeys.fieldUpdates(), () -> renderSetClause(statement.getAssignments()));
        root = new UpdateCommand(statement.getTargetTable().getTableExpression(), filter, updates);
    }

    @Override
    protected void renderUpdateClause(final UpdateStatement updateStatement) {
        try {
            getClauseStack().push(Clause.UPDATE);
            renderDmlTargetTableExpression(updateStatement.getTargetTable());
        } finally {
            getClauseStack().pop();
        }
    }

    @Override
    protected void renderSetClause(final List<Assignment> assignments) {
        try {
            List<AstFieldUpdate> updates = new ArrayList<>();
            getClauseStack().push(Clause.SET);
            for (Assignment assignment : assignments) {
                updates.add(mqlAstState.expect(AttachmentKeys.fieldUpdate(), () -> visitSetAssignment(assignment)));
            }
            mqlAstState.attach(AttachmentKeys.fieldUpdates(), updates);
        } finally {
            getClauseStack().pop();
        }
    }

    @Override
    protected void visitSetAssignment(final Assignment assignment) {
        final List<ColumnReference> columnReferences =
                assignment.getAssignable().getColumnReferences();
        if (columnReferences.size() == 1) {
            ColumnReference columnReference = columnReferences.get(0);
            if (columnReference.getQualifier() != null) {
                // TODO: anything to do here?
            }
            final Expression assignedValue = assignment.getAssignedValue();
            final SqlTuple sqlTuple = SqlTupleContainer.getSqlTuple(assignedValue);
            if (sqlTuple != null) {
                throw new NotSupportedRuntimeException();
            }
            AstValue value = mqlAstState.expect(AttachmentKeys.fieldValue(), () -> assignedValue.accept(this));
            mqlAstState.attach(
                    AttachmentKeys.fieldUpdate(), new AstFieldUpdate(columnReference.getColumnExpression(), value));
        } else {
            throw new NotSupportedRuntimeException();
        }
    }
}
