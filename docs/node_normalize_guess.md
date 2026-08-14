# Node Normalization Prefix Guessing Design

## Goal

Add an optional checkbox to the Node Normalization workflow that lets ID Mapper attempt to guess CURIE prefixes for unprefixed source values.

When enabled, the normalize task expands an unprefixed identifier into multiple candidate CURIEs, submits all candidates to the Node Normalization service, and writes the first candidate that successfully normalizes.

## User Interface

Add a checkbox to the `Normalize Identifiers...` dialog:

```text
Guess CURIE prefix for unprefixed values
```

Default:

```text
unchecked
```

Behavior:

- Existing full CURIE values are never expanded.
- When unchecked, current behavior remains unchanged.
- When checked, unprefixed values are expanded using suggested prefixes from the prefix catalog.
- The `CURIE prefix` field remains available and editable.
- If the user enters a prefix while guessing is enabled, that prefix is tried first.
- If the prefix catalog is unavailable and the user supplied a prefix, use only the user prefix.
- If the prefix catalog is unavailable and no user prefix was supplied, fail early with a clear error.

## Command and CyREST

Add a command/CyREST argument:

```text
guessPrefix=false
```

Optional future argument:

```text
maxPrefixGuesses=25
```

The command keeps `prefix` as a plain string. GUI prefix suggestions remain a desktop-only convenience.

Example:

```text
idmapper normalize columnName=name guessPrefix=true outputColumnName="idmapper::normalized::name"
```

## Configuration

Add a property limiting candidate expansion:

```text
idmapper.nodeNormalization.maxPrefixGuesses=25
```

This should be a positive integer. It prevents large source tables from multiplying into an unbounded number of service requests.

The existing prefix catalog configuration should be reused:

```text
idmapper.nodeNormalization.curiePrefixesUrl
idmapper.nodeNormalization.curiePrefixesCacheTtl
```

Add a property controlling whether format-based filtering is used before candidate expansion:

```text
idmapper.nodeNormalization.useIdentifierFormatFilters=true
```

This should default to true. If disabled, candidate generation should fall back to catalog-count ordering only.

## Candidate Generation

For each source value:

1. Trim surrounding whitespace.
2. Ignore null or blank values.
3. If the value already contains a CURIE prefix, use exactly that value as the only candidate.
4. If the value is unprefixed and `guessPrefix=false`:
   - If the user supplied a prefix, use `<prefix>:<value>`.
   - Otherwise use the trimmed value unchanged.
5. If the value is unprefixed and `guessPrefix=true`:
   - If the user supplied a prefix, add `<prefix>:<value>` first.
   - Use identifier-format rules to reduce the prefix catalog to plausible candidates.
   - Add plausible prefix-catalog candidates in descending confidence, then entity-count order.
   - If no format rule matches, fall back to prefix-catalog candidates in descending entity-count order.
   - Skip duplicate candidates.
   - Stop after `maxPrefixGuesses` candidates.

Prefix values should still accept both forms:

```text
HGNC
HGNC:
```

and normalize internally to:

```text
HGNC
```

## Identifier Format Filtering

Before expanding an unprefixed value across all known prefixes, inspect the identifier's shape and use that to reduce the candidate prefix list.

Examples:

```text
P04637        -> likely UniProtKB
ENSG00000141510 -> likely ENSEMBL
7157          -> likely NCBIGene, HGNC, RGD, MGI, ZFIN, SGD, or other numeric gene prefixes
11998         -> likely HGNC, NCBIGene, or other numeric gene prefixes
CHEBI:15377   -> already prefixed; no guessing
```

Add a format classifier:

```java
IdentifierFormatClassifier
```

Inputs:

- trimmed source value

Outputs:

```java
IdentifierFormatMatch {
    String ruleName;
    int confidence;
    Set<String> suggestedPrefixes;
}
```

Initial rules should be conservative. A false positive can produce wrong canonical output if it resolves before the correct prefix, so rules should only narrow the candidate list when the pattern is meaningful.

Suggested initial rules:

```text
^ENSG[0-9]+(\.[0-9]+)?$        -> ENSEMBL
^ENSP[0-9]+(\.[0-9]+)?$        -> ENSEMBL
^ENST[0-9]+(\.[0-9]+)?$        -> ENSEMBL
^[OPQ][0-9][A-Z0-9]{3}[0-9]$  -> UniProtKB
^[A-NR-Z][0-9][A-Z][A-Z0-9]{2}[0-9]$ -> UniProtKB
^GO:[0-9]{7}$                 -> already prefixed; no guessing because prefix is present
^[0-9]+$                      -> numeric identifier bucket
```

Numeric identifiers are ambiguous and should not collapse to one prefix. For numeric values, use a smaller ordered set of numeric-compatible prefixes rather than the whole catalog.

Suggested numeric-compatible prefixes:

```text
NCBIGene
HGNC
RGD
MGI
ZFIN
SGD
OMIM
PMID
RXCUI
UMLS
```

The numeric bucket should be ordered by a combination of:

1. User-supplied prefix, if any.
2. Prefixes that appear in the selected source column's existing prefixed values.
3. Prefixes with category/context evidence, if available.
4. Prefix catalog counts.

Column-level context can reduce candidates further:

