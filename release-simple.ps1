param([string]$Version)

if (-not $Version) {
    Write-Host "Usage: .\release-simple.ps1 -Version 1.2.1" -ForegroundColor Yellow
    git tag -l
    exit 1
}

Write-Host "Release version: $Version" -ForegroundColor Green

# Check version format
if ($Version -notmatch "^\d+\.\d+\.\d+$") {
    Write-Host "Invalid version format: $Version" -ForegroundColor Red
    exit 1
}

# Check git status
$status = git status --porcelain
if ($status) {
    Write-Host "Working directory not clean" -ForegroundColor Red
    git status --short
    exit 1
}

# Check if tag exists
$existingTag = git tag -l "v$Version"
if ($existingTag) {
    Write-Host "Tag v$Version already exists" -ForegroundColor Red
    exit 1
}

# Confirm
Write-Host "Create release v$Version? (y/N)" -ForegroundColor Yellow
$confirm = Read-Host
if ($confirm -ne "y") {
    Write-Host "Cancelled" -ForegroundColor Blue
    exit 0
}

# Update README with proper UTF-8 encoding handling
Write-Host "Updating README.md..." -ForegroundColor Blue
$content = Get-Content "README.md" -Raw -Encoding UTF8
$content = $content -replace "RemoveExtraBlankLines-\d+\.\d+\.\d+\.jar", "RemoveExtraBlankLines-$Version.jar"
$content = $content -replace "项目版本：\d+\.\d+\.\d+", "项目版本：$Version"
Set-Content "README.md" $content -NoNewline -Encoding UTF8

# Commit changes
$diff = git diff --name-only README.md
if ($diff) {
    Write-Host "Committing README changes..." -ForegroundColor Blue
    git add README.md
    git commit -m "Update version to v$Version"
}

# Create tag
Write-Host "Creating tag v$Version..." -ForegroundColor Blue
git tag -a "v$Version" -m "Release version $Version"

# Get current branch name
$currentBranch = git branch --show-current

# Push
Write-Host "Pushing to GitHub..." -ForegroundColor Blue
git push origin $currentBranch
git push origin "v$Version"

Write-Host "Release complete!" -ForegroundColor Green 