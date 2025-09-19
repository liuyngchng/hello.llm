#1. env
## 1.1 java 17
ubuntu
```$sh
sudo apt-get install openjdk-17-jdk
```
windows, see https://www.oracle.com/java/technologies/downloads/?er=221886#java17-windows 

for file jdk-17.0.16_windows-x64_bin.exe
```sh

```
## 1.2 maven
ubuntu
```$sh
sudo apt-get install maven
```
windows, see https://maven.apache.org/download.cgi 

for file apache-maven-3.9.11-bin.zip

# 2. compile and run
(1) change the LLM api-key "add_your_api_key" into your own's in file src/main/java/rd/test/Txt2Sql.java

(2) compile your source code with cmd: mvn clean compile

(3) package your compiled code with cmd: mvn package

(4) run your code with cmd: java -jar target/hello.llm-1.0-SNAPSHOT-jar-with-dependencies.jar ,you can see information as following:

