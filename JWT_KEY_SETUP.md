# JWT Key Management Setup Guide

Complete guide for managing JWT RSA keys in Docker with PostgreSQL database storage.

## Table of Contents
1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Detailed Setup](#detailed-setup)
5. [How It Works](#how-it-works)
6. [Troubleshooting](#troubleshooting)
7. [Team Onboarding](#team-onboarding)
8. [Security Best Practices](#security-best-practices)

---

## Overview

This project uses **JWT (JSON Web Tokens)** for authentication with **RSA public/private key pairs**.

**Key Features**:
- 🔐 RSA 2048-bit keys for security
- 💾 Database storage (PostgreSQL)
- 🐳 Docker-integrated workflow
- 🚫 Never committed to GitHub
- 🔄 Automatic initialization
- 👥 Team-friendly setup

**Architecture**:
```
Local Filesystem → Docker Volume Mount → Init Script → PostgreSQL → Spring Boot App
```

---

## Prerequisites

- Docker and Docker Compose installed
- Java 21+ (for keytool command)
- PostgreSQL client (psql) - included in postgres Docker image
- Terminal access

---

## Quick Start

### Step 1: Generate JWT Keys (One-Time Setup)

```bash
# Generate RSA key pair in Java KeyStore format
keytool -genkeypair \
  -alias jwt-key \
  -keyalg RSA \
  -keysize 2048 \
  -keystore keys/jwt-keystore.jks \
  -keypass changeit \
  -storepass changeit \
  -dname "CN=DSI-007, OU=DSI, O=DSI Innovators, L=Dhaka, ST=Dhaka, C=BD" \
  -validity 3650
```

**What this does**:
- Creates `keys/jwt-keystore.jks` file
- Generates 2048-bit RSA key pair
- Valid for 10 years (3650 days)
- Uses organization info: DSI-007

### Step 2: Extract PEM Files

```bash
# Extract private key (with bag attributes)
keytool -importkeystore \
  -srckeystore keys/jwt-keystore.jks \
  -destkeystore keys/jwt-key.p12 \
  -deststoretype PKCS12 \
  -srcalias jwt-key \
  -deststorepass changeit \
  -destkeypass changeit \
  -srcstorepass changeit

# Convert to PEM format
openssl pkcs12 -in keys/jwt-key.p12 \
  -nocerts -nodes \
  -passin pass:changeit \
  -out keys/private_key.pem

# Extract public key
openssl pkcs12 -in keys/jwt-key.p12 \
  -clcerts -nokeys \
  -passin pass:changeit \
  | openssl x509 -pubkey -noout \
  > keys/public_key_only.pem

# Clean up intermediate files
rm keys/jwt-key.p12
```

### Step 3: Verify Key Files

```bash
ls -lh keys/
```

**Expected output**:
```
jwt-keystore.jks       (Java KeyStore - 2KB)
private_key.pem        (RSA Private Key - PEM format)
public_key_only.pem    (RSA Public Key - PEM format)
```

### Step 4: Start Docker Services

```bash
docker-compose up -d
```

**What happens**:
1. **postgres** starts → Database ready
2. **liquibase** runs → Creates `setting` table
3. **jwt-keys-init** runs → Updates JWT keys in database
4. **app** starts → Loads keys from database
5. **nginx** starts → Reverse proxy ready

### Step 5: Verify Setup

```bash
# Check JWT keys initialization logs
docker-compose logs jwt-keys-init

# Check if keys are in database
docker-compose exec postgres psql -U sa -d app -c \
  "SELECT organization_code,
          CASE WHEN jwt_private_key IS NOT NULL THEN 'SET' ELSE 'NULL' END as private_key,
          CASE WHEN jwt_public_key IS NOT NULL THEN 'SET' ELSE 'NULL' END as public_key
   FROM setting WHERE organization_code = 'DSI-007';"
```

**Expected output**:
```
 organization_code | private_key | public_key
-------------------+-------------+------------
 DSI-007           | SET         | SET
```

✅ **Setup Complete!** Your JWT keys are now stored in the database and ready to use.

---

## Detailed Setup

### Understanding the File Structure

```
spring-security-in-action/
├── docker-compose.yml                    # Orchestrates all services
├── scripts/
│   └── update-jwt-keys.sh                # Bash script to update DB
├── keys/                                 # GITIGNORED - Local keys only
│   ├── .gitkeep                          # Tracks directory
│   ├── jwt-keystore.jks                  # Java KeyStore (generated)
│   ├── private_key.pem                   # RSA private key (generated)
│   └── public_key_only.pem               # RSA public key (generated)
├── src/main/resources/db/changelog/
│   ├── changelog-master.yaml             # Main changelog
│   └── changelog-3.0.yaml                # JWT keys table migration
├── src/main/java/.../entity/
│   └── Setting.java                      # JPA entity
├── src/main/java/.../repository/
│   └── SettingRepository.java            # Data access
└── src/main/java/.../service/
    └── JwtKeyService.java                # Key loading service
```

### Database Schema

**Table: `setting`**

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| organization_code | VARCHAR(100) | Unique org identifier |
| organization_name | VARCHAR(255) | Organization name |
| jwt_private_key | TEXT | RSA private key (PEM format) |
| jwt_public_key | TEXT | RSA public key (PEM format) |
| jwt_algorithm | VARCHAR(20) | Signing algorithm (RS256) |
| jwt_issuer | VARCHAR(255) | JWT issuer claim |
| jwt_expiration_minutes | INTEGER | Token expiration time |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

**Indexes**:
- `idx_setting_org_code` on `organization_code`

**Constraints**:
- Unique constraint on `organization_code`
- Check constraint on `jwt_algorithm` (RS256, RS384, RS512, HS256, HS384, HS512)

### Docker Service: jwt-keys-init

**Purpose**: Automatically updates JWT keys in PostgreSQL after Liquibase migrations.

**Configuration**:
```yaml
jwt-keys-init:
  image: postgres:17-alpine              # Reuses postgres image (has psql)
  depends_on:
    liquibase:
      condition: service_completed_successfully
  environment:
    ORGANIZATION_CODE: DSI-007           # Your organization code
    JWT_KEYS_DIR: /jwt-keys              # Container path for keys
    SSO_USER_DATABASE_HOST: postgres     # Database service name
    SSO_USER_DATABASE_NAME: app          # Database name
    SSO_USER_DATABASE_USERNAME: sa       # Database user
    SSO_USER_DATABASE_PASSWORD: Admin@123!
  volumes:
    - ./keys:/jwt-keys:ro                # Mount keys (read-only)
    - ./scripts/update-jwt-keys.sh:/scripts/update-jwt-keys.sh:ro
  command: ["/bin/sh", "/scripts/update-jwt-keys.sh"]
  restart: "no"                          # Run once and exit
```

**Key Points**:
- Runs **after** Liquibase creates the `setting` table
- Mounts `keys/` directory as **read-only** for security
- Executes once and exits (doesn't keep running)
- Uses same PostgreSQL image to avoid extra dependencies

---

## How It Works

### 1. Key Generation (Local Machine)

```
keytool (JDK) → jwt-keystore.jks → openssl → PEM files
```

1. `keytool` generates RSA key pair in Java KeyStore format
2. `openssl` converts to PEM format (universal text format)
3. Keys stored in `keys/` directory (gitignored)

### 2. Docker Startup Sequence

```mermaid
graph TD
    A[docker-compose up] --> B[PostgreSQL starts]
    B --> C{PostgreSQL healthy?}
    C -->|No| B
    C -->|Yes| D[Liquibase runs migrations]
    D --> E[setting table created]
    E --> F[jwt-keys-init starts]
    F --> G[Reads keys from /jwt-keys]
    G --> H[Updates PostgreSQL setting table]
    H --> I[Spring Boot app starts]
    I --> J[Loads keys from database]
    J --> K[JWT ready for use]
```

### 3. Key Loading in Spring Boot

```java
@Service
public class JwtKeyService {

    // Loads from database (cached)
    public PrivateKey loadPrivateKey() {
        Setting setting = settingRepository.findByOrganizationCode("DSI-007");
        return convertPemToPrivateKey(setting.getJwtPrivateKey());
    }

    public PublicKey loadPublicKey() {
        Setting setting = settingRepository.findByOrganizationCode("DSI-007");
        return convertPemToPublicKey(setting.getJwtPublicKey());
    }
}
```

**Caching**:
- Keys are cached in memory after first load
- Cache key: organization code
- Reduces database queries

### 4. Using Keys for JWT

**Example: Signing a JWT Token**
```java
@Service
public class JwtTokenService {

    @Autowired
    private JwtKeyService jwtKeyService;

    public String generateToken(String username) {
        PrivateKey privateKey = jwtKeyService.loadPrivateKey();
        String issuer = jwtKeyService.getIssuer();
        int expirationMinutes = jwtKeyService.getExpirationMinutes();

        return Jwts.builder()
            .setSubject(username)
            .setIssuer(issuer)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000))
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();
    }
}
```

**Example: Verifying a JWT Token**
```java
public boolean validateToken(String token) {
    PublicKey publicKey = jwtKeyService.loadPublicKey();

    try {
        Jwts.parserBuilder()
            .setSigningKey(publicKey)
            .build()
            .parseClaimsJws(token);
        return true;
    } catch (JwtException e) {
        return false;
    }
}
```

---

## Troubleshooting

### Issue 1: Keys Not Found

**Symptom**:
```
✗ Error: Private key file not found at /jwt-keys/private_key.pem
```

**Solution**:
```bash
# Check if keys exist locally
ls -l keys/

# If missing, generate keys (see Quick Start Step 1-2)
```

### Issue 2: Permission Denied

**Symptom**:
```
Permission denied: /jwt-keys/private_key.pem
```

**Solution**:
```bash
# Fix file permissions
chmod 644 keys/*.pem
chmod 644 keys/*.jks
```

### Issue 3: Database Connection Failed

**Symptom**:
```
✗ PostgreSQL did not become ready in time
```

**Solution**:
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Issue 4: Table 'setting' Does Not Exist

**Symptom**:
```
⚠ Table 'setting' does not exist yet
```

**Solution**:
```bash
# Check Liquibase logs
docker-compose logs liquibase

# Re-run migrations
docker-compose restart liquibase

# Wait for Liquibase to complete
docker-compose logs -f liquibase
```

### Issue 5: Keys Not Loading in Spring Boot

**Symptom**:
```java
IllegalStateException: JWT private key not configured for organization: DSI-007
```

**Solution**:
```bash
# Verify keys are in database
docker-compose exec postgres psql -U sa -d app -c \
  "SELECT LENGTH(jwt_private_key), LENGTH(jwt_public_key) FROM setting WHERE organization_code = 'DSI-007';"

# Should show two non-zero lengths

# If keys are null, re-run jwt-keys-init
docker-compose up jwt-keys-init
```

### Issue 6: Invalid Key Format

**Symptom**:
```
Invalid JWT private key configuration
```

**Solution**:
```bash
# Check PEM file format
head -1 keys/private_key.pem
# Should show: -----BEGIN PRIVATE KEY-----

head -1 keys/public_key_only.pem
# Should show: -----BEGIN PUBLIC KEY-----

# If format is wrong, regenerate PEM files (see Quick Start Step 2)
```

---

## Team Onboarding

### For New Team Members

**Step 1: Clone Repository**
```bash
git clone <repository-url>
cd spring-security-in-action
```

**Step 2: Generate Your Own Keys**
```bash
# Follow Quick Start Steps 1-2
# Generate keys locally (they won't be committed)
```

**Step 3: Start Docker Services**
```bash
docker-compose up -d
```

**Step 4: Verify**
```bash
docker-compose logs jwt-keys-init
docker-compose ps
```

✅ Done! Each developer has their own local keys.

### Why Each Developer Has Own Keys?

**Benefits**:
- ✅ No shared secrets in version control
- ✅ Each environment is isolated
- ✅ Easy to regenerate if compromised
- ✅ Matches production security model

**Note**: For production, use a proper secret management system (AWS Secrets Manager, HashiCorp Vault, Azure Key Vault).

---

## Security Best Practices

### ✅ DO

1. **Keep Keys Out of Git**
   - `keys/` directory is gitignored
   - Never commit `.jks`, `.pem`, or `.p12` files

2. **Use Strong Passwords**
   - Change default `changeit` password
   - Use different passwords for each environment

3. **Rotate Keys Regularly**
   - Production: Every 90-180 days
   - Development: Whenever compromised

4. **Restrict File Permissions**
   ```bash
   chmod 600 keys/*.pem
   chmod 644 keys/*.jks
   ```

5. **Use Environment-Specific Keys**
   - Development keys != Production keys
   - Never use dev keys in production

6. **Monitor Key Usage**
   - Log JWT signing operations
   - Alert on unusual token generation patterns

### ❌ DON'T

1. **Never Commit Keys to Git**
   ```bash
   # Check before committing
   git status
   # Should NOT show keys/ directory
   ```

2. **Never Share Keys via Email/Slack**
   - Use secure secret management
   - Rotate keys if accidentally shared

3. **Never Use Weak Passwords**
   - Avoid: `password`, `123456`, `changeit` (production)
   - Use: Strong, randomly generated passwords

4. **Never Store Keys in Plain Environment Variables**
   - Docker Compose `.env` files are acceptable for local dev
   - Production: Use Docker secrets or vault

5. **Never Reuse Keys Across Environments**
   - Development, staging, production each need unique keys

---

## Advanced Configuration

### Changing Organization Code

**Edit docker-compose.yml**:
```yaml
jwt-keys-init:
  environment:
    ORGANIZATION_CODE: YOUR-ORG-CODE  # Change this
```

**Update application.properties**:
```properties
jwt.organization.code=YOUR-ORG-CODE
```

### Using Different Key Sizes

**2048-bit (Standard)**:
```bash
-keysize 2048
```

**4096-bit (More Secure, Slower)**:
```bash
-keysize 4096
```

### Changing Key Validity Period

**Default: 10 years (3650 days)**
```bash
-validity 3650
```

**1 year**:
```bash
-validity 365
```

**5 years**:
```bash
-validity 1825
```

### Adding Multiple Organizations

**Insert additional organizations**:
```sql
INSERT INTO setting (organization_code, organization_name, jwt_algorithm, jwt_issuer)
VALUES ('ORG-002', 'Organization 2', 'RS256', 'https://org2.example.com');
```

**Generate keys for new organization**:
```bash
# Generate separate keystore
keytool -genkeypair \
  -alias jwt-key-org2 \
  -keyalg RSA \
  -keysize 2048 \
  -keystore keys/jwt-keystore-org2.jks \
  -dname "CN=ORG-002, OU=Dept, O=Organization 2, L=City, ST=State, C=BD" \
  -validity 3650
```

---

## Useful Commands

### View Keys in Database
```bash
docker-compose exec postgres psql -U sa -d app -c \
  "SELECT id, organization_code,
          LENGTH(jwt_private_key) as priv_len,
          LENGTH(jwt_public_key) as pub_len,
          jwt_algorithm, jwt_issuer
   FROM setting;"
```

### Update Keys Manually
```bash
# Re-run jwt-keys-init service
docker-compose up jwt-keys-init
```

### Clear Keys from Database
```bash
docker-compose exec postgres psql -U sa -d app -c \
  "UPDATE setting SET jwt_private_key = NULL, jwt_public_key = NULL WHERE organization_code = 'DSI-007';"
```

### Export Keys for Backup
```bash
# Backup keys directory
tar -czf jwt-keys-backup-$(date +%Y%m%d).tar.gz keys/

# Store backup securely (NOT in Git!)
```

### Restore Keys from Backup
```bash
# Extract backup
tar -xzf jwt-keys-backup-YYYYMMDD.tar.gz

# Restart jwt-keys-init
docker-compose up jwt-keys-init
```

---

## Production Deployment

### Recommended Approach: Docker Secrets

**docker-compose.prod.yml**:
```yaml
services:
  jwt-keys-init:
    secrets:
      - jwt_private_key
      - jwt_public_key
    environment:
      JWT_PRIVATE_KEY_FILE: /run/secrets/jwt_private_key
      JWT_PUBLIC_KEY_FILE: /run/secrets/jwt_public_key

secrets:
  jwt_private_key:
    file: ./secrets/private_key.pem
  jwt_public_key:
    file: ./secrets/public_key_only.pem
```

### Alternative: Environment Variables (Base64 Encoded)

```bash
# Encode keys
PRIVATE_KEY_BASE64=$(base64 -w 0 keys/private_key.pem)
PUBLIC_KEY_BASE64=$(base64 -w 0 keys/public_key_only.pem)

# Set environment variables
export JWT_PRIVATE_KEY_BASE64="$PRIVATE_KEY_BASE64"
export JWT_PUBLIC_KEY_BASE64="$PUBLIC_KEY_BASE64"
```

### Alternative: External Secret Management

- **AWS Secrets Manager**
- **HashiCorp Vault**
- **Azure Key Vault**
- **Google Cloud Secret Manager**

---

## FAQ

**Q: Do I commit the `keys/` directory to Git?**
A: No! The directory structure is tracked (`.gitkeep`), but key files are gitignored.

**Q: What if I lose my keys?**
A: Regenerate them (Steps 1-2), restart Docker. All existing JWT tokens will be invalid.

**Q: Can I use the same keys in production?**
A: No! Generate separate keys for each environment (dev, staging, prod).

**Q: How do I rotate keys without downtime?**
A: This requires multi-key support (future enhancement). For now, key rotation requires brief downtime.

**Q: What's the difference between `.jks` and `.pem` files?**
A: `.jks` is Java KeyStore (binary), `.pem` is text format. We use both - `.jks` for keytool compatibility, `.pem` for database storage.

**Q: Why RSA instead of HMAC (symmetric) keys?**
A: RSA (asymmetric) allows distributing public keys for verification without exposing signing keys. Better for microservices.

**Q: Can I view my private key?**
A: Yes: `cat keys/private_key.pem` - But never share it!

---

## Support

**Issues?**
1. Check [Troubleshooting](#troubleshooting) section
2. Review Docker logs: `docker-compose logs`
3. Verify key files exist: `ls -l keys/`
4. Check database: `docker-compose exec postgres psql -U sa -d app`

**Questions?**
- Review this documentation
- Check `NGINX_DOCUMENTATION.md` for reverse proxy setup
- See Spring Security documentation

---

## Summary

✅ **What We Built**:
- Secure JWT key management with RSA
- Database storage for keys (PostgreSQL)
- Docker-integrated workflow
- Automatic initialization
- Team-friendly onboarding

✅ **What's Protected**:
- Keys never in Git
- Keys never in Docker images
- Keys never in environment variables (in logs)
- Keys read-only in containers

✅ **What's Easy**:
- One-time key generation
- Automatic Docker setup
- Simple team onboarding
- Clear troubleshooting

**You're all set!** 🎉
