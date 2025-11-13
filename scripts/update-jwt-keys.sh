#!/bin/bash
set -e

# Docker-adapted JWT Keys Update Script
# This script updates JWT keys in the PostgreSQL database
# Designed to run inside a Docker container with mounted keys directory

# Configuration from environment variables (Docker Compose will provide these)
ORGANIZATION_CODE="${ORGANIZATION_CODE:-DSI-007}"
JWT_KEYS_DIR="${JWT_KEYS_DIR:-/jwt-keys}"
PRIVATE_KEY_FILE="${JWT_KEYS_DIR}/private_key.pem"
PUBLIC_KEY_FILE="${JWT_KEYS_DIR}/public_key_only.pem"

# Database configuration from Docker Compose environment
DB_HOST="${SSO_USER_DATABASE_HOST:-postgres}"
DB_PORT="${SSO_USER_DATABASE_PORT:-5432}"
DB_NAME="${SSO_USER_DATABASE_NAME:-app}"
DB_USER="${SSO_USER_DATABASE_USERNAME:-sa}"
DB_PASSWORD="${SSO_USER_DATABASE_PASSWORD:-Admin@123!}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   JWT Keys Database Update Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to wait for PostgreSQL to be ready
wait_for_postgres() {
    echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
    local max_attempts=30
    local attempt=1

    export PGPASSWORD="${DB_PASSWORD}"

    while [ $attempt -le $max_attempts ]; do
        if psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "SELECT 1" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ PostgreSQL is ready!${NC}"
            unset PGPASSWORD
            return 0
        fi

        echo "Attempt $attempt/$max_attempts: PostgreSQL not ready yet..."
        sleep 2
        attempt=$((attempt + 1))
    done

    unset PGPASSWORD
    echo -e "${RED}✗ PostgreSQL did not become ready in time${NC}"
    return 1
}

# Check if key files exist
check_key_files() {
    echo -e "${YELLOW}Checking for key files...${NC}"

    if [ ! -f "${PRIVATE_KEY_FILE}" ]; then
        echo -e "${RED}✗ Error: Private key file not found at ${PRIVATE_KEY_FILE}${NC}"
        echo -e "${YELLOW}Please ensure keys are generated and mounted at ${JWT_KEYS_DIR}${NC}"
        echo ""
        echo -e "${BLUE}To generate keys locally, run:${NC}"
        echo "  keytool -genkeypair -alias jwt-key -keyalg RSA -keysize 2048 \\"
        echo "    -keystore keys/jwt-keystore.jks -keypass changeit -storepass changeit \\"
        echo "    -dname \"CN=DSI-007, OU=DSI, O=DSI Innovators, L=Dhaka, ST=Dhaka, C=BD\" \\"
        echo "    -validity 3650"
        echo ""
        echo "Then extract PEM files and place them in the keys/ directory"
        return 1
    fi

    if [ ! -f "${PUBLIC_KEY_FILE}" ]; then
        echo -e "${RED}✗ Error: Public key file not found at ${PUBLIC_KEY_FILE}${NC}"
        echo -e "${YELLOW}Please ensure keys are generated and mounted at ${JWT_KEYS_DIR}${NC}"
        return 1
    fi

    echo -e "${GREEN}✓ Found private key file: ${PRIVATE_KEY_FILE}${NC}"
    echo -e "${GREEN}✓ Found public key file: ${PUBLIC_KEY_FILE}${NC}"
    echo ""
    return 0
}

# Read and process keys
read_keys() {
    echo -e "${YELLOW}Reading key files...${NC}"

    # Extract only the key content (skip bag attributes)
    # For private key: extract from -----BEGIN to -----END
    PRIVATE_KEY=$(sed -n '/-----BEGIN/,/-----END/p' "${PRIVATE_KEY_FILE}")
    PUBLIC_KEY=$(cat "${PUBLIC_KEY_FILE}")

    if [ -z "${PRIVATE_KEY}" ]; then
        echo -e "${RED}✗ Failed to read private key${NC}"
        return 1
    fi

    if [ -z "${PUBLIC_KEY}" ]; then
        echo -e "${RED}✗ Failed to read public key${NC}"
        return 1
    fi

    echo -e "${GREEN}✓ Private key loaded ($(echo "${PRIVATE_KEY}" | wc -l) lines)${NC}"
    echo -e "${GREEN}✓ Public key loaded ($(echo "${PUBLIC_KEY}" | wc -l) lines)${NC}"
    echo ""
    return 0
}

