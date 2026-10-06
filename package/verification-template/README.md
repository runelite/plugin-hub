This project includes the dependency verification hashes used by all Plugin Hub plugins.
At the start of every build `core` dependencies are verified, but `thirdParty` deps are not.
To add a new dependency for a plugin add a new dep to the `thirdParty` configuration in this
`build.gradle` and run `../gradlew --write-verification-metadata sha256`. The maintainer MUST
then verify the hashes match Maven Central and that the library in question is reasonable.