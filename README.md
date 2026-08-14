# idmapper

BridgeDB based Identifier mapping

Build requirement: Java 17.

Version 3.6.5 - remove KEGG from MappingSource

Version 3.6.4 - added https support

This is a Cytoscape app that allows identifier mapping on tables. Once this app is installed, the table browser will respond to a right click on the column header with a pop-up menu containing the item **Map Column...**

A dialog is shown to gather the information about the species, and which columns you want to map **From** (the source) and **To** (the target). By default, multiple return values will be simplified to the first in the list, but turning off the **Force Single** checkbox will maintain all returned IDs.

Once the dialog is confirmed, a new column will be added to the Node Table containing the mapped identifiers.

This version adds support for the command dialog and CyREST access.

'{  
 "columnName": "name",  
 "forceSingle": "true",  
 "mapFrom": "HGNC",  
 "mapTo": "Ensembl",  
 "network": "A",  
 "table": "default node",  
 "species": "Human (Homo sapiens)"  
}'

## Normalize Identifiers

ID Mapper also adds **Normalize Identifiers...** to the Cytoscape table column-header context menu. This operation normalizes identifiers in a scalar String table column using the NCATS Translator Node Normalization Service.

Default service endpoint:

```text
https://nodenormalization-sri.renci.org
```

The source column should contain CURIEs such as:

```text
NCBIGene:7157
HGNC:11998
UniProtKB:P04637
```

If the column contains unprefixed identifiers, supply a CURIE prefix in the dialog. Prefixes may be entered with or without the trailing colon, for example `HGNC` or `HGNC:`. Values that already contain a CURIE prefix are sent unchanged, so the prefix is not applied twice. Null and blank values are ignored.

In the Cytoscape desktop dialog, the CURIE prefix field is an editable suggested-value list. Suggestions are loaded from the Node Normalization prefix catalog and displayed with entity counts. If the catalog is unavailable, the field remains editable and manually entered prefixes still work.

The dialog also includes **Guess CURIE prefix for unprefixed values**. When enabled, unprefixed values are expanded into candidate CURIEs using the typed prefix, prefixes already observed in the selected column, identifier-format rules, and the prefix catalog. Format rules reduce obvious cases such as Ensembl-like and UniProt-like identifiers before falling back to catalog-count ordering. Numeric values use a smaller numeric-compatible prefix set.

The operation creates a scalar String output column containing the canonical identifier returned as the normalized node's primary id. The default output column name is:

```text
idmapper::normalized::<source-column-name>
```

The output column name is editable before the operation starts. Existing output columns are not reused unless overwrite/reuse is explicitly enabled. Unresolved identifiers are written as null.

Requests are sent to the POST form of `/get_normalized_nodes` in configurable batches. Duplicate identifiers are submitted once and reused for all matching rows.

## Normalize Command

The same feature is available as a Cytoscape command and through CyREST:

```text
idmapper normalize
```

Arguments:

```text
network           Optional network name or SUID; defaults to the current network
table             Optional target table; defaults to the current node table
columnName        Required source column containing identifiers
prefix            Optional CURIE prefix for values without one
outputColumnName  Optional output column name; defaults to idmapper::normalized::<columnName>
batchSize         Optional positive integer overriding the configured default
serviceUrl        Optional per-command service base URL override
overwrite         Optional boolean allowing reuse of an existing String output column
guessPrefix       Optional boolean that tries suggested prefixes for unprefixed values
maxPrefixGuesses  Optional positive integer limiting guesses per unprefixed value
```

Example:

```text
idmapper normalize network=current table="default node" columnName=name prefix=HGNC outputColumnName="idmapper::normalized::name" batchSize=500 guessPrefix=true maxPrefixGuesses=25
```

The command returns a JSON summary containing row count, submitted unique CURIE count, normalized count, unresolved count, failed batch count, cancellation state, output column name, and errors.

CyREST example:

```bash
curl -X POST 'http://localhost:1234/v1/commands/idmapper/normalize' \
  -H 'Content-Type: application/json' \
  -d '{
    "network": "current",
    "table": "default node",
    "columnName": "name",
    "prefix": "HGNC",
    "outputColumnName": "idmapper::normalized::name",
    "batchSize": 500,
    "guessPrefix": true,
    "maxPrefixGuesses": 25
  }'
```

## Node Normalization Properties

The Node Normalization settings are registered as Cytoscape properties and saved in the normal Cytoscape configuration directory:

```text
idmapper.nodeNormalization.baseUrl=https://nodenormalization-sri.renci.org
idmapper.nodeNormalization.batchSize=500
idmapper.nodeNormalization.connectTimeout=10000
idmapper.nodeNormalization.requestTimeout=30000
idmapper.nodeNormalization.curiePrefixesCacheTtl=86400000
idmapper.nodeNormalization.maxPrefixGuesses=25
idmapper.nodeNormalization.useIdentifierFormatFilters=true
```