# Check if setting table and row exist
check_table_exists() {
    echo -e "${YELLOW}Checking database schema...${NC}"

    export PGPASSWORD="${DB_PASSWORD}"

    # Check if setting table exists
    TABLE_CHECK=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -t -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'setting');" 2>&1)

    if echo "${TABLE_CHECK}" | grep -q "t"; then
        echo -e "${GREEN}✓ Table 'setting' exists${NC}"

        # Check if organization row exists
        ROW_CHECK=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
            -t -c "SELECT COUNT(*) FROM setting WHERE organization_code = '${ORGANIZATION_CODE}';" 2>&1)

        ROW_COUNT=$(echo "${ROW_CHECK}" | tr -d ' ')

        if [ "${ROW_COUNT}" -gt 0 ]; then
            echo -e "${GREEN}✓ Organization '${ORGANIZATION_CODE}' exists in setting table${NC}"
            unset PGPASSWORD
            return 0
        else
            echo -e "${YELLOW}⚠ Organization '${ORGANIZATION_CODE}' not found in setting table${NC}"
            echo -e "${YELLOW}You may need to insert a row first${NC}"
            unset PGPASSWORD
            return 1
        fi
    else
        echo -e "${YELLOW}⚠ Table 'setting' does not exist yet${NC}"
        echo -e "${YELLOW}This is normal if migrations haven't run yet${NC}"
        unset PGPASSWORD
        return 1
    fi
}

# Update keys in database
update_keys() {
    echo -e "${YELLOW}Updating JWT keys in database...${NC}"
    echo "Host: ${DB_HOST}:${DB_PORT}"
    echo "Database: ${DB_NAME}"
    echo "User: ${DB_USER}"
    echo "Organization: ${ORGANIZATION_CODE}"
    echo ""

    # Escape the keys for SQL (replace single quotes with two single quotes)
    PRIVATE_KEY_ESCAPED="${PRIVATE_KEY//\'/\'\'}"
    PUBLIC_KEY_ESCAPED="${PUBLIC_KEY//\'/\'\'}"

    # Create the SQL update statement
    SQL="UPDATE setting
    SET jwt_private_key = '${PRIVATE_KEY_ESCAPED}',
        jwt_public_key = '${PUBLIC_KEY_ESCAPED}'
    WHERE organization_code = '${ORGANIZATION_CODE}';"

    # Execute the SQL update
    export PGPASSWORD="${DB_PASSWORD}"
    RESULT=$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "${SQL}" 2>&1)
    EXIT_CODE=$?
    unset PGPASSWORD

    if [ ${EXIT_CODE} -eq 0 ]; then
        echo -e "${GREEN}✓ Successfully updated JWT keys in settings table!${NC}"
        echo ""
        return 0
    else
        echo -e "${RED}✗ Failed to update JWT keys!${NC}"
        echo -e "${RED}Error: ${RESULT}${NC}"
        return 1
    fi
}

# Verify the update
verify_update() {
    echo -e "${YELLOW}Verifying update...${NC}"

    export PGPASSWORD="${DB_PASSWORD}"
    VERIFY_SQL="SELECT id, organization_code,
        CASE WHEN jwt_private_key IS NOT NULL THEN 'SET (length: ' || LENGTH(jwt_private_key) || ')' ELSE 'NULL' END as private_key_status,
        CASE WHEN jwt_public_key IS NOT NULL THEN 'SET (length: ' || LENGTH(jwt_public_key) || ')' ELSE 'NULL' END as public_key_status
    FROM setting
    WHERE organization_code = '${ORGANIZATION_CODE}';"

    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "${VERIFY_SQL}"
    unset PGPASSWORD
    echo ""
}

# Main execution
main() {
    # Wait for PostgreSQL
    if ! wait_for_postgres; then
        echo -e "${RED}Exiting due to database connection failure${NC}"
        exit 1
    fi
    echo ""

    # Check key files
    if ! check_key_files; then
        echo -e "${RED}Exiting due to missing key files${NC}"
        exit 1
    fi

    # Read keys
    if ! read_keys; then
        echo -e "${RED}Exiting due to key reading failure${NC}"
        exit 1
    fi

    # Check if table exists (non-fatal, just informational)
    if check_table_exists; then
        echo ""

        # Update keys
        if update_keys; then
            # Verify
            verify_update

            echo -e "${GREEN}========================================${NC}"
            echo -e "${GREEN}   JWT Keys Update Complete! ✓${NC}"
            echo -e "${GREEN}========================================${NC}"
            exit 0
        else
            echo -e "${RED}JWT keys update failed${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}Skipping key update - database schema not ready${NC}"
        echo -e "${YELLOW}Run Liquibase migrations first, then restart this service${NC}"
        exit 0
    fi
}

# Run main function
main
