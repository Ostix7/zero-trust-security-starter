# Demo Scenario

This scenario is designed for the diploma defense and demonstrates why the Zero Trust starter is useful as a practical platform, not just as a set of Spring Security settings.

## 1. Start the platform

```powershell
.\scripts\start-demo-compose.ps1
```

Services:

- gateway service on `http://localhost:8080`
- resource service on `http://localhost:8081`
- policy service on `http://localhost:8082`
- Prometheus on `http://localhost:9090`
- Grafana on `http://localhost:3000`

## 2. Generate demo JWT tokens

Admin token:

```powershell
.\scripts\generate-demo-jwt.ps1 -Subject security-admin -Roles ADMIN
```

Finance user:

```powershell
.\scripts\generate-demo-jwt.ps1 -Subject fin-user -Roles USER -Department finance -TenantId acme
```

Sales user:

```powershell
.\scripts\generate-demo-jwt.ps1 -Subject sales-user -Roles USER -Department sales -TenantId acme
```

Tenant user:

```powershell
.\scripts\generate-demo-jwt.ps1 -Subject tenant-user -Roles USER -TenantId acme
```

## 3. Show the security baseline

- call `GET http://localhost:8081/public/hello` without a token
- call `GET http://localhost:8081/api/me` without a token and show `401`
- call `GET http://localhost:8081/admin/report` with a non-admin token and show `403`

## 4. Show gateway propagation

- call `GET http://localhost:8080/api/proxy/me` with a valid user token
- show that the response contains the propagated principal and correlation id
- explain that the gateway does not reimplement token forwarding manually, because this is provided by the starter

## 5. Show tenant ABAC and context propagation

- call `GET http://localhost:8080/api/proxy/tenant-orders` with header `X-Tenant-Id: acme` and a token with `tenant_id=acme`
- explain that the gateway forwards both the tenant header and correlation id to the resource service
- repeat with `X-Tenant-Id: other` and show `TENANT_POLICY_DENIED`

## 6. Show external policy enforcement

- call `GET http://localhost:8080/api/proxy/finance-report` with the finance token and show success
- call the same endpoint with the sales token and show `POLICY_DENIED`
- explain that this decision comes from an external OPA-like service, not from hardcoded controller logic

## 7. Show non-human identity and secrets integration

- call `GET http://localhost:8080/public/service-ping` without a user token
- explain that the gateway uses a service token resolved via a Vault-style secret path
- mention that this models non-human service identity for scheduled tasks and backend integrations

## 8. Show observability

- open `http://localhost:9090` and show that Prometheus scrapes `/actuator/prometheus`
- open `http://localhost:3000` and show the provisioned `Zero Trust Overview` dashboard
- point at the `zero_trust_events_total` metrics and request-rate graphs

## 9. Show auditability

- call `GET http://localhost:8080/api/proxy/audit-events` with the admin token
- explain that security events are both logged and available for inspection in demo mode

## 10. Use the automated script if time is limited

```powershell
.\scripts\run-demo-scenario.ps1
```

The script executes the main defense story automatically and prints the status codes and JSON payloads for each step.
