## Solr Schema (Re)Design

### Status

![Completeness Badge](https://img.shields.io/badge/Document_Level-Completed-green.svg?style=flat-square)

### Context
This document details the Solr schema improvements implemented on top of the changes described in the [SolrContentStore removal ADR](../solr-content-store-removal/00001-solr-content-store-removal.md). 
The starting context is the Solr Schema as described in [SEARCH-1707](https://issues.alfresco.com/jira/browse/SEARCH-1707).

Other than comments, examples and readability improvements, the big change introduced after the _SolrContentStore_ removal consisted of all 
fields having the "stored" flag set to true. That was needed for replacing the _SolrContentStore_ storage in favour of 
Solr built-in capabilities. 

After removing the _SolrContentStore_, the SearchServices Solr schema was composed by group of fields executing a different text analysis but sharing the 
same identical content.  
With that assumption, the logical step on top of the refactoring above has been to reduce the number of stored fields 
in order to 

- avoid duplicated content as much as possible   
- improve the indexing process 
- gain disk space

As you can imagine the most part of changes involved the Solr schema. Fields, specifically text fields, have been 
grouped by purpose, and for each group the number of stored fields has been reduced as much as possible. 

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
in this refactoring. In addition, there are also a set of fields which should be "theoretically" used for multi-content 
(content@m). The same consideration above is valid for those fields, as well: they haven't been involved in the refactoring 
because at the moment multi-content is not supported at all.  

The starting points of the investigation have been: 

- fields listed above are *all* marked as stored: it means their content is verbatim retained/copied in the index
- fields belonging to the same group have the same identical content  

After switching to Solr storage capabilities, we were been able to remove a huge set of customisations, but at the same 
time we realised we were storing the same content several times, therefore wasting a lot of disk space. 

In addition, the amount of data required for "representing" a document to be indexed included the same redundancy level, 
because the same text content were associated to each field in a group.     

#### Iteration #1: one single stored field per group
We decided to adopt an incremental approach for gradually removing as much as possible the stored fields in the schema. 
In a first iteration we've created one stored field for each group, and then we've used the copyField directive 
to have Solr copying the field value on all other indexed fields:

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

The indexing process is now simplified, because the _SolrInputDocument_ instance needs to provide a value only for 
those stored fields, and then before indexing Solr takes care about copying their value on the other fields of the same group. 
The drawback of this approach is: the stored field needs to have a value which can be used for creating (through copy) the 
value of the other fields: for example, if we have a value 
  
_"Hello I'm a title"_   

some destination fields (e.g. text@s____@*, text@s__t@*)just need to be filled with that literal value, while some others 
(e.g. text@s__lt@*, text@s__l_@*) require an additional information, a locale marker which consists of 

- a starting delimiter char \u0000 
- the locale language code 
- a closing delimiter char \u0000

This marker has to be set at the beginning of the field value, so in the example above the value becomes:

_"\u0000en\u0000Hello I'm a title"_   
   
Since it's not possible to interfere with the copy field directive in order to inject such prefix at index time, a different
approach has been adopted: the stored field **always** contains the prefix marker; fields that don't need that prefix
must provide an analyzer which strips it out. This is the main reason why you will find the following CharFilter inside
the text analysis of a lot of text fields: 

    <charFilter class="solr.PatternReplaceCharFilterFactory" pattern="(#0;.*#0;)" replacement=""/>
           
As a side note, there are some fields that cannot use the copy field approach. Specifically 

- fields used for sorting (mltext@m___sort): they have a special value set on _SolrInformationServer_ side which cannot be 
 rebuilt by Solr by simply copying the value of the stored field     
- fields with docValues enabled: mainly used for faceting, they cannot have the locale prefix marker but having docValues
enabled, they cannot have a TextField as a type. That means we cannot define an analyzer for removing that unwanted prefix 
before the field gets indexed.   
    
#### Iteration #2: disjoint set of stored fields    
The approach of the first iteration worked, but it had the following drawback: 

- we always had one stored field per group, even if that group was not used (that could be possible, depending on the Alfresco Data Model)
- each stored field was always copied on all the indexed fields belonging to its group, regardless their usage (for example we 
couldn't use the cross-locale versione of a specific field)  
    
So, we gained disk space in terms of stored content, but a different type of redundancy was introduced, for creating the 
inverted index for all copied fields.      
    
In order to overcome this new type of redundancy the definite version of the schema we've introduced a set of "disjoint" 
stored fields. "Disjoint" means within a group

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
fields. The naming adopted for those stored field follows the existing approach already in use for text and mltext fields.
Specifically:

t = tokenised
s = string (oppure _)
c = cross locale
s = sort
s = suggest

## Major changes
The previous sections gave a high-level "functional" overview of the most relevant changes in the Solr schema. 
However, there are also some other implementation details that need to be described for a better understanding of all 
components that participate in the new Solr data model. 
    
### Stored Fields Fragment Generator    
The [entire set](../../../src/main/resources/solr/instance/templates/rerank/conf/generated_copy_fields.xml) 
of disjoint stored fields is quite huge. Instead of manually creating all those fields, a [python procedure](../../../generator-solr-config/generate-solr-config.py) 
generates the XML fragment that is included at the end of the [solrconfig.xml](../../../src/main/resources/solr/instance/templates/rerank/conf/solrconfig.xml)       

    <xi:include href="solrconfig_insight.xml" xmlns:xi="http://www.w3.org/2001/XInclude">
        <xi:fallback/>
    </xi:include>    
    
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
text analysis. It is untokenised and its value is managed as a whole token.

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
Solr built-in TextField is not able to switch the text analysis at runtime, so the 

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