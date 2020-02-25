## Solr Schema (Re)Design

### Status

![Completeness Badge](https://img.shields.io/badge/Document_Level-Completed-green.svg?style=flat-square)

### Context
This document details the Solr schema improvements implemented on top of the changes described in the [SolrContentStore removal ADR](../solr-content-store-removal/00001-solr-content-store-removal.md).   
The starting context is the Solr Schema as described in [SEARCH-1707](https://issues.alfresco.com/jira/browse/SEARCH-1707).

Other than additional comments, examples and readability improvements, the big change introduced consisted of all 
fields having the "stored" flag set to true. That was needed for replacing the _SolrContentStore_ storage in favour of 
Solr built-in capabilities. 

The SearchServices Solr schema is composed by group of fields executing a different text analysis but sharing the 
same identical content. For example, the following group:

    <!-- Note: prior to this task all these fields were stored -->
    
    <dynamicField name="text@s__l_@*" type="alfrescoFieldType" omitNorms="true" />
    <dynamicField name="text@s__lt@*" type="alfrescoFieldType" omitNorms="false" />
    <dynamicField name="text@s___t@*" type="text___" />
    <dynamicField name="text@s__sort@*" type="alfrescoCollatableTextFieldType" />
    <dynamicField name="text@sd___@*" type="identifier" stored="true" docValues="true" />

is related to single valued text attributes. For instance, a "name" attribute at indexing time uses that group 
in order to generate the following fields: 

    text@s__l_@{http://www.alfresco.org/model/content/1.0}name    
    text@s__lt@{http://www.alfresco.org/model/content/1.0}name     
    text@s___t@{http://www.alfresco.org/model/content/1.0}name     
    text@s__sort@{http://www.alfresco.org/model/content/1.0}name     
    text@sd___@{http://www.alfresco.org/model/content/1.0}name    

The logical step on top of this refactoring has been to reduce the number of stored fields 
in order to 

- **avoid duplicated content** as much as possible   
- **improve** the indexing process 
- **gain** disk space 

### Solr Schema Fields Logical Grouping
Four main groups have been identified: 

1. fields starting with "text@s" (s stands for single valued)   


      <dynamicField name="text@s____@*" type="identifier" />
      <dynamicField name="text@s__l_@*" type="alfrescoFieldType" omitNorms="true" />
      <dynamicField name="text@s__lt@*" type="alfrescoFieldType" omitNorms="false" />
      <dynamicField name="text@s___t@*" type="text___" />
      <dynamicField name="text@s__sort@*" type="alfrescoCollatableTextFieldType" />
      <dynamicField name="text@sd___@*" type="identifier" stored="true" docValues="true" />


2. fields starting with "text@m" (multivalued)
   
   
      <dynamicField name="text@m____@*" type="identifiers" />
      <dynamicField name="text@m__l_@*" type="alfrescoFieldType" multiValued="true" />
      <dynamicField name="text@m__lt@*" type="alfrescoFieldType" multiValued="true" />
      <dynamicField name="text@m___t@*" type="text___" multiValued="true" />
      <dynamicField name="text@md___@*" type="identifiers" stored="true" docValues="true" />

3. fields starting with "mltext@m" 


      <dynamicField name="mltext@m____@*" type="identifiers" />
      <dynamicField name="mltext@m__l_@*" type="alfrescoFieldType" multiValued="true" />
      <dynamicField name="mltext@m__lt@*" type="alfrescoFieldType" multiValued="true" />
      <dynamicField name="mltext@m___t@*" type="text___" multiValued="true" />
      <dynamicField name="mltext@m__sort@*" type="alfrescoCollatableMLTextFieldType" />

4. fields starting with "content@s" 


        <dynamicField name="content@s____@*" type="identifier" termPositions="false" />
        <dynamicField name="content@s__l_@*" type="alfrescoFieldType" termPositions="false" />
        <dynamicField name="content@s__lt@*" type="alfrescoFieldType" />
        <dynamicField name="content@s___t@*" type="text___" />

Note SearchServices schema includes also other fields (e.g. static fields, primitive fields): they haven't been involved
in this refactoring.   
The starting points of the investigation have been: 

- fields listed above are *all* marked as stored: it means their content is verbatim retained/copied in the index, therefore generating a lot of redundancy
- fields belonging to the same group have the same identical content (again, a lot of redundancy)  

After switching to Solr storage capabilities, we were been able to remove a huge set of customisations, but at the same 
time we realised we were storing the same content several times.  
In other words, the amount of data required for "representing" a whole document inevitably required a high redundancy degree, 
because the same text content was associated to all fields belonging to a give group.     

We decided to adopt an incremental approach for gradually reducing as much as possible the number of stored fields in the schema. 

#### Iteration #1: one single stored field per group
In a first iteration we've created one stored field for each group, and then we've used the copyField directive 
to have Solr copying the field value on all other indexed (but this time not stored) fields:

      <!-- Group 1: TEXT "S" -->
      <dynamicField name="text@s_stored@*" type="identifier" stored="true" indexed="false/>
      
      <dynamicField name="text@s____@*" type="identifier" stored="false"/>
      <dynamicField name="text@s__l_@*" type="alfrescoFieldType" stored="false" />
      <dynamicField name="text@s__lt@*" type="alfrescoFieldType" stored="false" />
      <dynamicField name="text@s___t@*" type="text___" stored="false" />
      <dynamicField name="text@s__sort@*" type="alfrescoCollatableTextFieldType" />
      <dynamicField name="text@sd___@*" type="identifier" stored="true" docValues="true" />
      
      <copyField source="text@s_stored@*" dest="text@s____@*"/>
      <copyField source="text@s_stored@*" dest="text@s__l_@*"/>
      <copyField source="text@s_stored@*" dest="text@s__lt@*"/>
      <copyField source="text@s_stored@*" dest="text@s___t@*"/>
      <copyField source="text@s_stored@*" dest="text@s__sort@*"/>  
      
      <!-- Groups 2 and 3 follows the same logic... --> 
      
      ...
      
      <!-- Group 4: MLTEXT -->
      
      <dynamicField name="mltext@m_stored@*" type="identifiers" stored="true" indexed="false/>
      
      <dynamicField name="mltext@m____@*" type="identifier" multiValued="true" stored="false"/>
      <dynamicField name="mltext@m__l_@*" type="alfrescoFieldType" omitNorms="true" multiValued="true" stored="false"/>
      <dynamicField name="mltext@m__lt@*" type="alfrescoFieldType" omitNorms="true" multiValued="true" stored="false"/>
      <dynamicField name="mltext@m___t@*" type="text___" multiValued="true" stored="false" />
      
      <dynamicField name="mltext@m__sort@*" type="alfrescoCollatableMLTextFieldType" />
      
      <copyField source="mltext@m_stored@*" dest="mltext@s____@*"/>
      <copyField source="mltext@m_stored@*" dest="mltext@m__l_@*"/>
      <copyField source="mltext@m_stored@*" dest="mltext@m__lt@*"/>
      <copyField source="mltext@m_stored@*" dest="mltext@m___t@*"/>

The indexing process is simpler than before, because the _SolrInputDocument_ instance needs to provide a value only for 
those stored fields, and then before indexing Solr takes care about copying their value on the other fields of the same group.   
The drawback of this approach is: the stored field has one value which must contain all the information needed for creating the other fields. 
Although that could sound trivial, the different cardinality between the source and the destinations could lead some issue related 
with the different usage of the target fields.   
For example, the value of the stored field is:   
  
_"Hello I'm a title"_   

some target fields (e.g. text@s____@*, text@s__t@*) just need to be filled with that literal value, while some others 
(e.g. text@s__lt@*, text@s__l_@*) require an additional information, specifically a locale marker which consists of 

- a starting delimiter char \u0000 
- the locale language code 
- a closing delimiter char \u0000

This marker has to be set at the beginning of the field value, so in the example above the value those fields expect is:

_"\u0000en\u0000Hello I'm a title"_   
   
Since it's not possible to interfere with the copy field directive in order to inject such prefix at index time, a different
approach has been adopted: the stored field **always** contains the prefix marker; fields that don't need that prefix
must provide an analyzer which strips it out. This is the main reason why you will find the following CharFilter inside
the text analysis of a lot of existing field types: 

    <charFilter class="solr.PatternReplaceCharFilterFactory" pattern="(#0;.*#0;)" replacement=""/>
           
As a side note, there are some fields that cannot use the copy field approach. Specifically 

- fields used for **sorting** (mltext@m___sort): they have a special value set on _SolrInformationServer_ side which cannot be 
 rebuilt by Solr by simply copying the value of the stored field     
- fields with **docValues** enabled: mainly used for faceting, they cannot have the locale prefix marker but having docValues
enabled, they cannot have a TextField as a type. That means we cannot define an analyzer for removing that unwanted prefix 
before the field gets indexed.   

Those field have been marked as stored or docValues and they are sent in the documents as usual, without relying on the copyField
directive.
    
#### Iteration #2: disjoint set of stored fields    
The approach of the first iteration worked, but it had the following drawback: 

- we always had one stored field per group, even if that group was not used (that could be possible, depending on the Alfresco Data Model)
- each stored field was always copied on all the indexed fields belonging to its group, regardless their usage (for example, we 
don't need the cross-locale version for a specific field)  
    
So, we gained disk space in terms of stored content, but a different type of redundancy was introduced, for creating the 
inverted index for all copied fields.      
    
In order to overcome this new type of redundancy a set of "disjoint" stored fields has been introduced.   
"Disjoint" means, within a group,

- we've introduced several stored fields, one per possible usage scenario       
- each stored fields is copied only on those indexed fields that map the usage scenario  

These fields are automatically generated by a [python procedure](../../../generator-solr-config/generate-solr-config.py) 
and included in the solrconfig.xml using the xi:include directive.    
Here's an extract of the [included fragment](../../../src/main/resources/solr/instance/templates/rerank/conf/generated_copy_fields.xml). :   

    <!-- Ex. 1: the field value is used only for suggestions -->
    <dynamicField name="text@s_stored_____s@*" type="localePrefixedField" />
    <copyField source="text@s_stored_____s@*" dest="suggest" />
    
    ...
    
    <!-- Ex. 2: the field value is used for suggestions and cross-locale searches -->
    <dynamicField name="text@s_stored_t___s@*" type="localePrefixedField" />
    <copyField source="text@s_stored_t___s@*" dest="text@s__lt@*" />
    <copyField source="text@s_stored_t___s@*" dest="suggest" />
    
    ...
    
    <dynamicField name="text@s_stored_t_c_s@*" type="localePrefixedField" />
    <copyField source="text@s_stored_t_c_s@*" dest="text@s__lt@*" />
    <copyField source="text@s_stored_t_c_s@*" dest="text@s___t@*" />
    <copyField source="text@s_stored_t_c_s@*" dest="suggest" />
    
    ...
    
    <dynamicField name="text@s_stored_tscss@*" type="localePrefixedField" />
    <copyField source="text@s_stored_tscss@*" dest="text@s__lt@*" />
    <copyField source="text@s_stored_tscss@*" dest="text@s___t@*" />
    <copyField source="text@s_stored_tscss@*" dest="text@s__l_@*" />
    <copyField source="text@s_stored_tscss@*" dest="text@s__sort@*" />
    <copyField source="text@s_stored_tscss@*" dest="text@s____@*" />
    <copyField source="text@s_stored_tscss@*" dest="suggest" />
    
As you can see we are several stored fields that, depending on the target usage, are copied across different searchable
fields. The naming adopted for the new stored fields follows the existing approach already in use for "text" and "mltext" fields.
Specifically, the stored field name is structured as follows:

    field_type_name@(s|m)_stored(t|_)(s|_)(c|_)(s|_)(s|_)@local_name

where 

- field type name can be "text" or "mltext"
- (s|m): single value or multivalued field 
- "_stored": static part which denotes a stored field 
- (t|_) tokenised usage (t) or not (\_)
- (s|_) string usage (s) or not (\_) 
- (c|_) cross locale usage (c) or not (\_) 
- (s|_) sort usage (s) or not (\_)
- (s|_) suggestable usage (s) or not (\_)

## Major changes
The previous sections gave a high-level "functional" overview of the most relevant changes in the Solr schema. 
However, there are also some other implementation details that need to be described for a better understanding of all 
components that participate in the new Solr data model. 
    
### Stored Fields Fragment Generator    
The [entire set](../../../src/main/resources/solr/instance/templates/rerank/conf/generated_copy_fields.xml) 
of disjoint stored fields is quite huge. Instead of manually creating all those fields, a [python procedure](../../../generator-solr-config/generate-solr-config.py) 
generates the XML fragment that is included at the end of the [schema.xml](../../../src/main/resources/solr/instance/templates/rerank/conf/schema.xml)       

    <xi:include href="generated_copy_fields.xml" xmlns:xi="http://www.w3.org/2001/XInclude">
        <xi:fallback/>
    </xi:include>    

The procedure is a very simple script which can be executed without any parameter at all

    python generate-solr-config.py
 
It generates a 
[generated_copy_fields.xml file](../../../src/main/resources/solr/instance/templates/rerank/conf/generated_copy_fields.xml) 
within the execution folder. You can generate that file once and them move it into the rerank template configuration folder. 
The execution is manual, it hasn't been bound at build time because there's no need to re-execute it for every build.   
    
### Custom StrField
We've already mentioned that stored fields contain a language marker at the beginning of their value. That is needed because
a stored field will be used at indexing time (by means of copyField directive) for feeding the other searchable (and not stored)
fields. A target field could need that prefix (that is the case of all fields having AlfrescoFieldType as type) or not.
In this latter case we could be in one of the following scenarios: 

- the field type is a TextField (or a subclass): it must provide in its analyzer a char/token filter which removes the prefix
- the field type is a StrField: it's not possible to define a text analysis for these fields, so in order to overcome 
that limit the _org.alfresco.solr.StripLocaleStrField_ custom type has been introduced. It is a subclass of Solr _StrField_ which removes the locale prefix 
from the value that will be stored. 


    <fieldType 
        name="stripLocaleStrField" 
        class="org.alfresco.solr.StripLocaleStrField" 
        indexed="true" 
        stored="false"/>
    
    ...
    
    <dynamicField name="text@s____@*" type="stripLocaleStrField"/> 

This is the type used for untokenised and indexed fields.  

### Custom TextField for stored fields  
Stored fields are used, as the name suggests

- for retaining a verbatim copy of the original text content
- at field retrieval (i.e. query response) time
- for some complementary search features like Highlighting and More Like This (not yet implemented)

Usually, the field type of a stored field is a _StrField_ (or one subclass), that is: a field type which doesn't provide any
text analysis. It is untokenised and its value is managed as a whole single token.

The Solr Highlighting search feature uses the stored value for computing the highlight snippets: it needs to analyze the 
stored content using the index analyzer defined for the field type. If the field type doesn't provide any tokenisation, 
the highlighting won't return any snippet.   
This is the main reason why in SearchServices stored fields cannot be "StrField": a special TextField field type is needed, 
it must be able to provide the proper text analysis depending on the language prefix put at the beginning of the field value.

Let's see an example. Suppose we have a simple document, with just one field:  

    {
        "description": "We were discussing about an interesting topic"
    } 

The user requests to highlight the term "discuss". He would expect "discussing" to be highlighted: 

_"we were **discussing** about an interesting topic"_       

In other words, the highlighting process is expected to take care about the field language during the tokenisation. 
Solr built-in TextField type is not able to switch the text analysis at runtime, so the 

_org.alfresco.solr.schema.highlight.LanguagePrefixedTextField_ 

has been introduced in order to fulfill that requirement. The field type analyses the locale marker and determines the 
analyzer that will be used looking at: 

- a field type called "highlighted_text_" + locale (e.g. highlighted_text_en) 
- if it's not found, then it checks if a field type called "text_" + locale is defined (e.g. text_en)
- if it cannot be found, it uses the cross locale field type "text___"  

The runtime computed analyzer makes sure the text analysis (for the highlighting) will always match the locale information
within the field value.  

### Highlighter 
The Alfresco Highlighter is a customisation of the Solr Default Highlighter. The most relevant changes that affected this
component has been described in the _SolrContentStore_ removal [ADR](../solr-content-store-removal/00001-solr-content-store-removal.md).
In this context it is important to underline the stored fields management described above widely reduced its complexity: 
prior to that, for each field interested in the highlight process, the Highlighter executed in the worst case two requests:

- a first request using the cross-locale field
- a second request using the locale-specific field        

Since the new data model provides a single stored version of each field, the Highlighter executes only a single request 
using that field.

## Closing Remarks
A precise measure of the performance improvements introduced is out of the scope of this task because it would require a proper 
benchmark infrastructure. This section is more a summary which lists the major relevant impacts that should be taken in 
account when a comparison/benchmark will be done between the "pre" and "post" SolrContentStore SearchServices versions. 

### Query time

- **field retrieval**: a user requested field is quickly associated to the (single) corresponding stored field   
- **highlighting**: instead of doubling the highlighting process (first cross-locale and then locale specific fields) there's just one execution 
which targets the unique stored field (per attribute)

### Indexing time
In general we expect 

- a higher indexing throughput   
- less disk space required for storing the index datafiles: 

These are the points that contribute to the list above: 

- **atomic updates** reduce the amount of data sent to Solr on partial updates   
- **the amount of data sent** at indexing time is smaller: only the stored fields are sent
- **the merge logic** (in case of document update) has been centralised in Solr
- **the amount of stored data in the index is smaller**: there's just one stored field per Alfresco attribute. This can be very important in 
those scenarios where the same field has more than one "usage" (e.g. cross-language, language specific and suggestions). 
Prior to that we had one copy of the same stored content for each usage, while now there's always one single stored field.      