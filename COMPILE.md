# Helios
## Introduction

## Compile guide

Development environement:
- Ubuntu 16.04 LTS
- Oracle Java 1.8
- Maven 3.3.9
- Git

1. Install Ubuntu Packages

~~~
sudo apt-get install oracle-java8-installer maven git
~~~

2. Compile Application

This maven instruction will load all required libraries, build project 
and creates a zip file with all required parts.
~~~
mvn clean dependency:resolve compile package assembly:single
~~~


