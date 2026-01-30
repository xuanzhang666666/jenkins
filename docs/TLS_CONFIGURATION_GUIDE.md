# Jenkins 2.541 + Tomcat 10.0 TLS 1.2+ 配置指南

本文档详细说明了在 Jenkins 2.541 部署在 Tomcat 10.0 环境下，启用内网 TLS 1.2 或以上版本的完整配置方案。

## 背景

当 Jenkins 通过 REST API 提供服务，且安全组要求内网必须使用 TLS 互访（HTTPS: TLS 1.2 或以上）时，需要对系统进行全面的配置调整。

## 目录

- [1. Tomcat HTTPS 配置](#1-tomcat-https-配置)
- [2. SSL/TLS 证书准备](#2-ssltls-证书准备)
- [3. Jenkins 配置调整](#3-jenkins-配置调整)
- [4. Java/JVM 参数配置](#4-javajvm-参数配置)
- [5. REST API 客户端调整](#5-rest-api-客户端调整)
- [6. 防火墙和网络配置](#6-防火墙和网络配置)
- [7. HTTP 到 HTTPS 重定向](#7-http-到-https-重定向)
- [8. 验证和测试](#8-验证和测试)
- [9. 监控和日志](#9-监控和日志)
- [10. Jenkins 配置文件调整](#10-jenkins-配置文件调整)
- [11. CLI 客户端的 TLS 配置](#11-cli-客户端的-tls-配置)
- [12. REST API 客户端详细配置](#12-rest-api-客户端详细配置)
- [13. Jenkins 插件配置调整](#13-jenkins-插件配置调整)
- [14. JENKINS_HOME 配置文件检查](#14-jenkins_home-配置文件检查)
- [15. Reverse Proxy 场景](#15-reverse-proxy-场景可选)
- [16. 性能优化和安全加固](#16-性能优化和安全加固)
- [17. 监控和日志配置](#17-监控和日志配置)
- [18. 迁移检查清单](#18-迁移检查清单)

---

## 1. Tomcat HTTPS 配置

这是最重要的配置步骤。需要修改 Tomcat 的 `server.xml` 文件。

### 配置文件位置
```
$TOMCAT_HOME/conf/server.xml
```

### 配置示例

```xml
<Connector port="8443" protocol="org.apache.coyote.http11.Http11NioProtocol"
           maxThreads="150" SSLEnabled="true">
    <SSLHostConfig protocols="TLSv1.2,TLSv1.3">
        <Certificate certificateKeystoreFile="conf/keystore.jks"
                     certificateKeystorePassword="your-password"
                     type="RSA" />
    </SSLHostConfig>
</Connector>
```

### 关键配置项说明

| 配置项 | 说明 | 推荐值 |
|--------|------|--------|
| `port` | HTTPS 端口 | 8443 |
| `protocol` | 协议处理器 | `org.apache.coyote.http11.Http11NioProtocol` |
| `protocols` | 支持的 TLS 版本 | `TLSv1.2,TLSv1.3` |
| `certificateKeystoreFile` | 证书库路径 | 绝对路径或相对于 CATALINA_BASE 的路径 |
| `certificateKeystorePassword` | 证书库密码 | 强密码 |

### 注意事项

- **明确禁用 TLS 1.0 和 1.1**：只在 protocols 中指定 `TLSv1.2` 和 `TLSv1.3`
- **使用 NIO 协议**：性能更好，推荐使用 `Http11NioProtocol`

---

## 2. SSL/TLS 证书准备

### 生成自签名证书（开发/测试环境）

```bash
# 生成密钥库
keytool -genkey -alias jenkins -keyalg RSA -keysize 2048 \
        -keystore /path/to/keystore.jks -validity 365 \
        -dname "CN=jenkins-host, OU=IT, O=Company, L=City, S=State, C=CN"

# 查看证书
keytool -list -v -keystore /path/to/keystore.jks
```

### 使用企业 CA 签发的证书（生产环境推荐）

```bash
# 1. 生成证书签名请求 (CSR)
keytool -certreq -alias jenkins -keystore /path/to/keystore.jks \
        -file jenkins.csr

# 2. 将 CSR 提交给企业 CA 签发

# 3. 导入 CA 证书链
keytool -import -alias root -file root-ca.crt \
        -keystore /path/to/keystore.jks

keytool -import -alias intermediate -file intermediate-ca.crt \
        -keystore /path/to/keystore.jks

# 4. 导入签发的服务器证书
keytool -import -alias jenkins -file jenkins.crt \
        -keystore /path/to/keystore.jks
```

### 证书要求

- **密钥长度**：至少 2048 位（推荐 2048 或 4096）
- **有效期**：建议不超过 2 年
- **域名/IP**：证书中的 CN 或 SAN 必须与实际访问地址匹配
- **证书格式**：JKS 或 PKCS12 格式

---

## 3. Jenkins 配置调整

### 3.1 Jenkins 全局安全配置

在 Jenkins Web UI 中进行以下配置：

1. **更新 Jenkins URL**
   - 路径：`系统管理 → 系统配置 → Jenkins Location`
   - 将 `Jenkins URL` 改为：`https://your-domain:8443/jenkins`

2. **启用 CSRF 保护**（REST API 调用必须）
   - 路径：`系统管理 → 全局安全配置 → CSRF Protection`
   - 勾选 "启用代理兼容" 选项

3. **API Token 配置**
   - 路径：`用户 → 配置 → API Token`
   - 为每个需要调用 REST API 的用户生成 API Token

### 3.2 系统属性配置

如需在运行时设置 TLS 协议版本，在 Jenkins Script Console 中执行：

```groovy
System.setProperty("https.protocols", "TLSv1.2,TLSv1.3")
System.setProperty("jdk.tls.client.protocols", "TLSv1.2,TLSv1.3")
```

---

## 4. Java/JVM 参数配置

在 Tomcat 启动脚本中配置 JVM 参数。

### 配置文件位置

创建或编辑 `$TOMCAT_HOME/bin/setenv.sh`（Linux/Mac）或 `setenv.bat`（Windows）

### Linux/Mac 配置示例

```bash
#!/bin/bash

# 启用 TLS 1.2/1.3
export JAVA_OPTS="$JAVA_OPTS -Dhttps.protocols=TLSv1.2,TLSv1.3"
export JAVA_OPTS="$JAVA_OPTS -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3"

# 如果使用自签名证书，配置信任库
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStore=/path/to/truststore.jks"
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.ssl.trustStorePassword=password"

# 内存配置
export JAVA_OPTS="$JAVA_OPTS -Xms2048m -Xmx4096m"

# Jenkins Home 目录
export JENKINS_HOME="/var/lib/jenkins"
```

### Windows 配置示例

```batch
set JAVA_OPTS=%JAVA_OPTS% -Dhttps.protocols=TLSv1.2,TLSv1.3
set JAVA_OPTS=%JAVA_OPTS% -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3
set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.trustStore=C:\path\to\truststore.jks
set JAVA_OPTS=%JAVA_OPTS% -Djavax.net.ssl.trustStorePassword=password
```

---

## 5. REST API 客户端调整

调用方应用需要进行相应的调整。

### 5.1 更新访问 URL

```
旧 URL: http://jenkins-host:8080/jenkins/job/xxx
新 URL: https://jenkins-host:8443/jenkins/job/xxx
```

### 5.2 Java 客户端配置

#### 使用信任库（推荐）

```java
import javax.net.ssl.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import java.io.File;

public class JenkinsClient {
    public static CloseableHttpClient createHttpClient() throws Exception {
        SSLContext sslContext = SSLContextBuilder.create()
            .loadTrustMaterial(
                new File("/path/to/truststore.jks"), 
                "password".toCharArray()
            )
            .build();

        return HttpClients.custom()
            .setSSLContext(sslContext)
            .setSSLHostnameVerifier(new DefaultHostnameVerifier())
            .build();
    }
}
```

#### 信任所有证书（仅用于测试，生产环境禁用）

```java
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class UnsafeJenkinsClient {
    public static void trustAllCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { 
                    return null; 
                }
                public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {}
            }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        
        // 警告：仅用于开发测试！
    }
}
```

### 5.3 Python 客户端配置

```python
import requests
from requests.auth import HTTPBasicAuth

# 使用 CA 证书
def call_jenkins_api(url, username, token, ca_cert_path):
    response = requests.get(
        url,
        auth=HTTPBasicAuth(username, token),
        verify=ca_cert_path  # 或 verify=False 仅用于测试
    )
    return response.json()

# 获取 Crumb（CSRF Token）
def get_crumb(jenkins_url, username, token, ca_cert_path):
    crumb_url = f"{jenkins_url}/crumbIssuer/api/json"
    response = requests.get(
        crumb_url,
        auth=HTTPBasicAuth(username, token),
        verify=ca_cert_path
    )
    crumb_data = response.json()
    return {
        crumb_data['crumbRequestField']: crumb_data['crumb']
    }

# 执行构建
def trigger_build(jenkins_url, job_name, username, token, ca_cert_path):
    headers = get_crumb(jenkins_url, username, token, ca_cert_path)
    build_url = f"{jenkins_url}/job/{job_name}/build"
    
    response = requests.post(
        build_url,
        auth=HTTPBasicAuth(username, token),
        headers=headers,
        verify=ca_cert_path
    )
    return response.status_code

# 使用示例
jenkins_url = "https://jenkins-host:8443/jenkins"
username = "admin"
token = "your-api-token"
ca_cert = "/path/to/ca-bundle.crt"

# 获取 Jenkins 信息
info = call_jenkins_api(f"{jenkins_url}/api/json", username, token, ca_cert)
print(info)

# 触发构建
status = trigger_build(jenkins_url, "my-job", username, token, ca_cert)
print(f"Build trigger status: {status}")
```

### 5.4 curl 命令示例

```bash
# 设置变量
JENKINS_URL="https://jenkins-host:8443/jenkins"
USERNAME="admin"
TOKEN="your-api-token"
CA_CERT="/path/to/ca.crt"

# 获取 API 信息
curl -u $USERNAME:$TOKEN \
     --cacert $CA_CERT \
     $JENKINS_URL/api/json

# 获取 Crumb
CRUMB=$(curl -u $USERNAME:$TOKEN \
        --cacert $CA_CERT \
        "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)")

# 创建 Job
curl -X POST -u $USERNAME:$TOKEN \
     --cacert $CA_CERT \
     -H "$CRUMB" \
     -H "Content-Type: text/xml" \
     --data-binary @job-config.xml \
     "$JENKINS_URL/createItem?name=test-job"

# 触发构建
curl -X POST -u $USERNAME:$TOKEN \
     --cacert $CA_CERT \
     -H "$CRUMB" \
     "$JENKINS_URL/job/test-job/build"

# 带参数触发构建
curl -X POST -u $USERNAME:$TOKEN \
     --cacert $CA_CERT \
     -H "$CRUMB" \
     "$JENKINS_URL/job/test-job/buildWithParameters?PARAM1=value1&PARAM2=value2"
```

---

## 6. 防火墙和网络配置

### 6.1 端口配置

- **开放 HTTPS 端口**：允许内网访问 8443 端口
- **关闭 HTTP 端口**：禁止或重定向 8080 端口
- **防火墙规则**：配置仅允许内网 IP 段访问

### 6.2 Linux 防火墙配置示例

```bash
# firewalld
sudo firewall-cmd --permanent --add-port=8443/tcp
sudo firewall-cmd --permanent --remove-port=8080/tcp
sudo firewall-cmd --reload

# iptables
sudo iptables -A INPUT -p tcp --dport 8443 -s 192.168.0.0/16 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8080 -j DROP
sudo iptables-save > /etc/iptables/rules.v4
```

---

## 7. HTTP 到 HTTPS 重定向

### 7.1 Tomcat web.xml 配置

在 `$TOMCAT_HOME/webapps/jenkins/WEB-INF/web.xml` 末尾添加：

```xml
<security-constraint>
    <web-resource-collection>
        <web-resource-name>Protected Context</web-resource-name>
        <url-pattern>/*</url-pattern>
    </web-resource-collection>
    <user-data-constraint>
        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
    </user-data-constraint>
</security-constraint>
```

### 7.2 保留 HTTP Connector 配置

在 `server.xml` 中保留 HTTP Connector，但配置重定向：

```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443" />
```

---

## 8. 验证和测试

### 8.1 基础连接测试

```bash
# 测试 TLS 1.2 连接
openssl s_client -connect jenkins-host:8443 -tls1_2

# 测试 TLS 1.3 连接
openssl s_client -connect jenkins-host:8443 -tls1_3

# 验证证书有效性
openssl s_client -connect jenkins-host:8443 -showcerts

# 检查支持的加密套件
nmap --script ssl-enum-ciphers -p 8443 jenkins-host
```

### 8.2 REST API 功能测试

```bash
# 1. 测试基本连接
curl -v -u username:token https://jenkins-host:8443/jenkins/api/json

# 2. 测试 Job 列表
curl -u username:token https://jenkins-host:8443/jenkins/api/json?tree=jobs[name]

# 3. 测试创建 Job
curl -X POST -u username:token \
     -H "$CRUMB" \
     -H "Content-Type: text/xml" \
     --data-binary @job-config.xml \
     "https://jenkins-host:8443/jenkins/createItem?name=test-job"

# 4. 测试构建触发
curl -X POST -u username:token \
     -H "$CRUMB" \
     "https://jenkins-host:8443/jenkins/job/test-job/build"

# 5. 测试构建状态查询
curl -u username:token \
     "https://jenkins-host:8443/jenkins/job/test-job/lastBuild/api/json"
```

### 8.3 验证禁用旧版本 TLS

```bash
# 这些连接应该失败
openssl s_client -connect jenkins-host:8443 -tls1
openssl s_client -connect jenkins-host:8443 -tls1_1
```

---

## 9. 监控和日志

### 9.1 Tomcat 日志配置

编辑 `$TOMCAT_HOME/conf/logging.properties`：

```properties
# SSL/TLS 详细日志
org.apache.coyote.http11.level = FINE
org.apache.tomcat.util.net.level = FINE
```

### 9.2 启用 SSL Debug

在 `setenv.sh` 中添加：

```bash
# 详细的 SSL/TLS 调试信息
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.debug=ssl:handshake:verbose"

# 或者仅握手信息
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.debug=ssl:handshake"
```

---

## 10. Jenkins 配置文件调整

### 10.1 修改 JenkinsLocationConfiguration.xml

位置：`$JENKINS_HOME/jenkins.model.JenkinsLocationConfiguration.xml`

```xml
<?xml version='1.1' encoding='UTF-8'?>
<jenkins.model.JenkinsLocationConfiguration>
  <adminAddress>admin@your-company.com</adminAddress>
  <jenkinsUrl>https://jenkins-host:8443/jenkins/</jenkinsUrl>
</jenkins.model.JenkinsLocationConfiguration>
```

**重要**：`jenkinsUrl` 必须以 `https://` 开头

### 10.2 检查主配置文件

位置：`$JENKINS_HOME/config.xml`

确保没有硬编码的 HTTP URLs，特别是：
- SCM 仓库地址
- Webhook URLs
- 外部系统集成地址

---

## 11. CLI 客户端的 TLS 配置

Jenkins CLI 支持通过 HTTPS 连接。

### 11.1 下载 CLI

```bash
wget https://jenkins-host:8443/jenkins/jnlpJars/jenkins-cli.jar
```

### 11.2 使用 CLI（带证书验证）

```bash
# 基本用法
java -jar jenkins-cli.jar -s https://jenkins-host:8443/jenkins/ \
     -auth username:token \
     list-jobs

# 执行构建
java -jar jenkins-cli.jar -s https://jenkins-host:8443/jenkins/ \
     -auth username:token \
     build test-job
```

### 11.3 配置自签名证书信任

如果使用自签名证书，需要导入到 Java truststore：

```bash
# 1. 导出服务器证书
openssl s_client -connect jenkins-host:8443 < /dev/null \
    | openssl x509 -out jenkins.crt

# 2. 导入到 Java truststore
keytool -import -alias jenkins -file jenkins.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit

# 3. 或者使用自定义 truststore
java -Djavax.net.ssl.trustStore=/path/to/truststore.jks \
     -Djavax.net.ssl.trustStorePassword=password \
     -jar jenkins-cli.jar -s https://jenkins-host:8443/jenkins/ \
     -auth username:token list-jobs
```

### 11.4 跳过证书验证（仅测试）

```bash
# 不推荐在生产环境使用
java -jar jenkins-cli.jar -s https://jenkins-host:8443/jenkins/ \
     -noCertificateCheck \
     -auth username:token list-jobs
```

---

## 12. REST API 客户端详细配置

### 12.1 认证方式

Jenkins REST API 支持多种认证方式：

1. **API Token（推荐）**
   ```
   Authorization: Basic base64(username:api-token)
   ```

2. **用户名密码（不推荐）**
   ```
   Authorization: Basic base64(username:password)
   ```

3. **Bearer Token**
   ```
   Authorization: Bearer <token>
   ```

### 12.2 CSRF Protection 处理

如果启用了 CSRF Protection，必须先获取 Crumb：

```bash
# 获取 Crumb
curl -u username:token \
     "https://jenkins-host:8443/jenkins/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)"

# 输出示例：Jenkins-Crumb:1234567890abcdef
```

在后续请求中添加该 Header。

### 12.3 常用 API 端点

| API 端点 | 说明 | 方法 |
|----------|------|------|
| `/api/json` | Jenkins 基本信息 | GET |
| `/crumbIssuer/api/json` | 获取 CSRF Token | GET |
| `/createItem?name=xxx` | 创建 Job | POST |
| `/job/{name}/config.xml` | 获取 Job 配置 | GET |
| `/job/{name}/build` | 触发构建 | POST |
| `/job/{name}/buildWithParameters` | 带参数构建 | POST |
| `/job/{name}/{number}/api/json` | 获取构建信息 | GET |
| `/job/{name}/{number}/console` | 获取构建日志 | GET |
| `/queue/api/json` | 获取构建队列 | GET |

---

## 13. Jenkins 插件配置调整

某些插件在启用 HTTPS 后需要特别配置。

### 13.1 Git 插件

如果 Git 仓库使用 HTTPS：

```groovy
// 在 Jenkins Script Console 中执行
System.setProperty("https.protocols", "TLSv1.2,TLSv1.3")
```

### 13.2 Mailer 插件

配置 SMTP over TLS：

1. 进入：`系统管理 → 系统配置 → 邮件通知`
2. 配置：
   - SMTP 服务器：smtp.company.com
   - 勾选 "Use SSL" 或 "Use TLS"
   - SMTP 端口：587 (TLS) 或 465 (SSL)

### 13.3 受影响的常见插件

以下插件在启用 HTTPS 后可能需要检查配置：

- **LDAP Plugin**：LDAPS 连接
- **JIRA Plugin**：JIRA API 地址
- **Slack Plugin**：Webhook URLs
- **GitLab Plugin**：GitLab API 地址
- **Kubernetes Plugin**：Kubernetes API Server
- **Docker Plugin**：Docker Registry

---

## 14. JENKINS_HOME 配置文件检查

### 14.1 关键配置文件

```
$JENKINS_HOME/
├── jenkins.model.JenkinsLocationConfiguration.xml  # Jenkins URL
├── credentials.xml                                  # 凭据
├── config.xml                                       # 主配置
├── secrets/                                         # 密钥目录
├── identity.key.enc                                 # 实例密钥
├── jobs/
│   └── [job-name]/
│       └── config.xml                               # Job 配置
└── plugins/                                         # 插件目录
```

### 14.2 需要更新的内容

使用 grep 查找所有 HTTP URLs：

```bash
cd $JENKINS_HOME
grep -r "http://jenkins" . --include="*.xml"
```

将所有匹配的 HTTP URL 改为 HTTPS。

---

## 15. Reverse Proxy 场景（可选）

如果在 Tomcat 前面使用 Nginx/Apache 作为反向代理。

### 15.1 Nginx 配置

```nginx
upstream jenkins {
    server 127.0.0.1:8080 fail_timeout=0;
}

server {
    listen 443 ssl http2;
    server_name jenkins.your-domain.com;

    # SSL 证书
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    # TLS 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 代理配置
    location /jenkins {
        proxy_pass http://jenkins;
        
        # 重要：告诉 Jenkins 它在 HTTPS 后面
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Port 443;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # WebSocket 支持（CLI 需要）
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # 超时设置
        proxy_connect_timeout 90;
        proxy_send_timeout 90;
        proxy_read_timeout 90;
        
        # 缓冲区设置
        proxy_buffering off;
        proxy_request_buffering off;
    }
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name jenkins.your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

### 15.2 Tomcat Connector 配置（Reverse Proxy 场景）

```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"
           scheme="https"
           proxyPort="443"
           proxyName="jenkins.your-domain.com" />
```

### 15.3 Apache 配置

```apache
<VirtualHost *:443>
    ServerName jenkins.your-domain.com
    
    SSLEngine on
    SSLCertificateFile /path/to/cert.pem
    SSLCertificateKeyFile /path/to/key.pem
    SSLProtocol TLSv1.2 TLSv1.3
    
    ProxyPreserveHost On
    ProxyRequests Off
    
    RequestHeader set X-Forwarded-Proto "https"
    RequestHeader set X-Forwarded-Port "443"
    
    ProxyPass /jenkins http://localhost:8080/jenkins nocanon
    ProxyPassReverse /jenkins http://localhost:8080/jenkins
    
    <Location /jenkins>
        ProxyPassReverse /
        Require all granted
    </Location>
</VirtualHost>

<VirtualHost *:80>
    ServerName jenkins.your-domain.com
    Redirect permanent / https://jenkins.your-domain.com/
</VirtualHost>
```

---

## 16. 性能优化和安全加固

### 16.1 加密套件优化

在 Tomcat `server.xml` 中配置推荐的加密套件：

```xml
<SSLHostConfig protocols="TLSv1.2,TLSv1.3"
               ciphers="TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                        TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                        TLS_AES_128_GCM_SHA256,
                        TLS_AES_256_GCM_SHA384,
                        TLS_CHACHA20_POLY1305_SHA256"
               honorCipherOrder="true">
    <Certificate certificateKeystoreFile="conf/keystore.jks"
                 certificateKeystorePassword="password"
                 type="RSA" />
</SSLHostConfig>
```

### 16.2 启用 HSTS

在 `$TOMCAT_HOME/conf/web.xml` 中添加：

```xml
<filter>
    <filter-name>httpHeaderSecurity</filter-name>
    <filter-class>org.apache.catalina.filters.HttpHeaderSecurityFilter</filter-class>
    <init-param>
        <param-name>hstsEnabled</param-name>
        <param-value>true</param-value>
    </init-param>
    <init-param>
        <param-name>hstsMaxAgeSeconds</param-name>
        <param-value>31536000</param-value>
    </init-param>
    <init-param>
        <param-name>hstsIncludeSubDomains</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>

<filter-mapping>
    <filter-name>httpHeaderSecurity</filter-name>
    <url-pattern>/*</url-pattern>
    <dispatcher>REQUEST</dispatcher>
</filter-mapping>
```

### 16.3 禁用不安全的 HTTP 方法

```xml
<security-constraint>
    <web-resource-collection>
        <web-resource-name>Restricted methods</web-resource-name>
        <url-pattern>/*</url-pattern>
        <http-method>TRACE</http-method>
        <http-method>OPTIONS</http-method>
    </web-resource-collection>
    <auth-constraint />
</security-constraint>
```

### 16.4 会话 Cookie 安全

在 `$TOMCAT_HOME/conf/context.xml` 中：

```xml
<Context>
    <CookieProcessor sameSiteCookies="strict" />
</Context>
```

---

## 17. 监控和日志配置

### 17.1 Jenkins 系统日志

在 Jenkins Web UI 中配置：

1. 进入：`系统管理 → 系统日志 → 添加新记录器`
2. 配置以下 Loggers：

| Logger 名称 | 级别 | 说明 |
|------------|------|------|
| `hudson.remoting` | FINE | 远程调用 |
| `org.apache.http` | FINE | HTTP 请求 |
| `jenkins.security` | FINE | 安全相关 |
| `hudson.security` | FINE | 认证授权 |

### 17.2 SSL/TLS 调试日志

临时启用详细的 SSL 调试（性能影响大，仅用于排错）：

```bash
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.debug=all"
```

或者仅记录握手信息：

```bash
export JAVA_OPTS="$JAVA_OPTS -Djavax.net.debug=ssl:handshake"
```

### 17.3 Tomcat 访问日志

在 `server.xml` 中配置访问日志：

```xml
<Valve className="org.apache.catalina.valves.AccessLogValve" 
       directory="logs"
       prefix="jenkins_access_log" 
       suffix=".txt"
       pattern="%h %l %u %t &quot;%r&quot; %s %b %D %{User-Agent}i" />
```

### 17.4 日志轮转

配置 `logrotate`（Linux）：

```bash
# /etc/logrotate.d/jenkins
/var/log/jenkins/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    sharedscripts
    postrotate
        systemctl reload jenkins
    endscript
}
```

---

## 18. 迁移检查清单

在正式切换到 HTTPS 前，按照以下清单逐项验证。

### 18.1 配置验证

- [ ] Tomcat `server.xml` 配置了 HTTPS Connector
- [ ] SSL 证书已正确安装到 keystore
- [ ] JVM 参数配置了 TLS 1.2+
- [ ] Jenkins URL 已更新为 HTTPS
- [ ] `$JENKINS_HOME` 配置文件中的 URL 已更新

### 18.2 网络验证

```bash
# 验证端口监听
netstat -tlnp | grep 8443

# 验证 TLS 版本
openssl s_client -connect localhost:8443 -tls1_2
openssl s_client -connect localhost:8443 -tls1_3

# 验证旧版本被禁用
openssl s_client -connect localhost:8443 -tls1    # 应该失败
openssl s_client -connect localhost:8443 -tls1_1  # 应该失败
```

### 18.3 证书验证

```bash
# 查看证书详细信息
openssl s_client -connect localhost:8443 -showcerts

# 验证证书链
openssl s_client -connect localhost:8443 -CAfile /path/to/ca.crt

# 检查证书有效期
echo | openssl s_client -connect localhost:8443 2>/dev/null | \
    openssl x509 -noout -dates
```

### 18.4 加密套件验证

```bash
# 使用 nmap 扫描
nmap --script ssl-enum-ciphers -p 8443 localhost

# 使用 testssl.sh（推荐）
./testssl.sh https://localhost:8443
```

### 18.5 API 功能验证

```bash
# 1. 基本连接
curl -v -u username:token https://localhost:8443/jenkins/api/json

# 2. 获取 Crumb
CRUMB=$(curl -u username:token \
        https://localhost:8443/jenkins/crumbIssuer/api/xml?xpath=concat\(//crumbRequestField,\":\",//crumb\))

# 3. 创建测试 Job
cat > test-job.xml << 'EOF'
<?xml version='1.1' encoding='UTF-8'?>
<project>
  <description>Test Job</description>
  <keepDependencies>false</keepDependencies>
  <properties/>
  <scm class="hudson.scm.NullSCM"/>
  <canRoam>true</canRoam>
  <disabled>false</disabled>
  <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
  <triggers/>
  <concurrentBuild>false</concurrentBuild>
  <builders>
    <hudson.tasks.Shell>
      <command>echo "Hello from HTTPS Jenkins"</command>
    </hudson.tasks.Shell>
  </builders>
  <publishers/>
  <buildWrappers/>
</project>
EOF

curl -X POST -u username:token \
     -H "$CRUMB" \
     -H "Content-Type: text/xml" \
     --data-binary @test-job.xml \
     "https://localhost:8443/jenkins/createItem?name=https-test"

# 4. 触发构建
curl -X POST -u username:token \
     -H "$CRUMB" \
     "https://localhost:8443/jenkins/job/https-test/build"

# 5. 查看构建状态
sleep 5
curl -u username:token \
     "https://localhost:8443/jenkins/job/https-test/lastBuild/api/json"

# 6. 删除测试 Job
curl -X POST -u username:token \
     -H "$CRUMB" \
     "https://localhost:8443/jenkins/job/https-test/doDelete"
```

### 18.6 CLI 验证

```bash
# 下载 CLI
wget https://localhost:8443/jenkins/jnlpJars/jenkins-cli.jar

# 测试 CLI 命令
java -jar jenkins-cli.jar -s https://localhost:8443/jenkins/ \
     -auth username:token \
     list-jobs

java -jar jenkins-cli.jar -s https://localhost:8443/jenkins/ \
     -auth username:token \
     who-am-i
```

### 18.7 插件验证

- [ ] Git 插件可以正常克隆仓库
- [ ] LDAP 认证正常工作
- [ ] 邮件通知正常发送
- [ ] Webhook 触发正常工作
- [ ] 外部系统集成正常

### 18.8 性能验证

```bash
# 使用 ab（Apache Bench）进行压力测试
ab -n 1000 -c 10 -A username:token \
   https://localhost:8443/jenkins/api/json

# 使用 JMeter 进行更复杂的性能测试
```

---

## 配置文件位置总结

| 配置项 | 文件路径 | 说明 |
|--------|----------|------|
| Tomcat HTTPS | `$TOMCAT_HOME/conf/server.xml` | SSL Connector 配置 |
| JVM 参数 | `$TOMCAT_HOME/bin/setenv.sh` | TLS 协议版本 |
| Tomcat 日志 | `$TOMCAT_HOME/conf/logging.properties` | 日志级别配置 |
| Web 安全 | `$TOMCAT_HOME/conf/web.xml` | HTTPS 重定向、HSTS |
| Context 配置 | `$TOMCAT_HOME/conf/context.xml` | Cookie 安全设置 |
| Jenkins URL | `$JENKINS_HOME/jenkins.model.JenkinsLocationConfiguration.xml` | 系统访问 URL |
| Jenkins 主配置 | `$JENKINS_HOME/config.xml` | 主配置文件 |
| 证书库 | `/path/to/keystore.jks` | SSL 证书存储 |
| 信任库 | `/path/to/truststore.jks` | 受信任证书 |

---

## 常见问题排错

### 问题 1：证书不被信任

**现象**：客户端报错 `sun.security.validator.ValidatorException: PKIX path building failed`

**解决**：
```bash
# 导入 CA 证书到客户端 truststore
keytool -import -alias company-ca -file ca.crt \
        -keystore $JAVA_HOME/lib/security/cacerts \
        -storepass changeit
```

### 问题 2：TLS 握手失败

**现象**：`Received fatal alert: handshake_failure`

**排查**：
```bash
# 检查支持的协议版本
openssl s_client -connect jenkins-host:8443 -tls1_2 -debug

# 检查加密套件
nmap --script ssl-enum-ciphers -p 8443 jenkins-host
```

**解决**：确保客户端和服务器端支持相同的 TLS 版本和加密套件

### 问题 3：CSRF Protection 导致 API 调用失败

**现象**：`403 No valid crumb was included in the request`

**解决**：
```bash
# 方式1：每次请求前获取 crumb
CRUMB=$(curl -u user:token "$JENKINS_URL/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,\":\",//crumb)")
curl -H "$CRUMB" ...

# 方式2：使用 API Token 并在请求中包含 crumb
```

### 问题 4：Webhook 回调失败

**现象**：外部系统（如 GitLab）无法调用 Jenkins webhook

**排查**：
- 检查外部系统是否信任 Jenkins 证书
- 检查防火墙规则
- 查看 Jenkins 日志

**解决**：
- 在外部系统中配置 CA 证书
- 或使用公认的 CA 签发的证书

### 问题 5：性能下降

**现象**：启用 HTTPS 后响应变慢

**优化**：
- 使用 HTTP/2：在 Tomcat Connector 中添加 `upgrade="h2"`
- 启用 Session Ticket
- 使用硬件加速（如 AES-NI）
- 调整 `maxThreads` 和连接池大小

---

## 安全建议

1. **定期更新证书**：建议证书有效期不超过 2 年，并在到期前 1 个月更新
2. **使用强密码**：keystore 和 truststore 密码应足够复杂
3. **限制访问**：仅允许必要的 IP 段访问 Jenkins
4. **启用审计日志**：记录所有 API 调用和管理操作
5. **定期安全扫描**：使用 SSL Labs、testssl.sh 等工具定期扫描
6. **最小权限原则**：为 API 用户分配最小必要权限
7. **备份配置**：定期备份 `$JENKINS_HOME` 和证书文件

---

## 参考资源

- [Tomcat 10 SSL/TLS Configuration](https://tomcat.apache.org/tomcat-10.0-doc/ssl-howto.html)
- [Jenkins Security Recommendations](https://www.jenkins.io/doc/book/security/)
- [OWASP TLS Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)

---

## 文档版本

- **版本**: 1.0
- **创建日期**: 2026-01-30
- **适用版本**: Jenkins 2.541 + Tomcat 10.0
- **作者**: Jenkins 运维团队

---

**注意**: 本文档提供的所有示例配置仅供参考，请根据实际环境进行调整。在生产环境实施前，建议先在测试环境进行完整验证。
