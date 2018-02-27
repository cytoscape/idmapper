Copy "idmapper-3.6.1.jar" from here to ~/Applications/Cytoscape_v3.6.0/apps

#idmapper

BridgeDB based Identifier mapping

Version 3.6

This is a Cytoscape app that allows identifier mapping on tables.   Once this app is installed, the table browser will respond to a right click on the column header with a pop-up menu containing the item **Map Column...**  

A dialog is shown to gather the information about the species, and which columns you want to map **From** (the source) and **To** (the target).  By default, multiple return values will be simplified to the first in the list, but turning off the **Force Single** checkbox will maintain all returned IDs.

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
