




本地打包 war 包:


source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk use maven 3.9.6 && sdk use java 17.0.13-tem

mvn clean


mvn package -Drevision=2.541.2 -Dchangelist="-wormpex" -DskipTests -DskipITs -Dmaven.test.skip=true -Denforcer.skip=true -Dcheckstyle.skip=true -Dspotbugs.skip=true

