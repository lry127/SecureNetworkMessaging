# Creating your own CA and Certs to secure network traffic

`SecureNetworkMessaging` enforces [TLS mutual authentication](https://en.wikipedia.org/wiki/Mutual_authentication) to make sure only authorized users can access your services and access them in a secure way.

To do so, it's necessary to create certificates signed by a trusted CA for both server and client.

## 1. Create your own CA

1. create ca dir

```bash
mkdir ca && cd ca
```

2. generate ca private key

```bash
openssl genrsa -out ca.key 2048 
```

3. create a ca cert

```bash
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt \
  -subj "/C=US/CN=SecureNetworkMessaging Self Trusted"
```

4. go back

```bash
cd ..
```

One liner:

```
mkdir ca && cd ca
openssl genrsa -out ca.key 2048 
openssl req -x509 -new -nodes -key ca.key -sha256 -days 3650 -out ca.crt \
  -subj "/C=US/CN=SecureNetworkMessaging Self Trusted"
cd ..
```



## 2. Create Server Cert And Sign it

1. create server cert dir

```bash
mkdir server && cd server
```

2. generate server private key

```bash
openssl genrsa -out server.key 2048
```

3. create a csr ([Certificate Signing Request](https://en.wikipedia.org/wiki/Certificate_signing_request)) file

```bash
openssl req -new -key server.key -out server.csr \
  -subj "/C=US/CN=SecureNetworkMessaging Server #1"
```

4. create a signed certificate using your ca private key

```
openssl x509 -req -in server.csr -CA ../ca/ca.crt -CAkey ../ca/ca.key -CAcreateserial \
  -out server.crt -days 365 -sha256
```

5. store key and cert in pkcs12 format

here `password` is used as keystore password, you can change it at the end of the command

```
openssl pkcs12 -export -out server.p12 -inkey server.key -in server.crt -certfile ../ca/ca.crt -password pass:password
```

6. go back

```
cd ..
```

One liner

```
mkdir server && cd server
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr \
  -subj "/C=US/CN=SecureNetworkMessaging Server #1"
openssl x509 -req -in server.csr -CA ../ca/ca.crt -CAkey ../ca/ca.key -CAcreateserial \
  -out server.crt -days 365 -sha256
openssl pkcs12 -export -out server.p12 -inkey server.key -in server.crt -certfile ../ca/ca.crt -password pass:password
cd ..

```



## 3. Create Client Cert And Sign it

Nearly identical to creating a server cert, all commands are listed here without explaining

```
mkdir client && cd client
openssl genrsa -out client.key 2048
openssl req -new -key client.key -out client.csr \
  -subj "/C=US/CN=SecureNetworkMessaging Client #1"
openssl x509 -req -in client.csr -CA ../ca/ca.crt -CAkey ../ca/ca.key -CAcreateserial \
  -out client.crt -days 365 -sha256
openssl pkcs12 -export -out client.p12 -inkey client.key -in client.crt -certfile ../ca/ca.crt -password pass:password
cd ..
```

> [!TIP]
> You might want to generate unique certificate for each user, instead of hardcoding one client cert to your app for all clients.

## 4. Putting certs to correct resources

Copy your `server/server.p12`  and `client/client.p12` to `src/main/resources/` and that's it.
