# Node Normalization Implementation Plan

## Scope

Add a new ID Mapper operation named `Normalize Identifiers...` that normalizes identifiers in a Cytoscape table column using the NCATS Translator Node Normalization Service.

The feature should be available through:

- Cytoscape table column-header context menu
- Cytoscape command API
- CyREST through the Cytoscape command API

The implementation should happen on a dedicated feature branch, for example:

```bash
git checkout -b feature/node-normalization
```

The related Java 17 upgrade should preferably be kept in a separate commit or pull request from the Node Normalization feature.

## Current Repo Observations

- Context-menu and command registration are centralized in `src/main/java/org/cytoscape/idmapper/internal/CyActivator.java`.
- `Map column...` currently uses one task factory for both `TableColumnTaskFactory` and command `TaskFactory`.
- GUI mapping logic lives in `src/main/java/org/cytoscape/idmapper/task/ColumnMappingTask.java`.
- Command mapping logic is duplicated in `src/main/java/org/cytoscape/idmapper/task/MapColumnCommandTask.java`.
- There is currently no test tree.
- `pom.xml` is already modified in the working tree and currently compiles with Java 11 source/target settings.

The new feature should avoid duplicating GUI and command behavior. Put normalization, batching, HTTP, parsing, validation, and table-write behavior behind shared service/task classes.

## Proposed Package Structure

Add a small normalization-focused package, for example:

```text
src/main/java/org/cytoscape/idmapper/normalization/
  CurieNormalizer.java
  NodeNormalizationClient.java
  NodeNormalizationException.java
  NodeNormalizationRequest.java
  NodeNormalizationResult.java
  NormalizeIdentifiersRequest.java
  NormalizeIdentifiersSummary.java
  NormalizeIdentifiersService.java
```

Add Cytoscape task/factory classes, for example:

```text
src/main/java/org/cytoscape/idmapper/task/
  NormalizeIdentifiersTask.java
  NormalizeIdentifiersCommandTask.java
  NormalizeIdentifiersTaskFactory.java
  NormalizeIdentifiersTaskFactoryImpl.java
```

## Shared Normalization Core

Create a shared implementation that is independent of Swing and Cytoscape command tunables.

Responsibilities:

- Trim source values.
- Ignore null and blank values.
- Detect already-prefixed CURIEs.
- Normalize user prefixes supplied as either `HGNC` or `HGNC:`.
- Apply the prefix only when a source value does not already contain a CURIE prefix.
- Avoid applying a prefix twice.
- Deduplicate CURIEs before service calls.
- Retain enough mapping information to write results back to all original rows.
- Split unique CURIEs into positive-size batches.
- Preserve successful batch results if later batches fail.
- Produce a summary with:
  - rows examined
  - unique CURIEs submitted
  - successfully normalized
  - unresolved
  - failed batches

Suggested CURIE prefix detection:

- Treat values matching a conservative pattern like `^[A-Za-z][A-Za-z0-9_.-]*:.+` as already-prefixed.
- Reject empty or malformed user prefixes.

## Node Normalization HTTP Client

Use the POST form of:

```text
/get_normalized_nodes
```

The configured base URL should default to:

```text
https://nodenormalization-sri.renci.org
```

Client requirements:

- Safely combine the base URL with `/get_normalized_nodes`.
- Tolerate base URLs with or without a trailing slash.
- Use HTTP POST with JSON.
- Set configurable connection and request timeouts.
- Report non-2xx responses as batch failures.
- Report malformed JSON as batch failures.
- Treat missing entries in a successful response as unresolved identifiers.
- Parse the canonical identifier from each normalized node's primary id.

The client and response parser should be unit-testable without Cytoscape.

## Cytoscape Properties

Add editable Cytoscape properties:

```text
idmapper.nodeNormalization.baseUrl
idmapper.nodeNormalization.batchSize
idmapper.nodeNormalization.connectTimeout
idmapper.nodeNormalization.requestTimeout
```

Suggested defaults:

- `idmapper.nodeNormalization.baseUrl=https://nodenormalization-sri.renci.org`
- `idmapper.nodeNormalization.batchSize=500`
- `idmapper.nodeNormalization.connectTimeout=10000`
- `idmapper.nodeNormalization.requestTimeout=30000`

Read these from Cytoscape's standard property/configuration mechanism. The dialog may display the resolved service endpoint and optionally allow an override.

## Context Menu UI

Register a new table column task factory in `CyActivator`.

Service properties should include:

```text
ServiceProperties.TITLE = Normalize Identifiers...
ServiceProperties.COMMAND_NAMESPACE = idmapper
ServiceProperties.COMMAND = normalize
ServiceProperties.COMMAND_SUPPORTS_JSON = true
```

The task launched from a table column-header context menu should show a dialog with:

- Source column, prepopulated from the right-clicked column
- CURIE prefix, optional
- Output column name, editable
- Batch size
- Service endpoint, displayed or editable
- Normalize action
- Cancel action

Default output column:

```text
idmapper::normalized::<source-column-name>
```

Fallback acceptable default:

```text
idmapper::normalized
```

The output column name must be editable before the task starts.

