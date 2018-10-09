# Description

Athento Tools to make Nuxeo Upgrades and manage statistics.

## Statistics

### Athento.CalculateStats

**What does it do**

Calculate all stats for a list of document types in an existing path.

**Operations**

### Calculate Statistics for all indicators for a list of document types

`POST` http://localhost:8080/nuxeo/site/automation/Athento.CalculateStats

```json
{ 
  "params": {
  	"repository": "default",
  	"doctypes": "File,Contract,Image,OtherDocType",
  	"path": "/"
  }
}
```

_where_:

* _repository_ (default value is "default"): is the repository to calculate stats.
* _doctypes_: is a list of document types to calculate sstats.
* _path_ (default value is "/"): is the defined path in repository to calculate the stats.

`Response OK is a string with the calculation status.`

### Calculate Statistics for a stat indicator

`POST` http://localhost:8080/nuxeo/site/automation/Athento.CalculateStat

```json
{ 
  "params": {
  	"stat": "Documents",
  	"statName": "Total Documents",
  	"path": "/"
  }
}
```

_where_:

* _stat_: is the stat name. You can see below a list of available stats.
* _statName_: list of indicator name to calculate.
* _path_ (default value is "/"): is the defined path in repository to calculate the stats.

`Response OK is a string with the calculation status.`

Available stats and statNames (indicators)

Stat **Documents**
 
* Total Folders
* Total active Folders
* Total Folders not deleted
* Total active File not deleted with content
* Total Documents
* Total active Documents
* Total Documents not deleted
* Total active File not deleted with content
* Total for _doctype_
* Totals active for _doctype_
* Totals active not deleted for _doctype_
* Totals active with content for _doctype_

Stat **Users and Groups**

* Users
* Groups

Stat **Disk**

* Disk

### Get current statistics

`POST` http://localhost:8080/nuxeo/site/automation/Athento.GetStats

```json
{ 
}
```

_where_: _there is no params in the call_.

`Response OK is a JSON with all stats and totals.`

## Maintenance

### Athento.ACLOptimization

**What does it do**

Launch DB functions to optimize ACLs.

**Available function parameters**

* REBUILD_READ_ACLS: Launch rebuild ACLs db procedure.
* UPDATE_READ_ACLS: Launch update read ACLs db procedure.

**Example**

`POST` http://localhost:8080/nuxeo/site/automation/Athento.ACLOptimization

```json
{ 
  "params": {
  	"function": "REBUILD_READ_ACLS"
  }
}
```

**Response**

`OK`

### Athento.UpgradeACL

**Requirements**

```xml
<dependency>
    <groupId>org.yerbabuena.athento.common</groupId>
    <artifactId>athento-manager-common</artifactId>
    <version>3.2.2.2</version>
</dependency>
```

**What does it do**

Transform denied permissions in granted permissions using worker manager.

**How to use**

Launch the operation using a POST automation call. You only need the input document reference to start the upgrade ACLs from him. So, use `"input"` with the id or path reference.

The parameters are:

* aclName (mandatory): it is the name of ACL where it upgrades the denied permissions.
* onlyFolder (optional, default is _true_): it indicates the upgrade work will be only in _folderish_ documents.
* save (optional, default is _false_): you can use this flag to show a previous result into a temporal file result. This file will be into `/tmp` directory and its name will be `upgrade-${timestamp}`

Result is an `OK` string if there is no error.

**Example**

`POST` http://localhost:8080/nuxeo/site/automation/Athento.UpgradeACL

Raw in request:

```json
{ 
  "input": "87781dbf-12a6-4aa0-9b29-90a34de57413",
  "params": {
  	"onlyFolder": true,
  	"save": false
  }
}
```

**Response**

`OK`

**File result block**

```json
{
  "node": "doc=default-domain, denied=null, granted=[Administrator:Everything:true, members:Read:true, members:CanAskForPublishing:true]",
  "children": [
    {
      "node": "doc=sections, denied=null, granted=null"
    },
    {
      "node": "doc=templates, denied=null, granted=null"
    },
    {
      "node": "doc=workspaces, denied=null, granted=[Administrator:Everything:true, members:Read:true]",
      "children": [
        {
          "node": "doc=Workspace1, denied=null, granted=[Administrator:Everything:true, members:Read:true]",
          "children": [
            {
              "node": "doc=WorkFolder, denied=null, granted=null"
            }
          ]
        }
      ]
    }
  ]
}
```


