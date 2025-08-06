OVERVIEW
========


The goal of this README is to provide a quick overview of the OSLC provider tests 
and how to easily use / modify them to validate an OSLC service provider.

The current work focused on OSLC-CM 1.0 and 2.0 implementations.  It can test 
both 1.0 and 2.0 implementations at the same time.  It is intended to work
with any OSLC Core-based implementations such as OSLC-QM and OSLC-RM 2.0.

## Running tests

### Eclipse

Running the tests in Eclipse:

Use an existing launch config created for known tests:
  - OSLC V2 RTC.launch
  - OSLC V2 CQ.launch
  
From Eclipse, _Run -> Run Configurations... -> JUnits_

### Maven

[Alternative]: running the tests with Maven on the command-line with something like :

    $mvn -Dtest=DynamicSuiteBuilder -DargLine="-Dprops=config/fusionforge/fusionforge-setupv1.properties" test


## Provider specific setup

- Note: for some configurations that use internal IBM hosted servers, you will
  first need to BSO-login before accessing the servers.
- Need to update config/*/*setup*.properties
  - Initial catalog URL
  - Template files for creation tests: config/cq/*template*
  - OAuth registration keys

## Adding tests

To add another tool (OSLC service provider) to test:

- setup a config/[tool] directory with configuration files 
  - start by copying an existing setup, like rtc
  - update the .properties file with tool specific info
  - update the template files with tool specific info
- create a launcher [optional]
   - can simply copy existing one, then change its name and location for 
      setup.properties

To add a tests for a domain:
- Update org.eclipse.lyo.testsuite.DynamicSuiteBuilder.suite() to 
  conditionally include the appropriate tests 
  
## Code Structure

The DynamicSuiteBuilder class is the driver of the suite, it determines which 
version of OSLC the provider supports and runs the appropriate set of tests 
against it. It does this by simply building an ArrayList of test classes to be 
run and returning it in the suite() method. Future test classes can be quickly 
added by adding them to the ArrayList for the appropriate version of OSLC that 
the class is testing.

When the DynamicSuiteBuilder and the various test classes are running, they pull 
the information about the OSLC service provider from the setup.properties file. 
The other properties files contained in the project are basically guidelines / 
templates to be swapped with the setup.properties file before running. The 
properties file itself contains comments mentioning what each field is used for.

Another thing to note is that right now, the OSLCv2 test suite has two test 
classes for query tests. The one the DynamicSuiteBuilder uses currently is the 
SimplifiedQueryTests, which just checks the basic format of returned query 
results but doesn't verify that certain results are present and correct. Once 
OSLC Shapes are more fully implemented and we can programmatically create 
records for all the QueryBase elements using the respective Shapes, the more 
comprehensive QueryTests class should probably be used instead. Also, the 
ChangeRequestTests in OSLCv2 requires that the creation factory have a record to
query on to actually retrieve a change request, this class as well as the 
CreationAndUpdateTests class can both be improved by utilizing shapes to 
programmatically create records on the fly.

## Authentication

The test suite supports multiple authentication methods:

- BASIC: Username/password authentication (default)
- FORM: Form-based authentication
- OAUTH: OAuth authentication

To configure the authentication method, set the `authMethod` property in your `setup.properties` file:

```
authMethod=BASIC
userId=yourUsername
pw=yourPassword
```

For BASIC auth, a sample configuration file is provided at `config/sample-setup-basic.properties`.

## Running BASIC Auth Tests

To run the BASIC auth tests, you can use the `BasicAuthTests` class. This class contains tests that verify BASIC auth functionality.

## Credits
