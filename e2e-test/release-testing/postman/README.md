# Postman Collections for Release Testing

The files in `files` need to be placed in your Postman working directory before you
execute the test suite.

Within the collection then the `DataSetUp` methods must be run before the `TestCases`
and Solr needs a bit of time so that it can index the new documents.
