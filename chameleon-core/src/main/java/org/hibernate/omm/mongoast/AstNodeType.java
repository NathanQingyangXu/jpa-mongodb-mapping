package org.hibernate.omm.mongoast;

public enum AstNodeType
{
    AccumulatorField,
    AddFieldsStage,
    Aggregation,
    AllFilterOperation,
    AndFilter,
    BinaryExpression,
    BinaryWindowExpression,
    BitsAllClearFilterOperation,
    BitsAllSetFilterOperation,
    BitsAnyClearFilterOperation,
    BitsAnySetFilterOperation,
    BucketAutoStage,
    BucketStage,
    CollStatsStage,
    ComparisonFilterOperation,
    ComputedArrayExpression,
    ComputedDocumentExpression,
    ComputedField,
    CondExpression,
    ConstantExpression,
    ConvertExpression,
    CountStage,
    CurrentOpStage,
    CustomAccumulatorExpression,
    DateAddExpression,
    DateDiffExpression,
    DateFromIsoWeekPartsExpression,
    DateFromPartsExpression,
    DateFromStringExpression,
    DatePartExpression,
    DateSubtractExpression,
    DateToPartsExpression,
    DateToStringExpression,
    DateTruncExpression,
    DensifyStage,
    DerivativeOrIntegralWindowExpression,
    DocumentsStage,
    ElemMatchFilterOperation,
    ExistsFilterOperation,
    ExponentialMovingAverageWindowExpression,
    ExprFilter,
    FacetStage,
    FacetStageFacet,
    FieldOperationFilter,
    FieldPathExpression,
    FilterExpression,
    FilterField,
    FindProjection,
    FunctionExpression,
    GeoIntersectsFilterOperation,
    GeoNearStage,
    GeoWithinBoxFilterOperation,
    GeoWithinCenterFilterOperation,
    GeoWithinCenterSphereFilterOperation,
    GeoWithinFilterOperation,
    GetFieldExpression,
    GraphLookupStage,
    GroupStage,
    ImpliedOperationFilterOperation,
    IndexOfArrayExpression,
    IndexOfBytesExpression,
    IndexOfCPExpression,
    IndexStatsStage,
    InFilterOperation,
    JsonSchemaFilter,
    LetExpression,
    LimitStage,
    ListLocalSessionsStage,
    ListSessionsStage,
    LookupStage,
    LookupStageEqualityMatch,
    LookupStageUncorrelatedMatch,
    LTrimExpression,
    MapExpression,
    MatchesEverythingFilter,
    MatchesNothingFilter,
    MatchStage,
    MergeStage,
    ModFilterOperation,
    NaryExpression,
    NearFilterOperation,
    NearSphereFilterOperation,
    NinFilterOperation,
    NorFilter,
    NotFilterOperation,
    NullaryWindowExpression,
    OrFilter,
    OutStage,
    PickAccumulatorExpression,
    PickExpression,
    Pipeline,
    PlanCacheStatsStage,
    ProjectStage,
    ProjectStageExcludeFieldSpecification,
    ProjectStageIncludeFieldSpecification,
    ProjectStageSetFieldSpecification,
    RangeExpression,
    RawFilter,
    RedactStage,
    ReduceExpression,
    RegexFilterOperation,
    RegexExpression,
    ReplaceAllExpression,
    ReplaceOneExpression,
    ReplaceRootStage,
    ReplaceWithStage,
    RTrimExpression,
    SampleStage,
    SetStage,
    SetWindowFieldsStage,
    ShiftWindowExpression,
    SizeFilterOperation,
    SkipStage,
    SliceExpression,
    SortArrayExpression,
    SortStage,
    SortByCountStage,
    SwitchExpression,
    SwitchExpressionBranch,
    TernaryExpression,
    TextFilter,
    TrimExpression,
    TypeFilterOperation,
    UnaryAccumulatorExpression,
    UnaryExpression,
    UnaryWindowExpression,
    UnionWithStage,
    UniversalStage,
    UnsetStage,
    UnwindStage,
    VarBinding,
    VarExpression,
    WhereFilter,
    WindowField,
    ZipExpression
}
