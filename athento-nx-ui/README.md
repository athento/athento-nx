# Athento UI for Nuxeo #

## How to show a NXQL query document result

- Using QSearch (Athento Query Search) view, you can share a public link (securized by token-auth of NX) to show your query result. Use:

        GET /nuxeo/nxqsearch/default@qsearch?contentViewName=query_search&query=<your-nxql-query>&token=<your-token>
        
Also, you can find a link into NXQL search type in JSF interface with the link to QSearch view with the input query as url parameter.

## Save your contentView columns ##

- You can select and save your favourite columns. After logout and login, you will have your selected columns again.


## Preview without authentication ##

Preview is based on Restlet API with URL below:

        GET /nuxeo/restAPI/athpreview/{repoId}/{docId}/{fieldPath}?subPath=(subPath)&amp;token=(token)

where:

- repoId: is the repository name.
- docId: is the document identifier.
- fieldPath: is the xpath of content to preview.
- subpath: is use to reference images into preview (with no access token control)
- token: is the basic control access token based on changedToken of dublincore:modified metadata.

## Select a page in a ContentView ##

- You can introduce a page index into page-navigator to select your page in a contentView.


## Preview (pdf2htmlEX based) ##

### Prerequisites ###

You can find install and quickstart information here: https://github.com/coolwanglu/pdf2htmlEX

Use the ```pdf2htmlEX.sh``` script to make a full installation, and later:

- In your OS X: You need to set the config fonts with

        export FONTCONFIG_PATH=/opt/X11/lib/X11/fontconfig

- In Ubuntu (14.04 trusty):

 - Add to /etc/apt/sources.list the lines below:

                deb http://ppa.launchpad.net/fontforge/fontforge/ubuntu trusty main
                deb-src http://ppa.launchpad.net/fontforge/fontforge/ubuntu trusty main
                deb http://ftp.de.debian.org/debian sid main 

 - Now, execute:

                apt-get update
                apt-get install libfontforge-dev 

- In Ubuntu (16.04 xenial):
 - Download distribution from http://launchpadlibrarian.net/233283831/fontforge_20120731.b-7.1_i386.deb
                wget http://launchpadlibrarian.net/233283831/fontforge_20120731.b-7.1_i386.deb
 - Install dependencies
                wget http://launchpadlibrarian.net/233283831/fontforge_20120731.b-7.1_i386.deb
                apt-get install libpython2.7 libc6 libgdraw4 libfontforge1 fontforge-common
                apt-get -f install
 - Install local package
                dpkg -i fontforge_20120731.b-7.1_i386.deb
                
                
## Layouts y widgets

### Currency widget type

Example:

```xml
<widget name="totalAmount" type="currency">
    <labels>
        <label mode="any">label.total.amount</label>
    </labels>
    <translated>true</translated>
    <fields>
        <field>contable:totalamount</field>
    </fields>
    <properties widgetMode="view">
        <property name="type">currency</property>
        <property name="minFractionDigits">2</property>
        <property name="currencySymbol">€</property>
    </properties>
</widget>
```

### Workflow History tab

You can find the new Workflow History tab with the information about document workflow steps.
