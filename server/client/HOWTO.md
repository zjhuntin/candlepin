Here are instructions on how to use Swagger to generate a client.

Before you start, you must have the swagger-codegen library installed.  The
easiest way to do this is to grab it with Maven.

```
$ mvn dependency:get -DgroupId=io.swagger -DartifactId=swagger-codegen-cli -Dversion=2.2.1 -Dpackaging=jar
```

1. Grab the `swagger.json` file.  To do this you must have Candlepin deployed
   and running!

   ```
   $ wget --no-check-certificate --user admin --password admin https://localhost:8443/candlepin/swagger.json
   ```

2. Generate the client

   ```
   $ java -jar ~/.m2/repository/io/swagger/swagger-codegen-cli/2.2.1/swagger-codegen-cli-2.2.1.jar generate -i swagger.json -l ruby -o swagger-client
   ```

   More information on swagger-codegen is available at
   http://swagger.io/docs/#swagger-codegen-documentation-9


# Using the Generated Client

* Make sure resource parameters that take an object are given a proper
  name and marked as required if they are indeed required.  Providing a
  name means that in the generated client code, the method will take
  the object as a parameter instead of having to pass in a hash with
  an options hash containing a :body key
