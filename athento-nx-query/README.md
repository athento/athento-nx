[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7f3dae7055c749e7873a67c0ad787a09)](https://www.codacy.com?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=athento/athento-nx-query&amp;utm_campaign=Badge_Grade)

# athento-nx-query

This plugin adds some new features that improve queries in Nuxeo

# Extended NXQL

When you install this plugin, you can use an extended version of NXQL. The features available are:

## Format date

### Examples

```
SELECT ecm:uuid, invoice:invoice_date::date FROM Invoice WHERE ...
```

where:

* invoice:invoce_date is a date field of Invoice document type

using:

* _date_ to get a full date format (yyyy-MM-dd HH:mm:ss)
* _shortdate_ to get a short date format (yyyy-MM-dd)

You can change the time result, adding or substracting hours. With date-1 to get GMT+1 or date--1 to get GMT-1.

For example, 

- to get invoice:invoice_date in GMT-5, you can use:
```
SELECT ecm:uuid, invoice:invoice_date::date--5 FROM Invoice WHERE ...
```

- to get invoice:invoice_date in GMT+2, you can use:
```
SELECT ecm:uuid, invoice:invoice_date::date-2 FROM Invoice WHERE ...
```


## Vocabulary values

Nuxeo stores vocabulary keys in the metadata of documents, and that is what is returned as values in queries from NXQL. However, the label of the vocabulary entry is usually needed.

This plugin includes a reserved word "vocabulary" to query the label of the entry referenced by a metadata.

### Example

```
SELECT ecm:uuid, invoice:type::vocabulary::invoicetypes FROM Invoice WHERE ...
```

where:

* invoice:type is a field that references a vocabulary
* vocabulary is a reserved word
* invoicetypes is the name of the vocabulary


## Related documents

If you have a field that is of type Document, you can navigate through it by using an extended version of field in the SELECT. For example:

```
  SELECT ecm:uuid, invoice:payment_doc::document::payment:payment_date FROM Invoice WHERE ...
```  
  
In the example above:

* invoice:payment_doc is a field in the schema invoice (in the storage level, it is an uuid)
* document is a reserved word that tells to query the related document
* payment:payment_date is the field that you want to retrieve in the query

You can use nesting of clauses, for example:

```
  SELECT ecm:uuid, invoice:payment_doc::document::payment:provider_doc::document::billing:address FROM Invoice WHERE ...
```  

where:

* payment:provider_doc is a field of payment schema used to define the product provider, defined as document with uuid too
* document is a reserved word again for this field
* billing:address is the field of billing address for invoice provider

Also, you can use the reserved fields:

* ecm:uuid to get uuid
* ecm:parentId to get parent uuid
* ecm:primaryType to get document type
* ecm:currentLifeCycleState to get the lifecycle
* ecm:isProxy to get if document is a proxy
* ecm:isVersion to get if document is a version

Example (to get the lifecycle of provider in queried invoices)

```
  SELECT ecm:uuid, invoice:payment_doc::document::payment:provider_doc::document::ecm:currentLifeCycleState FROM Invoice WHERE ...
```  

### Response

Extended NXQL is able to get nested information from related documents, and the responses always contains the completed path of field using the reserved characters '>>'.

Example:

```
invoice:payment_doc>>payment:provider_doc>>ecm:currentLifeCycleState_0": "Active",
```

Pay attention suffix **_#**, used for manage repeat field in response.
   
## User information
 
Examples

```
  SELECT ecm:uuid, invoice:approved_by::user FROM Invoice WHERE ...
```  
 
where:

* invoice:approved_by is the a user entity referenced in the document
* user is the reserved word to get username of user
 
```
  SELECT ecm:uuid, invoice:approved_by::user::firstName FROM Invoice WHERE ...
```  
 
where:
 
* firstName is the firstname of user

also, you can use:

* lastName
* fullName to get the firstName and lastName
* email
* ...other available user schema field

