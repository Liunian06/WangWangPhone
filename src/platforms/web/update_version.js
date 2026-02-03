const fs = require('fs');
const path = require('path');

const versionFile = path.join(__dirname, '../../../version.properties');
if (!fs.existsSync(versionFile)) {
  console.error(`version.properties not found at ${versionFile}`);
  process.exit(1);
}

const content = fs.readFileSync(versionFile, 'utf8');
const versionCodeMatch = content.match(/VERSION_CODE=(\d+)/);
const versionNameMatch = content.match(/VERSION_NAME=([^\s#]+)/);

if (!versionCodeMatch || !versionNameMatch) {
  console.error('Could not find version info in version.properties');
  process.exit(1);
}

const versionCode = versionCodeMatch[1];
const versionName = versionNameMatch[1];

console.log(`Updating Web version to Name: ${versionName}, Code: ${versionCode}`);

// Update package.json if it exists
const packageJsonPath = path.join(__dirname, 'package.json');
if (fs.existsSync(packageJsonPath)) {
  const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
  pkg.version = versionName;
  fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n');
  console.log(`Updated ${packageJsonPath} version to ${versionName}`);
} else {
  // Create a minimal package.json if it doesn't exist
  const pkg = {
    name: "wangwang-web",
    version: versionName,
    description: "WangWangPhone Web Platform"
  };
  fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n');
  console.log(`Created ${packageJsonPath} with version ${versionName}`);
}