- Scan existing prefixed values in the same source column.
- Count observed prefixes.
- Promote observed prefixes when generating guesses for unprefixed values in that same column.
- Example: if the column contains `NCBIGene:7157` and `NCBIGene:672`, then unprefixed numeric values should try `NCBIGene` before other numeric-compatible prefixes.

Do not filter out the user-supplied prefix even if it does not match the format classifier. The user prefix is an explicit instruction and should remain first.

## Candidate Priority

When multiple candidates resolve for a row, select the first resolved candidate in deterministic priority order:

1. Existing full CURIE, if the source value already had a prefix.
2. User-supplied prefix candidate, if present.
3. Prefixes observed in already-prefixed values from the selected source column.
4. Prefixes suggested by identifier-format rules, sorted by confidence.
5. Prefix catalog candidates sorted by descending entity count, then prefix name.

The output column stores the canonical identifier from the selected normalized node's primary id.

## Data Model

The current one-source-value-to-one-CURIE model should be generalized.

Introduce a candidate plan model:

```java
PreparedIdentifier {
    int rowIndex;
    String originalValue;
    boolean alreadyPrefixed;
    String formatRuleName;
    List<String> candidateCuries;
}
```

And a row resolution model:

```java
RowNormalizationCandidateResult {
    int rowIndex;
    String originalValue;
    String selectedInputCurie;
    String canonicalCurie;
    List<String> unresolvedCandidates;
}
```

Add a planner class independent of Cytoscape UI/model APIs:

```java
CurieCandidatePlanner
```

Inputs:

- source values
- user prefix
- `guessPrefix`
- prefix suggestions
- `maxPrefixGuesses`
- identifier format classifier
- column-level observed prefix counts

Outputs:

- per-row candidate lists
- globally deduplicated candidate CURIEs
- row-to-candidate associations
- per-row format rule metadata

## Task Flow

Update the normalize task flow:

1. Read scalar String source values from the selected table column.
2. Scan the column for already-prefixed values and collect observed prefix counts.
3. Build a `CurieCandidatePlan`.
4. Apply identifier-format filters for unprefixed values when enabled.
5. Deduplicate candidate CURIEs globally.
6. Split unique candidates into configured batches.
7. POST batches to `/get_normalized_nodes`.
8. Preserve successful batch results if later batches fail.
9. Resolve each row by selecting the first candidate that successfully normalized.
10. Write the selected canonical CURIE to the output column.
11. Write null for rows with no resolved candidate.
12. Report progress, failed batches, and a completion summary.

Cancellation behavior remains between batches. If cancelled, avoid writing partial row results unless the task explicitly reports that partial writes occurred.

## Summary Fields

Extend the completion summary with:

```text
rowsExamined
rowsWithPrefixGuesses
rowsWithFormatFilteredGuesses
candidateCuriesSubmitted
uniqueCuriesSubmitted
successfullyNormalized
unresolved
failedBatches
cancelled
outputColumnName
```

Definitions:

- `rowsWithPrefixGuesses`: number of rows where an unprefixed source value produced more than one candidate.
- `rowsWithFormatFilteredGuesses`: number of guessed rows where identifier-format rules reduced the candidate prefix list.
- `candidateCuriesSubmitted`: total candidate count before global deduplication.
- `uniqueCuriesSubmitted`: number of unique candidate CURIEs sent to the service.
- `successfullyNormalized`: number of rows that received a canonical output value.
- `unresolved`: number of non-empty rows where no candidate resolved.

## Error Handling

Handle these cases:

- Invalid `maxPrefixGuesses`.
- Prefix catalog unavailable.
- Prefix catalog returns invalid JSON.
- No prefix candidates available while guess mode is enabled.
- Identifier-format rule matches but none of the suggested prefixes exist in the prefix catalog.
- HTTP failure in one or more normalization batches.
- Partial normalization responses.
- Multiple candidates resolve for the same row.

Unresolved candidate identifiers are not fatal. They should produce null output values unless another candidate for the same row resolves.

## Testing

Add unit tests for:

- Existing prefixed values are not expanded.
- Unprefixed value with guessing off preserves current behavior.
- Unprefixed value with guessing on generates prefix candidates.
- User-supplied prefix is tried before catalog prefixes.
- Prefixes with and without trailing colon behave the same.
- Duplicate candidates are submitted once.
- `maxPrefixGuesses` limit is enforced.
- ENSEMBL-like values prefer `ENSEMBL`.
- UniProt-like values prefer `UniProtKB`.
- Numeric values use only numeric-compatible prefixes before broader fallback.
- Existing prefixed values in the same column promote that prefix for unprefixed values.
- User-supplied prefix remains first even when format rules suggest other prefixes.
- Format-filter misses fall back to catalog-count ordering.
- Row resolution selects the first resolved candidate.
- No resolved candidates write null.
- Prefix catalog unavailable with user prefix.
- Prefix catalog unavailable without user prefix.
- Partial service responses.
- Failed batches with successful earlier batches.
- Command validation for `guessPrefix` and `maxPrefixGuesses`.

## Implementation Notes

Keep prefix guessing in the shared normalization core rather than the Swing handler. The GUI checkbox and command argument should only set `guessPrefix`; the task/service layer should own candidate expansion, deduplication, batching, and row resolution.

The prefix suggestion catalog remains a convenience data source. Normalization must still support manually supplied prefixes and fully specified CURIEs without relying on the catalog.
