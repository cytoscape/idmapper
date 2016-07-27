# idmap-impl
Identifier mapping

This is a Cytoscape app that allows identifier mapping on tables.   Once this app is installed, the table browser will respond to a right click on the column header with a pop-up menu containing the item **Map Column...**  

A dialog is shown to gather the information about the species, and which columns you want to map **From** (the source) and **To** (the target).  By default, multiple return values will be simplified to the first in the list, but turning off the **Force Single** checkbox will maintain all returned IDs.

Once the dialog is confirmed, a new column will be added to the Node Table containing the mapped identifiers.

The active service  (either Kei's mapping service or BridgeDb) is set in MapColumnTaskFactoryImpl. Line 17

__**BridgeDB** is the default service.__ and is expected to be the exclusive service in the core app.

Note that we are pointed at __/webservice.bridgedb.org:8185__ in BridgeDbIdMapper:33.  This is a test server.
