# MIRCVProject
**Pay Attention** : We have used JDK 17.
You can use the following project to:

## Index MSMARCOPassages Collection with INDEXCREATOR.JAVA as Main Class
When selecting this as main class, It is required for you to have the collection inside a ./data folder. 
You will be prompted for choosing scoring function (TFIDF, BM25) and the output format (TXT, BINARY, COMPRESSED).
- Choose TXT if you want to debug your index even visually (slowest)
- When choosing COMPRESSED, the compression strategy adopted is Simple9 for Document Ids and Unary for term frequencies

## Query the IR with GUI.JAVA as Main Class
When selecting this as main class, It is required for you to have first built an index using IndexCreator.
The demo interface will ask you for query continuosly, until you voluntary quit. By default, the passage retrieved is not displaied unless requested.
Be sure to have the entire collection inside a ./data folder if you want to retrieve text from PIDs.

**Important Note**: The GUI assumes by default you have a compressed Index stored, change the value of Constants.Compression if you want to query the system with other output formats.

## Constants.java
Feel free to navigate and modify constants to control the compression strategy and some important constraints of the project, such as memory limit
or block dimensions.

