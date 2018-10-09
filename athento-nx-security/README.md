# Athento Security for Nuxeo

## Dynamic ACLs

Specify the ACLs of documents on creation and modification. You can define you DynamicACLs for your **document types** or _facets_ and when it will be applied.

- Component is: _org.nuxeo.athento.athento-nx-security.DynamicACLService_
- Extension Point is: _dynamicAcl_

### Default For folderish facet

When a folderish is created, DynamicACLs service includes three ACEs to manage the permissions in the future. It improves the performance in the propagation process.

```
<extension target="org.nuxeo.athento.athento-nx-security.DynamicACLService"
           point="dynamicAcl">
           
    <dynamicAcl name="local" overwrite="false">
        <facet>Folderish</facet>
        <ace principal="@{doc.getPropertyValue('dc:title') + '_readers'}" />
        <ace principal="@{doc.getPropertyValue('dc:title') + '_writers'}" permission="ReadWrite"/>
        <ace principal="@{doc.getPropertyValue('dc:title') + '_admin'}" permission="Everything"/>
    </dynamicAcl>
    
</extension>
```

### Examples

Examples for dynamic with "Read" permission when it is undefined. And another with set permission.
            
Check below the principal string with a dynamic MVEL expression based in a document context and the rules to apply as well.
ComplexField and complextPattern are used for complex field. Get and replace the wildcard % for complex value into pattern whether it is defined.
Also, you can overwrite the ACL, and block the parent inheritance.

```
<extension target="org.nuxeo.athento.athento-nx-security.DynamicACLService"
           point="dynamicAcl">
           
    <dynamicAcl name="dynamic1" acl="myDynamicAcl1">
        <doctype>File</doctype>
        <ace principal="expr:@{'readers_' + doc.getPropertyValue('dc:title')}" permission="ReadWrite">
            <rule>doc.getPropertyValue('dc:title') == 'FileTest'</rule>
        </ace>
        <ace principal="expr:@{doc.getPropertyValue('complex:property')}" complexField="mapValue"
             complexPattern="%_readers" permission="ReadWrite" />
    </dynamicAcl>
    <dynamicAcl name="dynamic2" acl="myDynamicAcl2" overwrite="true" blockInheritance="true">
        <doctype>File</doctype>
        <doctype>Image</doctype>
        <ace principal="expr:@{'readers_' + doc.getPropertyValue('dc:title')}" permission="ReadWrite" />
        <ace principal="MyGroup" permission="Everything" />
    </dynamicAcl>
    
</extension>
```

## Document security

- Apply athentosec security schema to apply access control with:
  - Principals
  - Signed tokens
  - IPs
  - Content xpath
  - Expiration date

### Requirements

- Bouncy castle lib 1.49+ (bcprov-jdk15on.jar)

### Properties

- _cipher.key_: it is a mandatory property used to encrypt the information.
- _password.lastmodification.date_: it is the default last modification date.
- _password.expiration.days_: it is the number of days to check expirated passwords.
- _password.oldpassword.days_: it is the number of days to check old passwords.


## Mimetype control

This addon is able to check the upload file mimetypes before creation. To manage it, it has the properties below:

- **plugin.athento-nx-security-limit-file-upload-mime-types.documentTypesChecked [comma separated values|Empty]**: it indicates a list of document types to check the mimetype before creation. Empty or undefined for apply to all document types.
- **plugin.athento-nx-security-limit-file-upload-mime-types.enabled [True|False]**: to enable the mimetype check control.
- **plugin.athento-nx-security-limit-file-upload-mime-types.mimeTypesAllowed [comma separated values|Empty]**: it indicates a list of mimetypes allowed. Empty or undefined to check the default mimetypes below.

Default mimetypes to check are:

 * application/pdf, application/x-gzip, application/csv
 * application/vnd.oasis.opendocument.text
 * text/xml, text/html, text/plain, text/rtf, text/csv, text/css
 * application/msword, application/msexcel, application/vnd.ms-excel, application/vnd.ms-powerpoint
 * application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, application/vnd.openxmlformats-officedocument.spreadsheetml.template, application/vnd.sun.xml.writer, application/vnd.sun.xml.writer.template, application/vnd.oasis.opendocument.text, application/vnd.oasis.opendocument.text-template
 * audio/ogg, video/ogg, application/ogg, audio/wav, audio/aac, audio/midi, audio/mp3
 * application/wordperfect, application/rtf
 * video/mpeg, video/quicktime, application/visio, video/x-msvideo
 * image/gif, image/png, image/jpg, image/jpeg, image/tiff
