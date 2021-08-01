Instructions about how to use this application with IBM Jazz Rational Asset Manager (RAM).

The application accepts two arguments:
1) The location of the properties files
2) -z flag

The first argument must be passed everything and must be the first argument passed. This tells
the application where the properties file is that contains the settings used to generate the
configuration file for the lyo tests. To learn more about the various settings please read the
exampleProperty.properties file.

The second argument tells the application to create a zip file instead of a folders and files.

Note that this application creates configurations for Lyo tests to run against RAM APIs.
You can modify the generated files to run against other APIs that implement the asset management
specification but this application will not do that for you.

Also the templates folder must be in the root directory in order for the tool to find it.