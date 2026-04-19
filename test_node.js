import * as child_process from 'child_process';
try {
  child_process.execSync('node -e "fs.rmSync(\'dist\', { recursive: true, force: true })"', {stdio: 'inherit'});
} catch (e) {
  console.error("Failed!", e.message);
}
