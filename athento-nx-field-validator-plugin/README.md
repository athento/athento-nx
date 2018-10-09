# athento-nx-field-validator-plugin

## Synopsis

This Nuxeo plugin implements some common field validators for forms. It must be used from another plugin (the one you are developing).

Validations available: 
- validateEmailAddress
- validateHomePhoneNumber (must begin with 8 or 9)
- validateMobilePhoneNumber (must begin with 6 or 7)
- validateNIF (also checks for the correct letter) (also valid for NIE)
- validateNIFLazy (just check for 8 digits and a letter)

## Code Example

Use it in your contribution (extensions.xml):
```xml
	<widget name="EmailAddress" type="text">
		<labels>
			<label mode="any">label.email</label>
		</labels>
		<translated>true</translated>
		<fields>
			<field>MyApp:EmailAddress</field>
		</fields>
		<properties widgetMode="edit">
			<property name="validator">#{bFieldValidator.validateEmailAddress}</property>
		</properties>
	</widget>
```

Example for regex validation:
```xml
	<widget name="Contract" type="text">
		<labels>
			<label mode="any">label.contract</label>
		</labels>
		<translated>true</translated>
		<fields>
			<field>MyApp:Contract</field>
		</fields>
		<properties widgetMode="edit">
			<property name="validator">#{bFieldValidator.validateRegex}</property>
			<property name="regex">\d+\/\d{4}$</property> 
		</properties>
	</widget>
```


## Motivation

There are a lot of common validations across any web application that could be centralized in one plugin to contribute to Nuxeo platform.

## Installation

You just have to compile the pom.xml using Maven and deploy the plugin in 
```{r, engine='bash', count_lines}
cd nuxeo-field-validator-plugin
mvn clean install
cp target/fieldValidator-*.jar $NUXEO_HOME/nxserver/plugins
```
And then, restart your nuxeo server and enjoy.
