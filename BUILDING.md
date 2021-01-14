# Building LXTK

The build is based on [Apache Maven][1] and [Eclipse Tycho][2]
and is easy to run on a local machine:

 1. Make sure you have JDK 8 and Maven 3.5 or above installed.
 Both should be on the path.

 2. Make sure you have a local clone of the LXTK Git repository.

 3. Open a shell to the local clone of the LXTK Git repository and execute

    `$ cd releng`

    `$ mvn clean verify`

Once the build completes, the `repository/target` folder will contain
a repository of build artifacts.

[1]: https://maven.apache.org/
[2]: https://www.eclipse.org/tycho/
