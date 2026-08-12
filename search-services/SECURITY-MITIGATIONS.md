# Jetty 9.4.57 CVE Mitigations

## Scope

Search Services embeds the `solr-solr-6.6.5-patched.22.zip` runtime. That ZIP
contains Jetty `9.4.57.v20241219` JARs under `server/lib`; Maven dependency
management does not replace them in the packaged distribution.

This assessment applies to the default runtime assembled by
`search-services/packaging/pom.xml`. Reassess it when the Solr distribution,
Jetty configuration, ingress, or authentication mode changes.

## Reachability Assessment

| CVE | Disposition | Evidence |
| --- | --- | --- |
| CVE-2026-6790 | Not reachable | Requires HTTP/2 or HTTP/3. The distribution contains no HTTP/2, HTTP/3, or ALPN JARs. `server/etc/jetty-http.xml` configures only Jetty's HTTP/1.1 `HttpConnectionFactory`. |
| CVE-2024-6763 | Not applicable to current use | Requires application use of `org.eclipse.jetty.http.HttpURI` as a validator for attacker-controlled URLs. Repository application code does not reference that class, and the Jetty advisory states that normal server use is not vulnerable. |
| CVE-2025-11143 | Not exploitable through identified application code | Repository application code does not use `HttpURI` for URL allowlists/blocklists, redirects, host ACLs, or SSRF protection. Reject malformed URIs at the ingress as defense in depth. |
| CVE-2026-10050 | Not reachable | The vulnerable class is client-side `org.eclipse.jetty.client.DigestAuthentication`. The distribution contains no `jetty-client` JAR. The server-side `DigestAuthenticator` is a different class and is not configured. |
| CVE-2026-2332 | Reachable unless controlled at deployment | The vulnerable HTTP/1.1 chunk parser is active in `HttpConnectionFactory` and processes requests before Solr authentication. Application-level shared-secret checks do not prevent parser exploitation. |

## Required Controls for CVE-2026-2332

Production deployments must apply all applicable controls below:

1. Do not expose the Solr listener directly to public or general internal networks.
2. Restrict port `8983` to approved ACS, Search, monitoring, and administrative sources using network policy, firewall rules, or security groups.
3. Prefer mutual TLS and set `SOLR_SSL_NEED_CLIENT_AUTH=true`.
4. At any ingress proxy, reject malformed chunked transfer encoding, chunk extensions, and control characters in chunk metadata. Prefer normalized upstream requests using `Content-Length`.
5. Do not rely on `ALFRESCO_SECURE_COMMS=secret` alone. Jetty parses HTTP framing before the shared-secret authentication plugin runs.
6. Do not use `ALFRESCO_SECURE_COMMS=none` in production.

## Deployment Evidence

Capture the following evidence for each deployed environment before accepting
the mitigation:

- The effective `ALFRESCO_SECURE_COMMS` value.
- The effective `SOLR_SSL_NEED_CLIENT_AUTH` value.
- Listener bindings and published container ports.
- Firewall, security-group, or network-policy rules protecting port `8983`.
- Ingress or load-balancer rules for chunked transfer encoding and connection reuse.
- A request-path diagram showing every component between an untrusted client and Jetty.

Reopen the finding if untrusted clients can establish a direct HTTP/1.1
connection to Jetty, or if the ingress cannot reject the malformed chunked
requests described by CVE-2026-2332.