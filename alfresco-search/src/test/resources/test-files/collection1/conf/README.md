The folder contains several Solr configuration files that are used within the test suite. 
Note that the folder, although it has the structure that Solr expects (<core name>/conf), it is not a complete "core" 
folder, because 

- some files are missing (e.g. schema.xml)
- some files are used only in specific tests (e.g. solrconfig-rerank.xml, schema-rerank.xml)

During the build process, Maven creates a complete core definition under the test build output folder by merging the 
configuration of the "Rerank" template (src/main/resources/templates/rerank) together with the content of this folder.

Note that this folder is copied **after** the template, so duplicates will be overwritten. For example, both folders 
have a *solrcore.properties* and a *solrconfig.xml*: the test execution will use are those in this folder (because they 
will overwrite the same files in the rerank template).

