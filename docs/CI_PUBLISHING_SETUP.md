# CI Publishing Setup Guide

This guide explains how to set up GitHub Actions for automated publishing to Maven Central.

## Required GitHub Secrets

You need to add these secrets to your GitHub repository (Settings → Secrets and variables → Actions):

**Note**: The workflow also requires write permissions to push README updates. This is configured in the workflow file with `permissions: contents: write`.

### 1. Maven Central Credentials
- **`MAVEN_CENTRAL_USERNAME`**: Your Maven Central username (e.g., `J4Rk3ruE`)
- **`MAVEN_CENTRAL_PASSWORD`**: Your Maven Central password

### 2. GPG Signing
- **`SIGNING_KEY_ID`**: The last 8 characters of your GPG key ID (e.g., `7097995E`)
  - ⚠️ **IMPORTANT**: Must be exactly 8 characters, not the full 16-character key ID
  - To get the correct ID: `gpg --list-secret-keys --keyid-format SHORT`
- **`SIGNING_PASSWORD`**: Your GPG key password (leave empty if no password)
- **`GPG_KEY_CONTENTS`**: Your GPG private key exported as ASCII

## How to Export Your GPG Key

1. List your GPG keys:
   ```bash
   gpg --list-secret-keys
   ```

2. Export your private key:
   ```bash
   gpg --armor --export-secret-keys YOUR_KEY_ID > gpg_key.asc
   ```

3. Verify the exported key format:
   ```bash
   # Should show: -----BEGIN PGP PRIVATE KEY BLOCK-----
   head -1 gpg_key.asc
   
   # Should show: -----END PGP PRIVATE KEY BLOCK-----
   tail -1 gpg_key.asc
   ```

4. Copy the entire contents of `gpg_key.asc` (including the BEGIN and END lines) and paste it as the value for `GPG_KEY_CONTENTS` secret.

**Important**: The GPG key must be in ASCII-armored format and include:
- `-----BEGIN PGP PRIVATE KEY BLOCK-----`
- The key content (base64 encoded)
- `-----END PGP PRIVATE KEY BLOCK-----`

## Testing the CI Workflow

1. Go to Actions tab in your GitHub repository
2. Select "Publish" workflow
3. Click "Run workflow"
4. Enter the version (e.g., `1.0.0-alpha04`)
5. Check "Dry run" for testing without actual publishing
6. Click "Run workflow"

## Troubleshooting

### Common Issues

1. **GPG signing fails**: 
   - Make sure `GPG_KEY_CONTENTS` includes the entire key with headers
   - Check that `SIGNING_KEY_ID` is exactly 8 characters (the last 8 characters of your key ID)
   - Common error: "The key ID must be in a valid form" - this means your key ID is not 8 characters
   - If you see `signAndroidReleasePublication FAILED`, ensure the GPG key is properly exported in ASCII-armored format
   - The key should start with `-----BEGIN PGP PRIVATE KEY BLOCK-----`

2. **Maven Central authentication fails**:
   - Verify your username and password are correct
   - Make sure you're using Central Portal credentials, not legacy Sonatype credentials

3. **Version already exists**:
   - Maven Central doesn't allow overwriting versions
   - Always use a new version number

4. **Signing key not found**:
   - Ensure all signing-related secrets are properly set in GitHub
   - The `GPG_KEY_CONTENTS` secret must contain the full ASCII-armored private key
   - Check the workflow logs for "GPG key file created successfully" message

### Debugging

The workflow includes extensive debugging output. Check the logs for:
- GPG key file creation
- Environment variable setup
- Gradle properties
- Error messages from Maven Central

## Differences from Local Publishing

The CI workflow:
1. Creates `local.properties` dynamically with the version from workflow input
2. Uses GitHub secrets instead of local files
3. Can automatically update README.md and commit changes
4. Includes more error handling and debugging output

## Manual Workflow Dispatch

You can manually trigger the workflow:
1. Go to Actions → Publish
2. Click "Run workflow"
3. Fill in:
   - Version: The version to publish (e.g., `1.0.0-alpha04`)
   - Dry run: Check this to test without publishing
4. Click "Run workflow"

The workflow will use the version you specify instead of reading from local.properties.