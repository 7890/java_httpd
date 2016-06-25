
1) Create self-signed certificate with keytool (part of Java JDK) in current directory:

```
keytool -keystore keystore -alias jetty -genkey -keyalg RSA -sigalg SHA256withRSA
```

2) Set keystore URI and password in WebServer.properties

3) When accessing services via https:// in browser, add security exception to allow certificate not signed by official CA