## Table Validation and Writes

Initial output behavior:

- Create a scalar `String` output column.
- Store the canonical identifier returned by the normalized node's primary id.
- Store null when an identifier cannot be normalized.
- Preserve existing source-column values.
- Do not overwrite an existing output column unless the user or command explicitly allows reuse.
- If an existing compatible output column is reused, clear or replace only values associated with the current operation.

Validation cases:

- Source column must exist.
- Source column should be scalar `String` for the first implementation.
- List columns should be rejected with a clear error.
- Numeric, boolean, and unsupported columns should be rejected.
- Output column name must be non-empty and valid.
- Existing output column should require confirmation in GUI or `overwrite=true` in command mode.
- Batch size must be a positive integer.
- Prefix must be empty or a valid CURIE prefix.
- The task should handle network/table changes while running by retaining direct references and failing clearly if writes are no longer valid.

## Task Behavior

`NormalizeIdentifiersTask` should:

- Run as a Cytoscape task.
- Report progress after each completed batch.
- Support cancellation between batches.
- Avoid writing partial row results after cancellation unless explicitly reported.
- Use `TaskMonitor` for errors, warnings, progress, and completion summaries.
- Return both human-readable `String` and machine-readable `JSONResult`.

Unresolved identifiers are not fatal. They should produce null output values and be counted in the summary.

## Command and CyREST Support

Add a Cytoscape command:

```text
idmapper normalize
```

Suggested tunables:

```text
network
table
columnName
prefix
outputColumnName
batchSize
serviceUrl
overwrite
```

Behavior:

- `network` is optional and defaults to the current network.
- `table` is optional and defaults to the current node table.
- `columnName` is required.
- `prefix` is optional.
- `outputColumnName` defaults to `idmapper::normalized::<columnName>`.
- `batchSize` overrides the configured default when supplied.
- `serviceUrl` overrides the configured endpoint for that command invocation when supplied.
- `overwrite` defaults to false.
- Use the same underlying task/service implementation as the GUI.

Example:

```text
idmapper normalize network=current table="default node" columnName=name prefix=HGNC outputColumnName="idmapper::normalized::name" batchSize=500
```

The command should return JSON like:

```json
{
  "rowsExamined": 1000,
  "uniqueCuriesSubmitted": 875,
  "successfullyNormalized": 840,
  "unresolved": 35,
  "failedBatches": 0,
  "outputColumnName": "idmapper::normalized::name"
}
```

CyREST will use the same Cytoscape command API surface.

## Testing Plan

Add a test tree under:

```text
src/test/java/org/cytoscape/idmapper/
```

Use a mocked HTTP server or mock client. Tests must not depend on the live Node Normalization service.

Unit tests should cover:

- Recognition of already-prefixed CURIEs
- Applying a user-supplied prefix
- Prefixes supplied with and without trailing colon
- Null and blank source values
- Duplicate identifiers
- Batch splitting
- Association of responses with correct rows
- Unresolved identifiers
- Partial service responses
- HTTP errors
- JSON parsing errors
- Existing output-column behavior
- Command argument validation
- Configurable endpoint handling
- URLs with and without trailing slashes

If direct Cytoscape model testing is too heavy, keep most tests around the normalization service and use small mocked or fake table abstractions for row association/write behavior.

## Java 17 Upgrade Plan

Perform separately from the feature implementation where practical.

Steps:

- Change Maven compiler source/target or `release` setting to Java 17.
- Update Maven plugins as required for Java 17 compatibility.
- Confirm OSGi bundle packaging still works.
- Review dependencies for Java 17 compatibility.
- Run all existing and new tests with Java 17.
- Confirm the resulting bundle loads in the supported Cytoscape version.
- Document Java 17 as the build requirement in `README.md`.

## Documentation Plan

Update `README.md` to document:

- `Normalize Identifiers...` context-menu operation
- Accepted input formats, including full CURIEs like `NCBIGene:7157`
- Optional prefix behavior for unprefixed source values
- Output-column default and overwrite behavior
- `idmapper normalize` command and arguments
- CyREST invocation example
- Configurable properties
- Default Node Normalization endpoint
- Java 17 build requirement

## Acceptance Checklist

- Right-clicking a supported table column displays `Normalize Identifiers...`.
- Dialog defaults to the selected source column.
- Users can optionally provide a CURIE prefix for unprefixed values.
- Users can edit the output column name.
- Output column names default to the `idmapper::` namespace.
- App calls POST `/get_normalized_nodes`.
- Requests are divided into configurable batches.
- Duplicate identifiers are submitted only once.
- Canonical identifiers are written to the correct table rows.
- Null, empty, and unresolved values are handled without terminating the operation.
- Progress, cancellation, and completion summary are provided.
- Service base URL is stored as an editable Cytoscape property.
- Feature is available as `idmapper normalize` and through CyREST.
- GUI and command execution use the same underlying implementation.
- Automated tests do not require live service access.
- README documents GUI, command, CyREST, properties, and input/output behavior.
- Development occurs in a feature branch.
- ID Mapper builds, tests, and loads successfully using Java 17.
